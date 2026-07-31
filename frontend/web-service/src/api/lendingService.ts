import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/loans');

export interface LoanApplicationPayload {
  purpose: string;
  amount: number;
  termMonths: number;
}

export interface LoanApplicationResponse {
  id: string;
  status: string;
  estimatedRate: number;
  createdAt: string;
}

export interface LoanDetail {
  id: string;
  name: string;
  currency: string;
  remainingBalance: number;
  installmentsPaid: number;
  installmentsTotal: number;
  nextPaymentDueDate: string;
  nextPaymentAmount: number;
  autoPayEnabled: boolean;
  autoPayAccountName: string;
}

export const lendingService = {
  client,
  applyForLoan: (payload: LoanApplicationPayload) =>
    client.post<LoanApplicationResponse>('/apply', payload).then((response) => response.data),
  getLoanDetails: (id: string) =>
    client.get<LoanDetail>(`/${id}`).then((response) => response.data),
};

export default lendingService;
