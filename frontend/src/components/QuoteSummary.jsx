import { formatMoney, formatPercent, providerName } from '../format.js'

/** Shows the quote returned by the router and lets the user confirm the transfer. */
function QuoteSummary({ quote, providers, loading, onConfirm }) {
  return (
    <section className="card">
      <h2>Quote</h2>
      <p className="route">
        {providerName(providers, quote.sourceProviderCode)} → {providerName(providers, quote.destinationProviderCode)}
      </p>

      <dl className="summary">
        <div>
          <dt>Amount</dt>
          <dd>{formatMoney(quote.amount)}</dd>
        </div>
        <div>
          <dt>Fee ({formatPercent(quote.feePercentage)})</dt>
          <dd>{formatMoney(quote.feeAmount)}</dd>
        </div>
        <div className="total">
          <dt>Total</dt>
          <dd>{formatMoney(quote.totalAmount)}</dd>
        </div>
      </dl>

      <p className="hint">The fee is set by the destination provider. Nothing has been sent yet.</p>

      <button type="button" className="button primary" onClick={onConfirm} disabled={loading}>
        {loading ? 'Sending transfer…' : 'Confirm transfer'}
      </button>
    </section>
  )
}

export default QuoteSummary
