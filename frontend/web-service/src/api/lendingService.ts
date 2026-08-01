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

export interface PendingLoanApplication {
  id: string;
  applicantName: string;
  applicantId: string;
  purpose: string;
  currency: string;
  amount: number;
  termMonths: number;
  estimatedRate: number;
  status: string;
  submittedAt: string;
}

export const lendingService = {
  client,
  applyForLoan: (payload: LoanApplicationPayload) =>
    client.post<LoanApplicationResponse>('/apply', payload).then((response) => response.data),
  getLoanDetails: (id: string) =>
    client.get<LoanDetail>(`/${id}`).then((response) => response.data),
  // Officer-side view of the loan queue (FR-22/FR-23). Decisions happen in the
  // bank's lending system, so there is no review call here.
  getPendingApplications: () =>
    client.get<PendingLoanApplication[]>('/officer/pending').then((response) => response.data),
};

export default lendingService;
