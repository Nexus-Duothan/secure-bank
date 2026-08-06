import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/transfers');

export type TransferStatus = 'PENDING_CONFIRMATION' | 'COMPLETED' | 'FAILED' | 'REJECTED';

export interface TransferQuotePayload {
  fromAccountId: string;
  toAccount: string;
  amount: number;
  note?: string;
}

export interface TransferResponse {
  id: string;
  status: TransferStatus;
  fromAccountId: string;
  toAccount: string;
  amount: number;
  fee: number;
  totalDebit: number;
  currency: string;
  note?: string;
  failureReason?: string;
  createdAt: string;
  confirmedAt?: string;
}

export const transferService = {
  client,
  quoteTransfer: (payload: TransferQuotePayload, idempotencyKey?: string) =>
    client
      .post<TransferResponse>('/quote', payload, {
        headers: idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : undefined,
      })
      .then((response) => response.data),
  confirmTransfer: (id: string, totpCode: string) =>
    client.post<TransferResponse>(`/${id}/confirm`, { totpCode }).then((response) => response.data),
  getTransfer: (id: string) =>
    client.get<TransferResponse>(`/${id}`).then((response) => response.data),
};

export default transferService;
