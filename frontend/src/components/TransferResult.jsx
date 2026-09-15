import { formatDateTime, formatMoney, formatPercent, providerName } from '../format.js'

/** Shows the recorded transaction: SUCCESS or FAILED, with the DFSP's message. */
function TransferResult({ result, providers, onNewTransfer }) {
  const succeeded = result.status === 'SUCCESS'

  return (
    <section className="card">
      <div className="result-header">
        <h2>{succeeded ? 'Transfer completed' : 'Transfer failed'}</h2>
        <span className={`badge ${succeeded ? 'badge-success' : 'badge-failed'}`}>{result.status}</span>
      </div>
      <p className="result-message">{result.message}</p>

      <dl className="summary">
        <div>
          <dt>Transaction ID</dt>
          <dd className="mono">{result.transactionId}</dd>
        </div>
        <div>
          <dt>Route</dt>
          <dd>
            {providerName(providers, result.sourceProviderCode)} → {providerName(providers, result.destinationProviderCode)}
          </dd>
        </div>
        <div>
          <dt>Amount</dt>
          <dd>{formatMoney(result.amount)}</dd>
        </div>
        <div>
          <dt>Fee ({formatPercent(result.feePercentage)})</dt>
          <dd>{formatMoney(result.feeAmount)}</dd>
        </div>
        <div className="total">
          <dt>Total</dt>
          <dd>{formatMoney(result.totalAmount)}</dd>
        </div>
        <div>
          <dt>Time</dt>
          <dd>{formatDateTime(result.createdAt)}</dd>
        </div>
      </dl>

      <button type="button" className="button" onClick={onNewTransfer}>
        New transfer
      </button>
    </section>
  )
}

export default TransferResult
