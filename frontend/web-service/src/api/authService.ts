import { createApiClient } from './createApiClient';

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

export const authService = {
  client,
  login: (payload: LoginPayload) =>
    client.post<PreAuthResponse>('/login', payload).then((response) => response.data),
  verifyOtp: (payload: VerifyOtpPayload) =>
    client.post<AuthTokenResponse>('/login/verify-mfa', payload).then((response) => response.data),
  createAccount: (payload: CreateAccountPayload) =>
    client.post<CreateAccountResponse>('/register', payload).then((response) => response.data),
  requestPasswordReset: (email: string) =>
    client
      .post<RequestPasswordResetResponse>('/password-reset/request', { email })
      .then((response) => response.data),
  // Backend's confirm endpoint also requires a totpCode (MFA-protected reset) that
  // this screen doesn't collect yet, so a real backend will reject this until that's wired up.
  resetPassword: (token: string, newPassword: string) =>
    client
      .post<ResetPasswordResponse>('/password-reset/confirm', { token, newPassword })
      .then((response) => response.data),
  logout: () => client.post('/logout').then((response) => response.data),
};

export default authService;
