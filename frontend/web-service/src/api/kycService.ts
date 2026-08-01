import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/auth/officer');

export type KycStatus = 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export type KycDocumentType = 'NATIONAL_ID' | 'PASSPORT';

export interface KycApplication {
  applicationId: string;
  userId: string;
  documentType: KycDocumentType;
  documentNumber: string;
  status: KycStatus;
  rejectionReason: string | null;
  submittedAt: string;
  reviewedAt: string | null;
  reviewedBy: string | null;
}

/**
 * Officer-side view of the KYC queue (FR-02). Approve/reject decisions happen
 * in the bank's core system, so this app only reads the queue.
 */
export const kycService = {
  client,
  getPendingApplications: () =>
    client.get<KycApplication[]>('/kyc/pending').then((response) => response.data),
};

export default kycService;
