// Display helpers. Formatting only; all money calculations happen in the backend.

const moneyFormatter = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
})

/** 1015 → "1,015.00 BDT" */
export function formatMoney(value) {
  return `${moneyFormatter.format(value)} BDT`
}

/** 1.5 → "1.50%" */
export function formatPercent(value) {
  return `${Number(value).toFixed(2)}%`
}

/** "DFSP_A" → "DFSP-A" using the provider list from the API. */
export function providerName(providers, code) {
  return providers.find((provider) => provider.code === code)?.name ?? code
}

/** ISO timestamp → local date and time. */
export function formatDateTime(isoTimestamp) {
  return new Date(isoTimestamp).toLocaleString()
}
