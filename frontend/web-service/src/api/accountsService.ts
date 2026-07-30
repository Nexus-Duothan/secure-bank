import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/accounts');

export type AccountType = 'SAVINGS' | 'CURRENT';

export interface Account {
  id: string;
  nickname: string;
  accountType: AccountType;
  lastFourDigits: string;
  balance: number;
  currency: string;
  monthlyChangePercent: number;
  verifiedLabel: string;
}

export interface Transaction {
  id: string;
  merchant: string;
  category: string;
  date: string;
  amount: number;
  verified: boolean;
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
  status: string;
}

export const accountsService = {
  client,
  getPrimaryAccount: () => client.get<Account>('/primary').then((response) => response.data),
  getRecentTransactions: (limit = 4) =>
    client
      .get<Transaction[]>('/primary/transactions', { params: { limit } })
      .then((response) => response.data),
  getAccountById: (id: string) =>
    client.get<AccountDetail>(`/${id}`).then((response) => response.data),
};

export default accountsService;
