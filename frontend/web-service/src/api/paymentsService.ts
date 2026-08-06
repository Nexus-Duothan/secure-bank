import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/payments');

export interface PayBillPayload {
  billerCategory: string;
  billerName: string;
  referenceNumber: string;
  amount: number;
  fromAccountId: string;
  totpCode: string;
}

export interface PayBillResponse {
  id: string;
  status: string;
  currency: string;
  createdAt: string;
}

export interface HeldPayment {
  id: string;
  payerUserId: string;
  merchantCode: string;
  merchantName: string;
  amount: number;
  currency: string;
  channel: 'DIRECT' | 'QR';
  status: 'HELD_FOR_REVIEW' | 'COMPLETED' | 'DECLINED';
  note?: string;
  referenceNumber: string;
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

export type MerchantPaymentStatus =
  'COMPLETED' | 'PENDING' | 'HELD_FOR_REVIEW' | 'DECLINED' | 'REFUNDED';

export interface MerchantPayment {
  id: string;
  payerUserId: string;
  channel: 'DIRECT' | 'QR';
  referenceNumber: string;
  merchantName: string;
  currency: string;
  amount: number;
  status: MerchantPaymentStatus;
  createdAt: string;
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
  getMerchantSummary: () =>
    client.get<MerchantSummary>('/merchant/summary').then((response) => response.data),
  getMerchantPayments: () =>
    client.get<MerchantPayment[]>('/merchant/payments').then((response) => response.data),
  refundMerchantPayment: (id: string, totpCode: string) =>
    client
      .post<MerchantPayment>(`/merchant/payments/${id}/refund`, { totpCode })
      .then((response) => response.data),
  getMerchantSettlements: () =>
    client.get<MerchantSettlement[]>('/merchant/settlements').then((response) => response.data),
  getHeldPayments: () =>
    client.get<HeldPayment[]>('/officer/held').then((response) => response.data),
  reviewHeldPayment: (id: string, approve: boolean, note: string, totpCode: string) =>
    client
      .post<HeldPayment>(`/officer/${id}/review`, { approve, note, totpCode })
      .then((response) => response.data),
};

export default paymentsService;
