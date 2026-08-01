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

/** Response to a staged change; the change only takes effect once confirmed with the code. */
export type OtpChallenge = {
  changeRequestId: string;
  type: string;
  deliveryTarget: string;
  expiresAt: string;
  message: string;
  /** Only populated while the service runs with `securebank.user.otp.expose-code` enabled. */
  demoCode: string | null;
};
