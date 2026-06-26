const ZERO_DECIMAL = new Set(['VND', 'JPY', 'KRW', 'IDR', 'CLP']);

function fractionDigits(currency: string): number {
  return ZERO_DECIMAL.has(currency.toUpperCase()) ? 0 : 2;
}

/** "12,450,000 VND" — grouped number plus the currency code, matching the mockup. */
export function formatMoney(amount: number, currency = 'VND'): string {
  const digits = fractionDigits(currency);
  const formatted = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(amount ?? 0);
  return `${formatted} ${currency}`;
}

/** Signed variant for balances: "+2,750,000" / "-1,100,000". */
export function formatSigned(amount: number, currency = 'VND'): string {
  const sign = amount > 0 ? '+' : '';
  return `${sign}${formatMoney(amount, currency)}`;
}
