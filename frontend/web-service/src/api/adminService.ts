import { createApiClient } from './createApiClient';
import type { OtpChallenge, Role, UserProfile, UserStatus } from '../types';

const client = createApiClient('/api/v1/users/admin');

/** UNKNOWN means no address is configured for the service, so nothing was measured. */
export type ServiceHealthStatus = 'UP' | 'DOWN' | 'UNKNOWN';

export interface ServiceHealth {
  key: string;
  name: string;
  status: ServiceHealthStatus;
  /** Why it is not UP; null when it is. */
  detail: string | null;
  responseTimeMs: number | null;
  checkedAt: string;
}

export const adminService = {
  client,
  getUsers: () => client.get<UserProfile[]>('').then((response) => response.data),
  /** Probes every platform service; nothing is cached, so each call is a fresh measurement. */
  getSystemHealth: () =>
    client.get<ServiceHealth[]>('/system-health').then((response) => response.data),
  getUser: (userId: string) =>
    client.get<UserProfile>(`/${userId}`).then((response) => response.data),
  requestRoleChange: (userId: string, role: Role) =>
    client.post<OtpChallenge>(`/${userId}/role-change`, { role }).then((response) => response.data),
  requestStatusChange: (userId: string, status: UserStatus) =>
    client
      .post<OtpChallenge>(`/${userId}/status-change`, { status })
      .then((response) => response.data),
  confirmChange: (changeRequestId: string, otpCode: string) =>
    client
      .post<UserProfile>(`/changes/${changeRequestId}/confirm`, { otpCode })
      .then((response) => response.data),
};

export default adminService;
