import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/transfers');

export interface CreateTransferPayload {
  fromAccountId: string;
  toAccount: string;
  amount: number;
  note?: string;
}

export interface CreateTransferResponse {
  id: string;
  status: string;
  fee: number;
  currency: string;
  createdAt: string;
}

export const transferService = {
  client,
  createTransfer: (payload: CreateTransferPayload) =>
    client.post<CreateTransferResponse>('/', payload).then((response) => response.data),
};

export default transferService;
