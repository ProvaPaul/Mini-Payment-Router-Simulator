function FieldError({ message }) {
  if (!message) {
    return null
  }
  return <span className="field-error">{message}</span>
}

/** Provider dropdown, used for both the source and the destination provider. */
function ProviderSelect({ label, field, value, providers, error, disabled, onChange }) {
  return (
    <label className="field">
      <span>{label}</span>
      <select value={value} onChange={(event) => onChange(field, event.target.value)} disabled={disabled} required>
        {providers.map((provider) => (
          <option key={provider.code} value={provider.code}>
            {provider.name}
          </option>
        ))}
      </select>
      <FieldError message={error} />
    </label>
  )
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
        <ProviderSelect
          label="From"
          field="sourceProviderCode"
          value={form.sourceProviderCode}
          providers={providers}
          error={fieldErrors.sourceProviderCode}
          disabled={disabled}
          onChange={onChange}
        />
        <ProviderSelect
          label="To"
          field="destinationProviderCode"
          value={form.destinationProviderCode}
          providers={providers}
          error={fieldErrors.destinationProviderCode}
          disabled={disabled}
          onChange={onChange}
        />
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
