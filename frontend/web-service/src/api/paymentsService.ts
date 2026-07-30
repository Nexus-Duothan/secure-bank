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

export const paymentsService = {
  client,
  payBill: (payload: PayBillPayload) =>
    client.post<PayBillResponse>('/bills', payload).then((response) => response.data),
};

export default paymentsService;
