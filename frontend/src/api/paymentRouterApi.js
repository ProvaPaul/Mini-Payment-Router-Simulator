// All HTTP calls to the Payment Router live here, so components never build URLs or parse errors.
//
// By default the app calls the relative path /api. The Vite dev server (and Nginx in Docker)
// forwards it to the router, so the browser sees one origin and CORS is not involved.
// Set VITE_API_BASE_URL (e.g. http://localhost:8080) to call the router directly instead;
// the router's CORS allow-list must then include this page's origin.
const API_BASE = `${import.meta.env.VITE_API_BASE_URL ?? ''}/api`

// A request that takes longer than this is abandoned, so the UI never waits forever.
const REQUEST_TIMEOUT_MS = 20000

/** Error thrown for any failed API call. Carries the router's JSON error details. */
export class ApiError extends Error {
  constructor(message, status, fieldErrors = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${API_BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
      ...options,
    })
  } catch (networkError) {
    if (networkError.name === 'TimeoutError') {
      throw new ApiError('The Payment Router did not respond in time.', 0)
    }
    // fetch rejects when the server cannot be reached or the browser blocks the call (CORS).
    throw new ApiError('Cannot reach the Payment Router. Please check that the backend is running.', 0)
  }

  const body = await response.json().catch(() => null)

  if (!response.ok) {
    // The router always answers errors as {timestamp, status, error, message, fieldErrors}.
    throw new ApiError(
      body?.message ?? `Request failed with status ${response.status}`,
      response.status,
      body?.fieldErrors ?? {},
    )
  }
  return body
}

/** GET /api/providers → [{code, name}] */
export function getProviders() {
  return request('/providers')
}

/** POST /api/quotes → {amount, feePercentage, feeAmount, totalAmount, ...} */
export function requestQuote(payment) {
  return request('/quotes', { method: 'POST', body: JSON.stringify(payment) })
}

/** POST /api/transfers → {transactionId, status, message, ...} */
export function executeTransfer(payment) {
  return request('/transfers', { method: 'POST', body: JSON.stringify(payment) })
}
