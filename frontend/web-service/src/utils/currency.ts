/**
 * The bank's own currency. Accounts, bills and loans are all held in it, and the backend defaults
 * every new account and ledger posting to it, so it is the only sensible fallback while an
 * account is still loading.
 */
export const DEFAULT_CURRENCY = 'LKR';

/** The currency of the account being acted on; never asked of the customer. */
export const currencyOf = (account?: { currency?: string } | null) =>
  account?.currency || DEFAULT_CURRENCY;

/** "LKR 1,234.50" - the one money format used across the app. */
export const formatMoney = (value: number, currency: string = DEFAULT_CURRENCY) =>
  `${currency} ${new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)}`;
