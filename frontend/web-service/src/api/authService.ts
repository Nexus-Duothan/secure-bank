import { createApiClient } from './createApiClient';
import tokenStorage from './tokenStorage';

const publicClient = createApiClient('/api/v1/auth', { attachAccessToken: false });
const client = createApiClient('/api/v1/auth');

export interface LoginPayload {
  usernameOrEmail: string;
  password: string;
}

export interface PreAuthResponse {
  preAuthToken: string;
  mfaRequired: boolean;
  message: string;
}

export interface VerifyOtpPayload {
  preAuthToken: string;
  totpCode: string;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresInSeconds: number;
  userId: string;
  username: string;
  role: string;
  status: string;
}

export type AccountType = 'SAVINGS' | 'CURRENT';

export interface CreateAccountPayload {
  fullName: string;
  dateOfBirth: string;
  email: string;
  phoneNumber: string;
  address: string;
  nationalIdOrPassport: string;
  accountType: AccountType;
  username: string;
  password: string;
}

export interface CreateAccountResponse {
  userId: string;
  username: string;
  email: string;
  role: string;
  status: string;
  message: string;
}

export interface RequestPasswordResetResponse {
  message: string;
  token: string;
}

export interface ResetPasswordResponse {
  message: string;
}

export interface SessionInfo {
  sessionId: string;
  ipAddress: string | null;
  userAgent: string | null;
  deviceInfo: string | null;
  createdAt: string;
  lastActiveAt: string;
  expiresAt: string;
  current: boolean;
}

export const authService = {
  client,
  login: (payload: LoginPayload) =>
    publicClient.post<PreAuthResponse>('/login', payload).then((response) => response.data),
  verifyOtp: (payload: VerifyOtpPayload) =>
    publicClient
      .post<AuthTokenResponse>('/login/verify-mfa', payload)
      .then((response) => response.data),
  resendOtp: (preAuthToken: string, usernameOrEmail?: string) =>
    publicClient
      .post<{ success: boolean; message: string }>('/resend-otp', {
        preAuthToken,
        usernameOrEmail,
      })
      .then((response) => response.data),
  createAccount: (payload: CreateAccountPayload) =>
    publicClient
      .post<CreateAccountResponse>('/register', payload)
      .then((response) => response.data),
  requestPasswordReset: (email: string) =>
    publicClient
      .post<RequestPasswordResetResponse>('/password-reset/request', { email })
      .then((response) => response.data),
  resetPassword: (token: string, newPassword: string, totpCode: string) =>
    publicClient
      .post<ResetPasswordResponse>('/password-reset/confirm', { token, newPassword, totpCode })
      .then((response) => response.data),
  getSessions: () => {
    const refreshToken = tokenStorage.getRefreshToken();
    return client
      .get<SessionInfo[]>('/sessions', {
        headers: refreshToken ? { 'X-Refresh-Token': refreshToken } : undefined,
      })
      .then((response) => response.data);
  },
  revokeSession: (sessionId: string) => client.delete(`/sessions/${sessionId}`),
  logout: async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    if (!refreshToken) return;
    const sessions = await client.get<SessionInfo[]>('/sessions', {
      headers: { 'X-Refresh-Token': refreshToken },
    });
    const currentSession = sessions.data.find((session) => session.current);
    if (currentSession) {
      await client.delete(`/sessions/${currentSession.sessionId}`);
    }
  },
};

export default authService;
