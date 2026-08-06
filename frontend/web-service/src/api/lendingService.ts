import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/loans');

export type ApplicationStatus =
  'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'DISBURSED';
export type LoanStatus = 'ACTIVE' | 'DELINQUENT' | 'PAID_OFF';
export type InstallmentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'OVERDUE';

export interface LoanApplicationPayload {
  purpose: string;
  amount: number;
  termMonths: number;
  linkedAccountId: string;
}

export interface LoanApplicationResponse {
  id: string;
  purpose: string;
  amount: number;
  termMonths: number;
  annualInterestRate: number;
  linkedAccountId: string;
  status: ApplicationStatus;
  rejectionReason?: string;
  loanId?: string;
  createdAt: string;
}

export interface PendingLoanApplication {
  id: string;
  applicantUserId: string;
  purpose: string;
  currency: string;
  amount: number;
  termMonths: number;
  annualInterestRate: number;
  linkedAccountId: string;
  status: ApplicationStatus;
  createdAt: string;
}

export interface LoanDetail {
  id: string;
  applicationId: string;
  purpose: string;
  principal: number;
  annualInterestRate: number;
  termMonths: number;
  currency: string;
  linkedAccountId: string;
  status: LoanStatus;
  autopayEnabled: boolean;
  remainingBalance: number;
  installmentsPaid: number;
  installmentsTotal: number;
  nextInstallmentDueDate?: string;
  nextInstallmentAmount?: number;
  disbursedAt: string;
}

export interface LoanInstallment {
  id: string;
  installmentNumber: number;
  dueDate: string;
  principalAmount: number;
  interestAmount: number;
  totalAmount: number;
  remainingBalanceAfter: number;
  status: InstallmentStatus;
  paidAt?: string;
}

export const lendingService = {
  client,
  applyForLoan: (payload: LoanApplicationPayload) =>
    client.post<LoanApplicationResponse>('/apply', payload).then((response) => response.data),
  getApplication: (id: string) =>
    client.get<LoanApplicationResponse>(`/applications/${id}`).then((response) => response.data),
  listApplications: () =>
    client.get<LoanApplicationResponse[]>('/applications').then((response) => response.data),
  getPendingApplications: () =>
    client.get<PendingLoanApplication[]>('/officer/pending').then((response) => response.data),
  reviewApplication: (id: string, approve: boolean, note: string, totpCode: string) =>
    client
      .post<PendingLoanApplication>(`/officer/${id}/review`, { approve, note, totpCode })
      .then((response) => response.data),
  getLoanDetails: (id: string) =>
    client.get<LoanDetail>(`/${id}`).then((response) => response.data),
  listLoans: () => client.get<LoanDetail[]>('').then((response) => response.data),
  getInstallments: (id: string) =>
    client.get<LoanInstallment[]>(`/${id}/installments`).then((response) => response.data),
  payNow: (id: string) => client.post<LoanDetail>(`/${id}/pay`).then((response) => response.data),
  setAutopay: (id: string, enabled: boolean) =>
    client.patch<LoanDetail>(`/${id}/autopay`, { enabled }).then((response) => response.data),
};

export default lendingService;
