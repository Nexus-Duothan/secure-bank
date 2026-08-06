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

export const kycService = {
  client,
  getPendingApplications: () =>
    client.get<KycApplication[]>('/kyc/pending').then((response) => response.data),
  reviewApplication: (
    applicationId: string,
    action: 'APPROVED' | 'REJECTED',
    rejectionReason: string,
    totpCode: string
  ) =>
    client
      .post<KycApplication>(`/kyc/${applicationId}/review`, {
        action,
        rejectionReason,
        totpCode,
      })
      .then((response) => response.data),
};

export default kycService;
