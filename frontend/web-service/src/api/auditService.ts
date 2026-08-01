import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/audit');

export interface AuditTransaction {
  id: string;
  merchant: string;
  category: string;
  location: string;
  amount: number;
  currency: string;
  timestamp: string;
  dateGroupLabel: string;
  journalId: string;
  flagged: boolean;
}

export const auditService = {
  client,
  getTransactions: () =>
    client.get<AuditTransaction[]>('/transactions').then((response) => response.data),
};

export default auditService;
