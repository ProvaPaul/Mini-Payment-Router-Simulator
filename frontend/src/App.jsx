import { useEffect, useState } from 'react'
import { executeTransfer, getProviders, requestQuote } from './api/paymentRouterApi.js'
import ErrorMessage from './components/ErrorMessage.jsx'
import QuoteSummary from './components/QuoteSummary.jsx'
import TransferForm from './components/TransferForm.jsx'
import TransferResult from './components/TransferResult.jsx'

const EMPTY_FORM = { sourceProviderCode: '', destinationProviderCode: '', amount: '' }

/**
 * The whole payment flow on one page: choose providers and amount → get quote →
 * confirm transfer → see the result. All state lives here; child components receive
 * data and callbacks through props.
 */
function App() {
  const [providers, setProviders] = useState([])
  const [form, setForm] = useState(EMPTY_FORM)
  const [quote, setQuote] = useState(null)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null) // { message, fieldErrors }
  const [loading, setLoading] = useState('providers') // 'providers' | 'quote' | 'transfer' | null

  // Load the provider list once, when the page opens.
  useEffect(() => {
    getProviders()
      .then((list) => {
        setProviders(list)
        setForm((current) => ({
          ...current,
          sourceProviderCode: list[0]?.code ?? '',
          destinationProviderCode: list[1]?.code ?? '',
        }))
      })
      .catch((apiError) => setError({ message: apiError.message, fieldErrors: {} }))
      .finally(() => setLoading(null))
  }, [])

  function handleChange(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
    // Any change makes the shown quote out of date.
    setQuote(null)
    setError(null)
  }

  function toPayment() {
    return {
      sourceProviderCode: form.sourceProviderCode,
      destinationProviderCode: form.destinationProviderCode,
      amount: Number(form.amount),
    }
  }

  async function handleGetQuote(event) {
    event.preventDefault()
    setLoading('quote')
    setError(null)
    try {
      setQuote(await requestQuote(toPayment()))
    } catch (apiError) {
      setQuote(null)
      setError({ message: apiError.message, fieldErrors: apiError.fieldErrors })
    } finally {
      setLoading(null)
    }
  }

  async function handleConfirmTransfer() {
    setLoading('transfer')
    setError(null)
    try {
      setResult(await executeTransfer(toPayment()))
      setQuote(null)
    } catch (apiError) {
      setError({ message: apiError.message, fieldErrors: apiError.fieldErrors })
    } finally {
      setLoading(null)
    }
  }

  function handleNewTransfer() {
    setResult(null)
    setQuote(null)
    setError(null)
    setForm((current) => ({ ...current, amount: '' }))
  }

  const busy = loading !== null

  return (
    <div className="page">
      <header className="page-header">
        <h1>Mini Payment Router</h1>
        <p>Get a quote and send a transfer between digital financial service providers.</p>
      </header>

      <main className="content">
        {error && <ErrorMessage message={error.message} />}

        {result ? (
          <TransferResult result={result} providers={providers} onNewTransfer={handleNewTransfer} />
        ) : (
          <>
            <TransferForm
              providers={providers}
              form={form}
              fieldErrors={error?.fieldErrors ?? {}}
              disabled={busy || providers.length === 0}
              loading={loading === 'quote'}
              onChange={handleChange}
              onSubmit={handleGetQuote}
            />
            {quote && (
              <QuoteSummary
                quote={quote}
                providers={providers}
                loading={loading === 'transfer'}
                onConfirm={handleConfirmTransfer}
              />
            )}
          </>
        )}
      </main>
    </div>
  )
}

export default App
