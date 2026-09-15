// All HTTP calls to the Payment Router live here, so components never build URLs or parse errors.

const API_BASE = '/api'

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
      ...options,
    })
  } catch {
    // fetch only rejects when the server cannot be reached at all.
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
