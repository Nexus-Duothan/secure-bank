export type Role = 'CUSTOMER' | 'MERCHANT' | 'BANK_OFFICER' | 'ADMIN';

export type UserStatus = 'ACTIVE' | 'FROZEN' | 'SUSPENDED' | 'PENDING_REVIEW';

export type NotificationPreferences = {
  email: boolean;
  sms: boolean;
  push: boolean;
};

export type UserDevice = {
  id: string;
  deviceName: string;
  deviceType: string;
  browser: string;
  location: string;
  trusted: boolean;
  lastVerifiedAt: string;
};

export type UserProfile = {
  id: string;
  fullName: string;
  email: string;
  phoneNumber: string;
  addressLine: string;
  city: string;
  country: string;
  language: string;
  role: Role;
  status: UserStatus;
  idVerified: boolean;
  notificationPreferences: NotificationPreferences;
  linkedDevices: UserDevice[];
};

/**
 * Response to a staged change; the change only takes effect once confirmed with the current code
 * from the user's authenticator app (TOTP).
 */
export type OtpChallenge = {
  changeRequestId: string;
  type: string;
  deliveryTarget: string;
  expiresAt: string;
  message: string;
  /** Always null now that no code is generated or delivered by the backend. */
  demoCode: string | null;
};
