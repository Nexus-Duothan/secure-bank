import { createApiClient } from './createApiClient';
import type { NotificationPreferences, OtpChallenge, UserProfile } from '../types';

const client = createApiClient('/api/v1/users');

export type { NotificationPreferences, OtpChallenge, UserDevice, UserProfile } from '../types';

interface BackendUserDevice {
  id: string;
  deviceName: string;
  deviceType: string;
  browser: string;
  location: string;
  trusted: boolean;
  lastVerifiedAt: string;
}

interface BackendUserProfile {
  id: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  addressLine: string;
  city: string;
  country: string;
  language: string;
  role: string;
  status: string;
  idVerified: boolean;
  frozen: boolean;
  freezeReason: string | null;
  notificationPreferences: NotificationPreferences;
  linkedDevices: BackendUserDevice[];
}

export interface ProfileUpdatePayload {
  fullName?: string;
  email?: string;
  phoneNumber?: string;
  addressLine?: string;
  city?: string;
  country?: string;
  language?: string;
}

export interface DeviceLinkPayload {
  deviceName: string;
  deviceType?: string;
  browser?: string;
  location?: string;
}

const mapProfile = (profile: BackendUserProfile): UserProfile => ({
  id: profile.id,
  fullName: profile.fullName,
  email: profile.email,
  phoneNumber: profile.phoneNumber,
  addressLine: profile.addressLine,
  city: profile.city,
  country: profile.country,
  language: profile.language,
  role: profile.role as UserProfile['role'],
  status: profile.status as UserProfile['status'],
  idVerified: profile.idVerified,
  frozen: profile.frozen,
  freezeReason: profile.freezeReason,
  notificationPreferences: profile.notificationPreferences,
  linkedDevices: profile.linkedDevices ?? [],
});

export const userService = {
  client,
  getProfile: () =>
    client.get<BackendUserProfile>('/me').then((response) => {
      return mapProfile(response.data);
    }),
  requestProfileUpdate: (payload: ProfileUpdatePayload) =>
    client.post<OtpChallenge>('/me/profile-change', payload).then((response) => response.data),
  requestNotificationPreferencesUpdate: (payload: NotificationPreferences) =>
    client
      .post<OtpChallenge>('/me/notification-preferences-change', payload)
      .then((response) => response.data),
  requestDeviceLink: (payload: DeviceLinkPayload) =>
    client.post<OtpChallenge>('/me/devices/link', payload).then((response) => response.data),
  requestDeviceTrust: (deviceId: string) =>
    client.post<OtpChallenge>('/me/devices/trust', { deviceId }).then((response) => response.data),
  requestDeviceRevoke: (deviceId: string) =>
    client.post<OtpChallenge>('/me/devices/revoke', { deviceId }).then((response) => response.data),
  requestAccountFreeze: (reason?: string) =>
    client.post<OtpChallenge>('/me/freeze', { reason }).then((response) => response.data),
  requestAccountUnfreeze: () =>
    client.post<OtpChallenge>('/me/unfreeze').then((response) => response.data),
  confirmChange: (changeRequestId: string, otpCode: string) =>
    client
      .post<BackendUserProfile>(`/me/changes/${changeRequestId}/confirm`, { otpCode })
      .then((response) => mapProfile(response.data)),
};

export default userService;
