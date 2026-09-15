function FieldError({ message }) {
  if (!message) {
    return null
  }
  return <span className="field-error">{message}</span>
}

/**
 * Source, destination and amount inputs. Holds no state of its own:
 * values come in through props and every change is reported to the parent.
 */
function TransferForm({ providers, form, fieldErrors, disabled, loading, onChange, onSubmit }) {
  return (
    <form className="card" onSubmit={onSubmit}>
      <h2>New transfer</h2>

      <div className="field-row">
        <label className="field">
          <span>From</span>
          <select
            value={form.sourceProviderCode}
            onChange={(event) => onChange('sourceProviderCode', event.target.value)}
            disabled={disabled}
            required
          >
            {providers.map((provider) => (
              <option key={provider.code} value={provider.code}>
                {provider.name}
              </option>
            ))}
          </select>
          <FieldError message={fieldErrors.sourceProviderCode} />
        </label>

        <label className="field">
          <span>To</span>
          <select
            value={form.destinationProviderCode}
            onChange={(event) => onChange('destinationProviderCode', event.target.value)}
            disabled={disabled}
            required
          >
            {providers.map((provider) => (
              <option key={provider.code} value={provider.code}>
                {provider.name}
              </option>
            ))}
          </select>
          <FieldError message={fieldErrors.destinationProviderCode} />
        </label>
      </div>

      <label className="field">
        <span>Amount (BDT)</span>
        <input
          type="number"
          inputMode="decimal"
          min="0.01"
          step="0.01"
          placeholder="1000.00"
          value={form.amount}
          onChange={(event) => onChange('amount', event.target.value)}
          disabled={disabled}
          required
        />
        <FieldError message={fieldErrors.amount} />
      </label>

      <button type="submit" className="button primary" disabled={disabled}>
        {loading ? 'Getting quote…' : 'Get quote'}
      </button>
    </form>
  )
}

export default TransferForm
