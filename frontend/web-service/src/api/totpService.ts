import { createApiClient } from './createApiClient';

const publicClient = createApiClient('/api/v1/totp', { attachAccessToken: false });

export interface TotpSetupResponse {
  userId: string;
  secretKey: string;
  otpauthUrl: string;
  qrCodeBase64: string;
  scratchCodes: string[];
}

export interface TotpVerifyResponse {
  valid: boolean;
  message: string;
  usedScratchCode: boolean;
}

export const totpService = {
  setup: (userId: string, username?: string) =>
    publicClient
      .post<TotpSetupResponse>(`/setup/${userId}`, null, {
        params: username ? { username } : undefined,
      })
      .then((response) => response.data),
  enable: (userId: string, totpCode: string) =>
    publicClient
      .post<TotpVerifyResponse>('/enable', { userId, totpCode })
      .then((response) => response.data),
};

export default totpService;
