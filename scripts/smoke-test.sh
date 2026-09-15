#!/usr/bin/env bash
# Smoke test for the running Docker Compose stack.
#
# Sends requests the way the browser does: to the frontend (Nginx), which proxies /api to the
# payment-router, which calls DFSP-A/DFSP-B and saves to PostgreSQL. Then checks the database
# rows and the log file.
#
#   docker compose up --build -d
#   bash scripts/smoke-test.sh                                   # UI on http://localhost:3000
#   BASE_URL=http://localhost:3001 bash scripts/smoke-test.sh    # if FRONTEND_PORT was changed
#
# Requires: bash, curl, docker. Exits with 1 if any check fails.

set -u
BASE_URL="${BASE_URL:-http://localhost:3000}"
cd "$(dirname "$0")/.." || exit 1   # project root, where docker-compose.yml and logs/ are

failures=0
pass() { echo "PASS  $1"; }
fail() { echo "FAIL  $1"; failures=$((failures + 1)); }

# request METHOD PATH [JSON] → sets $status and $body.
# POSTs carry an Origin header like a browser does, so a broken Nginx Host header (403) is caught.
request() {
  local response
  if [ -n "${3:-}" ]; then
    response=$(curl -s -m 20 -w '\n%{http_code}' -X "$1" -H 'Content-Type: application/json' \
      -H "Origin: $BASE_URL" -d "$3" "$BASE_URL$2")
  else
    response=$(curl -s -m 20 -w '\n%{http_code}' -X "$1" "$BASE_URL$2")
  fi
  status=$(tail -n 1 <<<"$response")
  body=$(sed '$d' <<<"$response")
}

# check NAME EXPECTED_STATUS TEXT_EXPECTED_IN_BODY
check() {
  if [ "$status" = "$2" ] && grep -qF -- "$3" <<<"$body"; then
    pass "$1"
  else
    fail "$1 (expected HTTP $2 containing '$3', got HTTP $status: ${body:0:200})"
  fi
}

payment() { echo "{\"sourceProviderCode\":\"$1\",\"destinationProviderCode\":\"$2\",\"amount\":$3}"; }
transaction_id() { grep -o '"transactionId":"[^"]*"' <<<"$body" | cut -d '"' -f 4; }

# Status of one row, read inside the postgres container with its own credentials.
db_status() {
  docker compose exec -T postgres sh -c \
    "psql -U \"\$POSTGRES_USER\" -d \"\$POSTGRES_DB\" -tAc \"SELECT status FROM transactions WHERE transaction_id = '$1'\"" \
    | tr -d '[:space:]'
}

echo "== Docker Compose =="
running=$(docker compose ps --status running --services)
for service in postgres dfsp-a dfsp-b payment-router frontend; do
  if grep -qx "$service" <<<"$running"; then pass "$service is running"; else fail "$service is not running"; fi
done

echo "== Frontend and API through Nginx ($BASE_URL) =="
request GET /
check "UI page is served" 200 "<div id=\"root\">"
request GET /api/status
check "GET /api/status reaches the router" 200 "payment-router"
request GET /api/providers
check "providers are listed" 200 "DFSP_B"

echo "== Quote and validation =="
request POST /api/quotes "$(payment DFSP_A DFSP_B 1000)"
check "valid quote uses destination fee (1.50%)" 200 '"totalAmount":1015.00'
request POST /api/quotes "$(payment DFSP_A DFSP_B 0)"
check "invalid amount is rejected" 400 "amount must be greater than zero"
request POST /api/transfers "$(payment DFSP_A DFSP_A 1000)"
check "same source and destination is rejected" 400 "cannot be the same"
request POST /api/quotes "$(payment DFSP_A DFSP_X 1000)"
check "unknown provider is rejected" 400 "Provider not found: DFSP_X"

echo "== Transfers =="
request POST /api/transfers "$(payment DFSP_A DFSP_B 1000)"
check "A -> B transfer succeeds" 201 '"status":"SUCCESS"'
a_to_b=$(transaction_id)
request POST /api/transfers "$(payment DFSP_B DFSP_A 1000)"
check "B -> A transfer succeeds with DFSP-A fee (10.00)" 201 '"feeAmount":10.00'
b_to_a=$(transaction_id)
request POST /api/transfers "$(payment DFSP_A DFSP_B 30000)"
check "transfer rejected by DFSP-B is FAILED" 201 '"status":"FAILED"'
rejected=$(transaction_id)

echo "== Persistence (PostgreSQL) =="
for pair in "$a_to_b:SUCCESS" "$b_to_a:SUCCESS" "$rejected:FAILED"; do
  id=${pair%%:*}
  expected=${pair##*:}
  actual=$(db_status "$id")
  if [ -n "$id" ] && [ "$actual" = "$expected" ]; then
    pass "transaction $id saved as $expected"
  else
    fail "transaction '$id' expected $expected in database, found '$actual'"
  fi
done

echo "== File logging (./logs/payment-router.log) =="
if [ -n "$a_to_b" ] && grep -q "Transfer $a_to_b succeeded" logs/payment-router.log 2>/dev/null; then
  pass "transfer $a_to_b is in the log file"
else
  fail "transfer '$a_to_b' not found in logs/payment-router.log"
fi

echo
if [ "$failures" -eq 0 ]; then
  echo "All smoke checks passed."
else
  echo "$failures smoke check(s) failed."
  exit 1
fi
