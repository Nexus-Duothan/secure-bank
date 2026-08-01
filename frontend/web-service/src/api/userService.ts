import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/users');

export interface UserProfile {
  id: string;
  fullName: string;
  email: string;
  role: string;
  idVerified: boolean;
  twoFactorEnabled: boolean;
  lastVerifiedSession: string;
  trustedDevicesCount: number;
  deviceVerified: boolean;
}

export const userService = {
  client,
  getProfile: () => client.get<UserProfile>('/me').then((response) => response.data),
};

export default userService;
