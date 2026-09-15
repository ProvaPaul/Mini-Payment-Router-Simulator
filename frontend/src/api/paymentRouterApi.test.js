// Tests the frontend's side of the API contract: which URL, method and body are sent,
// and how router responses and network failures reach the components.
// fetch is replaced with a mock, so no backend has to be running.
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, executeTransfer, getProviders, requestQuote } from './paymentRouterApi'

const payment = { sourceProviderCode: 'DFSP_A', destinationProviderCode: 'DFSP_B', amount: 1000 }

function mockFetch(status, body) {
  const fetchMock = vi.fn().mockResolvedValue(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  )
  vi.stubGlobal('fetch', fetchMock)
  return fetchMock
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('paymentRouterApi', () => {
  it('loads providers from the relative /api path', async () => {
    const fetchMock = mockFetch(200, [{ code: 'DFSP_A', name: 'DFSP-A' }])

    await expect(getProviders()).resolves.toEqual([{ code: 'DFSP_A', name: 'DFSP-A' }])
    expect(fetchMock.mock.calls[0][0]).toBe('/api/providers')
  })

  it('posts the payment as JSON to /api/quotes', async () => {
    const quote = { feePercentage: 1.5, feeAmount: 15, totalAmount: 1015 }
    const fetchMock = mockFetch(200, quote)

    await expect(requestQuote(payment)).resolves.toEqual(quote)

    const [url, options] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/quotes')
    expect(options.method).toBe('POST')
    expect(options.headers['Content-Type']).toBe('application/json')
    expect(JSON.parse(options.body)).toEqual(payment)
  })

  it('returns a FAILED transfer as a normal result, not an error', async () => {
    const fetchMock = mockFetch(201, { status: 'FAILED', message: 'Destination DFSP is unavailable' })

    await expect(executeTransfer(payment)).resolves.toMatchObject({ status: 'FAILED' })
    expect(fetchMock.mock.calls[0][0]).toBe('/api/transfers')
  })

  it('turns a router error response into an ApiError with its message and field errors', async () => {
    mockFetch(400, {
      status: 400,
      error: 'Bad Request',
      message: 'Request validation failed',
      fieldErrors: { amount: 'amount must be greater than zero' },
    })

    const error = await requestQuote({ ...payment, amount: 0 }).catch((caught) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(400)
    expect(error.message).toBe('Request validation failed')
    expect(error.fieldErrors).toEqual({ amount: 'amount must be greater than zero' })
  })

  it('reports an unreachable router as an ApiError with status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))

    const error = await getProviders().catch((caught) => caught)

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(0)
    expect(error.message).toBe('Cannot reach the Payment Router. Please check that the backend is running.')
  })
})
