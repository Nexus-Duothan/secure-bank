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

export const authService = {
  client,
  login: (payload: LoginPayload) =>
    client.post<PreAuthResponse>('/login', payload).then((response) => response.data),
};

export default authService;
