import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/payments');

export interface PayBillPayload {
  billerCategory: string;
  billerName: string;
  referenceNumber: string;
  amount: number;
  fromAccountId: string;
}

export interface PayBillResponse {
  id: string;
  status: string;
  currency: string;
  createdAt: string;
}

export interface MerchantSummary {
  merchantName: string;
  currency: string;
  todayTotal: number;
  paymentsToday: number;
  refundsToday: number;
  pendingSettlement: number;
  nextPayoutDate: string;
}

export type MerchantPaymentStatus = 'SETTLED' | 'PENDING' | 'REFUNDED';

export interface MerchantPayment {
  id: string;
  payerName: string;
  method: string;
  reference: string;
  currency: string;
  amount: number;
  status: MerchantPaymentStatus;
  timestamp: string;
}

export interface MerchantSettlement {
  id: string;
  periodLabel: string;
  currency: string;
  grossAmount: number;
  fees: number;
  netAmount: number;
  status: 'PAID' | 'SCHEDULED';
  payoutDate: string;
}

export const paymentsService = {
  client,
  payBill: (payload: PayBillPayload) =>
    client.post<PayBillResponse>('/bills', payload).then((response) => response.data),
  // Merchant portal (FR-15/FR-20): view of takings and settlements. Refunds are
  // handled in the bank's merchant system, so there is no refund call here.
  getMerchantSummary: () =>
    client.get<MerchantSummary>('/merchant/summary').then((response) => response.data),
  getMerchantPayments: () =>
    client.get<MerchantPayment[]>('/merchant/payments').then((response) => response.data),
  getMerchantSettlements: () =>
    client.get<MerchantSettlement[]>('/merchant/settlements').then((response) => response.data),
};

export default paymentsService;
