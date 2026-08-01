import { createApiClient } from './createApiClient';
import type { OtpChallenge } from '../types';

const client = createApiClient('/api/v1/accounts');

export type AccountType = 'SAVINGS' | 'CURRENT';

export interface Account {
  id: string;
  nickname: string;
  accountType: AccountType;
  lastFourDigits: string;
  accountNumber?: string;
  balance: number;
  currency: string;
  monthlyChangePercent: number;
  verifiedLabel: string;
  status: string;
  frozen: boolean;
  freezeReason: string | null;
}

export interface Transaction {
  id: string;
  merchant: string;
  category: string;
  date: string;
  amount: number;
  verified: boolean;
}

export type TransactionDirection = 'ALL' | 'IN' | 'OUT';

export interface AccountActivity {
  id: string;
  merchant: string;
  category: string;
  transactionType: string;
  location: string;
  amount: number;
  currency: string;
  timestamp: string;
  dateGroupLabel: string;
  journalId: string;
  flagged: boolean;
}

export interface TransactionHistoryFilters {
  direction?: TransactionDirection;
  dateFrom?: string;
  dateTo?: string;
  minAmount?: number;
  maxAmount?: number;
  type?: string;
  flaggedOnly?: boolean;
}

export interface AccountDetail {
  id: string;
  nickname: string;
  accountTypeLabel: string;
  currency: string;
  balance: number;
  accountNumber: string;
  ifscCode: string;
  openedOn: string;
  homeBranch: string;
  ownershipLabel: string;
  status: string;
  frozen: boolean;
  freezeReason: string | null;
  cards: BankCard[];
}

export interface BankCard {
  id: string;
  accountId: string;
  cardType: 'DEBIT' | 'CREDIT';
  productName: string;
  maskedNumber: string;
  cardholderName: string;
  expiryDate: string;
  scheme: string;
  status: string;
  jointAccountCard: boolean;
}

/** An account product the bank offers. Customers pick one; they cannot invent one. */
export interface AccountProduct {
  code: string;
  name: string;
  accountType: AccountType;
  description: string;
  currency: string;
  interestRate: number;
  minimumBalance: number;
  monthlyFee: number;
}

export interface OpenAccountPayload {
  accountType: AccountType;
  /** Product code from the bank's catalogue; always required. */
  productCode: string;
  ownershipType: 'INDIVIDUAL' | 'JOINT';
}

export interface LinkCreditCardPayload {
  cardNumber: string;
  expiryDate: string;
  nationalIdOrPassport: string;
  accountId: string;
}

export interface LinkedCardResponse {
  card: BankCard;
  message: string;
}

export interface LinkAccountPayload {
  accountNumber: string;
  nationalIdOrPassport: string;
}

export interface LinkedAccountResponse {
  account: Account;
  message: string;
}

export const accountsService = {
  client,
  getAccounts: () => client.get<Account[]>('').then((response) => response.data),
  getPrimaryAccount: () => client.get<Account>('/primary').then((response) => response.data),
  getRecentTransactions: (id: string, limit = 4) =>
    client
      .get<Transaction[]>(`/${id}/transactions/recent`, { params: { limit } })
      .then((response) => response.data),
  getTransactionHistory: (id: string, filters: TransactionHistoryFilters = {}) =>
    client
      .get<AccountActivity[]>(`/${id}/transactions`, { params: filters })
      .then((response) => response.data),
  getAccountById: (id: string) =>
    client.get<AccountDetail>(`/${id}`).then((response) => response.data),
  requestAccountFreeze: (id: string, reason?: string) =>
    client.post<OtpChallenge>(`/${id}/freeze`, { reason }).then((response) => response.data),
  requestAccountUnfreeze: (id: string) =>
    client.post<OtpChallenge>(`/${id}/unfreeze`).then((response) => response.data),
  confirmChange: (changeRequestId: string, otpCode: string) =>
    client
      .post<AccountDetail>(`/changes/${changeRequestId}/confirm`, { otpCode })
      .then((response) => response.data),
  requestAccountLink: (payload: LinkAccountPayload) =>
    client.post<OtpChallenge>('/link', payload).then((response) => response.data),
  confirmAccountLink: (changeRequestId: string, otpCode: string) =>
    client
      .post<LinkedAccountResponse>(`/link/${changeRequestId}/confirm`, { otpCode })
      .then((response) => response.data),
  getAccountProducts: (accountType?: AccountType) =>
    client
      .get<AccountProduct[]>('/products', {
        params: accountType ? { accountType } : undefined,
      })
      .then((response) => response.data),
  requestAccountOpening: (payload: OpenAccountPayload) =>
    client.post<OtpChallenge>('/open', payload).then((response) => response.data),
  confirmAccountOpening: (changeRequestId: string, otpCode: string) =>
    client
      .post<LinkedAccountResponse>(`/open/${changeRequestId}/confirm`, { otpCode })
      .then((response) => response.data),
  requestCreditCardLink: (payload: LinkCreditCardPayload) =>
    client.post<OtpChallenge>('/cards/link', payload).then((response) => response.data),
  confirmCreditCardLink: (changeRequestId: string, otpCode: string) =>
    client
      .post<LinkedCardResponse>(`/cards/link/${changeRequestId}/confirm`, { otpCode })
      .then((response) => response.data),
  downloadStatement: (id: string) =>
    client
      .get<Blob>(`/${id}/statement`, { responseType: 'blob' as const })
      .then((response) => response.data),
};

export default accountsService;
