import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Avatar,
  Button,
  Flex,
  Form,
  Input,
  Modal,
  Select,
  Spin,
  Switch,
  Typography,
  message,
  theme,
} from 'antd';
import {
  CameraOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  EditOutlined,
  LeftOutlined,
  LogoutOutlined,
  RightOutlined,
  SafetyCertificateOutlined,
  StopOutlined,
  VerifiedOutlined,
} from '@ant-design/icons';
import authService from '../../api/authService';
import { getApiErrorMessage } from '../../api/apiError';
import userService, {
  type NotificationPreferences,
  type OtpChallenge,
  type ProfileUpdatePayload,
  type UserDevice,
  type UserProfile,
} from '../../api/userService';
import accountsService, { type Account } from '../../api/accountsService';
import tokenStorage from '../../api/tokenStorage';
import sessionUser from '../../api/sessionUser';
import TrustIndicator from '../../components/TrustIndicator';
import BottomNav from '../../components/BottomNav';
import { PasswordStrengthMeter } from '../../components/PasswordStrength';
import { PASSWORD_COMPLEXITY_MESSAGE, PASSWORD_PATTERN } from '../../components/passwordRules';
import { getProfilePhoto, saveProfilePhoto } from '../../utils/profilePhoto';

const { Text, Title } = Typography;

const NAVY = '#0B1B2B';
const TEAL_TINT = '#DCEFEA';
const LANGUAGE_OPTIONS = ['English', 'Sinhala', 'Tamil'];
const OVERVIEW_PANEL = 'overview';
const PROFILE_PANELS = ['personal', 'notifications', 'language', 'devices', 'password'] as const;

type ProfilePanel = (typeof PROFILE_PANELS)[number] | typeof OVERVIEW_PANEL;

interface ProfileLocationState {
  otpSuccessMessage?: string;
}

interface PasswordFormValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

interface SettingsRowItem {
  key: string;
  label: string;
  trailing?: string;
  editable?: boolean;
  onClick?: () => void;
}

interface SettingsRowProps extends SettingsRowItem {
  showDivider: boolean;
}

interface SecurityStatusRowProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  tone?: 'success' | 'warning' | 'danger';
}

interface DeviceRowProps {
  device: UserDevice;
  busy: boolean;
  disabled?: boolean;
  onVerify: (deviceId: string) => void;
  onRevoke: (deviceId: string) => void;
}

const DEFAULT_USER_PROFILE: UserProfile = {
  id: '',
  fullName: 'User Profile',
  email: '',
  phoneNumber: '',
  addressLine: '',
  city: '',
  country: '',
  language: 'English',
  role: 'CUSTOMER',
  status: 'ACTIVE',
  idVerified: true,
  notificationPreferences: { email: true, sms: true, push: true },
  linkedDevices: [],
};

const getInitials = (name: string) =>
  name
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase();

const formatNotificationSummary = (preferences: NotificationPreferences) => {
  const enabled = [
    preferences.email ? 'Email' : null,
    preferences.sms ? 'SMS' : null,
    preferences.push ? 'App alerts' : null,
  ].filter(Boolean);

  return enabled.length > 0 ? enabled.join(' / ') : 'None';
};

const formatDeviceCount = (count: number) => `${count} linked`;

const formatLastVerifiedSession = (devices: UserDevice[]) => {
  const latest = devices
    .filter((device) => Boolean(device.lastVerifiedAt))
    .sort((left, right) => right.lastVerifiedAt.localeCompare(left.lastVerifiedAt))[0];

  if (!latest) {
    return 'No verified sessions';
  }

  const timestamp = new Date(latest.lastVerifiedAt);
  if (Number.isNaN(timestamp.getTime())) {
    return 'Recently verified';
  }

  const now = new Date();
  if (timestamp.toDateString() === now.toDateString()) {
    return `Today ${timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
  }

  return timestamp.toLocaleDateString([], { day: '2-digit', month: 'short' });
};

const formatDeviceSubtitle = (device: UserDevice) =>
  [device.deviceType, device.browser, device.location].filter(Boolean).join(' / ');

const getPanelFromSearch = (value: string | null): ProfilePanel =>
  PROFILE_PANELS.includes(value as (typeof PROFILE_PANELS)[number])
    ? (value as ProfilePanel)
    : OVERVIEW_PANEL;

const SettingsRow: React.FC<SettingsRowProps> = ({
  label,
  trailing,
  editable,
  onClick,
  showDivider,
}) => {
  const { token } = theme.useToken();

  return (
    <Flex
      justify="space-between"
      align="center"
      onClick={onClick}
      style={{
        padding: '16px 20px',
        borderBottom: showDivider ? `1px solid ${token.colorBorder}` : 'none',
        cursor: onClick ? 'pointer' : 'default',
      }}
    >
      <Text style={{ fontSize: 15, fontWeight: 600, color: token.colorText }}>{label}</Text>
      <Flex align="center" gap={8}>
        {trailing && (
          <Text style={{ fontSize: 14, color: token.colorTextTertiary }}>{trailing}</Text>
        )}
        {editable ? (
          <EditOutlined style={{ fontSize: 14, color: token.colorPrimary }} />
        ) : (
          <RightOutlined style={{ fontSize: 12, color: token.colorTextTertiary }} />
        )}
      </Flex>
    </Flex>
  );
};

const SecurityStatusRow: React.FC<SecurityStatusRowProps> = ({
  icon,
  label,
  value,
  tone = 'success',
}) => {
  const { token } = theme.useToken();
  const toneStyles = {
    success: {
      bg: '#E7F6F1',
      border: '#B7E4D9',
      ink: token.colorPrimary,
      dot: '#19C59F',
    },
    warning: {
      bg: '#FFF7E6',
      border: '#FFD591',
      ink: '#AD6800',
      dot: '#FAAD14',
    },
    danger: {
      bg: '#FFF2F0',
      border: '#FFCCC7',
      ink: token.colorError,
      dot: token.colorError,
    },
  }[tone];

  return (
    <Flex
      align="center"
      gap={12}
      style={{
        padding: '14px 12px',
        borderRadius: 12,
        background: token.colorFillAlter,
        minWidth: 0,
      }}
    >
      <Flex
        align="center"
        justify="center"
        style={{
          width: 36,
          height: 36,
          borderRadius: 10,
          background: toneStyles.bg,
          color: toneStyles.ink,
          flexShrink: 0,
        }}
      >
        {icon}
      </Flex>
      <div style={{ minWidth: 0, flex: 1 }}>
        <Text
          style={{
            display: 'block',
            marginBottom: 6,
            color: token.colorTextSecondary,
            fontSize: 12,
            fontWeight: 600,
          }}
        >
          {label}
        </Text>
        <Flex
          align="center"
          gap={6}
          style={{
            width: 'fit-content',
            maxWidth: '100%',
            padding: '4px 9px',
            borderRadius: 999,
            border: `1px solid ${toneStyles.border}`,
            background: toneStyles.bg,
          }}
        >
          <span
            style={{
              width: 6,
              height: 6,
              borderRadius: '50%',
              background: toneStyles.dot,
              flexShrink: 0,
            }}
          />
          <Text
            style={{
              color: toneStyles.ink,
              fontSize: 12,
              fontWeight: 700,
              whiteSpace: 'nowrap',
            }}
          >
            {value}
          </Text>
        </Flex>
      </div>
    </Flex>
  );
};

const DeviceRow: React.FC<DeviceRowProps> = ({ device, busy, disabled, onVerify, onRevoke }) => {
  const { token } = theme.useToken();

  return (
    <Flex
      justify="space-between"
      align="center"
      style={{
        padding: '16px 18px',
        borderRadius: 16,
        border: `1px solid ${token.colorBorder}`,
        background: token.colorBgContainer,
      }}
    >
      <div style={{ minWidth: 0 }}>
        <Text style={{ display: 'block', fontSize: 15, fontWeight: 600, color: token.colorText }}>
          {device.deviceName}
        </Text>
        <Text
          style={{
            display: 'block',
            marginTop: 4,
            fontSize: 13,
            lineHeight: 1.5,
            color: token.colorTextSecondary,
          }}
        >
          {formatDeviceSubtitle(device)}
        </Text>
        <Text
          style={{
            display: 'block',
            marginTop: 4,
            fontSize: 12,
            color: device.trusted ? token.colorPrimary : token.colorWarning,
          }}
        >
          {device.trusted
            ? `Trusted / ${formatLastVerifiedSession([device])}`
            : 'Waiting for primary-device approval'}
        </Text>
      </div>

      <Button
        type="link"
        disabled={busy || disabled}
        style={{
          color: device.trusted ? token.colorError : token.colorPrimary,
          fontWeight: 600,
          paddingInline: 0,
        }}
        onClick={() => (device.trusted ? onRevoke(device.id) : onVerify(device.id))}
      >
        {device.trusted ? 'Revoke' : 'Verify'}
      </Button>
    </Flex>
  );
};

const Profile: React.FC = () => {
  const { token } = theme.useToken();
  const [messageApi, contextHolder] = message.useMessage();
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  const [personalForm] = Form.useForm<ProfileUpdatePayload>();
  const [notificationForm] = Form.useForm<NotificationPreferences>();
  const [languageForm] = Form.useForm<{ language: string }>();
  const [freezeForm] = Form.useForm<{ reason: string }>();
  const [passwordForm] = Form.useForm<PasswordFormValues>();
  const [profile, setProfile] = useState<UserProfile>(DEFAULT_USER_PROFILE);
  const [account, setAccount] = useState<Account | null>(null);
  const [loadingProfile, setLoadingProfile] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [deviceActionId, setDeviceActionId] = useState<string | null>(null);
  const [freezeModalOpen, setFreezeModalOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [profilePhoto, setProfilePhoto] = useState<string | null>(null);
  const [profileLoadError, setProfileLoadError] = useState<string | null>(null);
  const [accountLoadError, setAccountLoadError] = useState<string | null>(null);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const newPassword = Form.useWatch('newPassword', passwordForm) ?? '';

  const activePanel = getPanelFromSearch(searchParams.get('panel'));
  const trustedDeviceCount = useMemo(
    () => profile.linkedDevices.filter((device) => device.trusted).length,
    [profile.linkedDevices]
  );
  const pendingDevices = useMemo(
    () => profile.linkedDevices.filter((device) => !device.trusted),
    [profile.linkedDevices]
  );
  const linkedDevices = useMemo(
    () => profile.linkedDevices.filter((device) => device.trusted),
    [profile.linkedDevices]
  );
  const hasDeviceCapacity = trustedDeviceCount < 3;
  const sessionExpired = useMemo(() => {
    const message = profileLoadError?.toLowerCase() ?? '';
    return (
      message.includes('access token') ||
      message.includes('refresh token') ||
      message.includes('unauthorized') ||
      message.includes('401')
    );
  }, [profileLoadError]);

  useEffect(() => {
    const routeState = (location.state as ProfileLocationState | null) ?? null;
    if (!routeState?.otpSuccessMessage) {
      return;
    }

    messageApi.success(routeState.otpSuccessMessage);
    navigate(`${location.pathname}${location.search}`, { replace: true });
  }, [location.pathname, location.search, location.state, messageApi, navigate]);

  useEffect(() => {
    let cancelled = false;

    const applyProfileToForms = (nextProfile: UserProfile) => {
      personalForm.setFieldsValue({
        fullName: nextProfile.fullName,
        email: nextProfile.email,
        phoneNumber: nextProfile.phoneNumber,
        addressLine: nextProfile.addressLine,
        city: nextProfile.city,
        country: nextProfile.country,
      });
      notificationForm.setFieldsValue(nextProfile.notificationPreferences);
      languageForm.setFieldsValue({ language: nextProfile.language });
    };

    userService
      .getProfile()
      .then((data) => {
        if (!cancelled) {
          setProfile(data);
          setProfilePhoto(getProfilePhoto(data.id));
          setProfileLoadError(null);
          applyProfileToForms(data);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setProfileLoadError(
            getApiErrorMessage(error, 'Unable to load live profile details right now.')
          );
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingProfile(false);
        }
      });

    accountsService
      .getPrimaryAccount()
      .then((data) => {
        if (!cancelled) {
          setAccount(data);
          setAccountLoadError(null);
        }
      })
      .catch((error) => {
        if (!cancelled) {
          setAccount(null);
          setAccountLoadError(
            getApiErrorMessage(error, 'Live account details are unavailable right now.')
          );
        }
      });

    return () => {
      cancelled = true;
    };
  }, [languageForm, notificationForm, personalForm]);

  const handleProfilePhotoChange = async (file?: File) => {
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      messageApi.error('Please choose an image file.');
      return;
    }

    const photoDataUrl = await new Promise<string>((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result ?? ''));
      reader.onerror = () => reject(new Error('Could not read the selected image.'));
      reader.readAsDataURL(file);
    }).catch(() => '');

    if (!photoDataUrl) {
      messageApi.error('We could not upload the selected image.');
      return;
    }

    saveProfilePhoto(profile.id, photoDataUrl);
    setProfilePhoto(photoDataUrl);
    messageApi.success('Profile picture updated.');
  };

  const setPanel = (panel: ProfilePanel) => {
    if (panel === OVERVIEW_PANEL) {
      setSearchParams({});
      return;
    }

    setSearchParams({ panel });
  };

  const requestOtpFlow = async (
    action: Promise<OtpChallenge>,
    successMessage: string,
    afterChallenge?: () => void,
    flow: 'profile-change' | 'account-change' = 'profile-change'
  ) => {
    setSubmitting(true);
    try {
      const challenge = await action;
      afterChallenge?.();
      setSubmitting(false);
      setDeviceActionId(null);
      const returnTo =
        activePanel === OVERVIEW_PANEL ? '/profile' : `/profile?panel=${activePanel}`;
      navigate('/verify-otp', {
        state: {
          flow,
          challenge,
          successMessage,
          returnTo,
        },
      });
    } catch (error) {
      setSubmitting(false);
      setDeviceActionId(null);
      messageApi.error(
        getApiErrorMessage(error, 'We could not start the verification step. Please try again.')
      );
    }
  };

  const handlePersonalSave = async (values: ProfileUpdatePayload) => {
    const payload: ProfileUpdatePayload = {};

    if (values.fullName?.trim() !== profile.fullName) payload.fullName = values.fullName?.trim();
    if (values.email?.trim() !== profile.email) payload.email = values.email?.trim();
    if ((values.phoneNumber ?? '') !== (profile.phoneNumber ?? '')) {
      payload.phoneNumber = values.phoneNumber ?? '';
    }
    if ((values.addressLine ?? '') !== (profile.addressLine ?? '')) {
      payload.addressLine = values.addressLine ?? '';
    }
    if ((values.city ?? '') !== (profile.city ?? '')) payload.city = values.city ?? '';
    if ((values.country ?? '') !== (profile.country ?? '')) payload.country = values.country ?? '';

    if (Object.keys(payload).length === 0) {
      messageApi.info('No personal details have changed.');
      return;
    }

    await requestOtpFlow(
      userService.requestProfileUpdate(payload),
      'Personal details updated successfully.'
    );
  };

  const handleNotificationSave = async (values: NotificationPreferences) => {
    if (
      values.email === profile.notificationPreferences.email &&
      values.sms === profile.notificationPreferences.sms &&
      values.push === profile.notificationPreferences.push
    ) {
      messageApi.info('Notification methods are already up to date.');
      return;
    }

    await requestOtpFlow(
      userService.requestNotificationPreferencesUpdate(values),
      'Notification methods updated successfully.'
    );
  };

  const handleLanguageSave = async (values: { language: string }) => {
    const nextLanguage = values.language?.trim();
    if (!nextLanguage || nextLanguage === profile.language) {
      messageApi.info('Language preference is already up to date.');
      return;
    }

    await requestOtpFlow(
      userService.requestProfileUpdate({ language: nextLanguage }),
      'Language preference updated successfully.'
    );
  };

  const handleVerifyDevice = async (deviceId: string) => {
    const targetDevice = profile.linkedDevices.find((device) => device.id === deviceId);
    if (!targetDevice) {
      messageApi.error('We could not find that device.');
      return;
    }
    if (!targetDevice.trusted && !hasDeviceCapacity) {
      messageApi.error('Maximum linked devices reached.');
      return;
    }

    setDeviceActionId(deviceId);
    await requestOtpFlow(
      userService.requestDeviceTrust(deviceId),
      'The device has been verified successfully.'
    );
  };

  const handleRevokeDevice = async (deviceId: string) => {
    setDeviceActionId(deviceId);
    await requestOtpFlow(
      userService.requestDeviceRevoke(deviceId),
      'The device has been revoked successfully.'
    );
  };

  const handleFreezeSubmit = async () => {
    if (!account) return;
    const values = await freezeForm.validateFields();
    await requestOtpFlow(
      accountsService.requestAccountFreeze(account.id, values.reason?.trim() || undefined),
      'Your account has been frozen.',
      () => {
        setFreezeModalOpen(false);
        freezeForm.resetFields();
      },
      'account-change'
    );
  };

  const handleFreezeToggle = async () => {
    if (!account) return;
    if (account.frozen) {
      await requestOtpFlow(
        accountsService.requestAccountUnfreeze(account.id),
        'Your account has been reactivated.',
        undefined,
        'account-change'
      );
      return;
    }

    setFreezeModalOpen(true);
  };

  /**
   * Checks the current password with the backend and stages the new one. The password only changes
   * after the authenticator step, which then signs the customer out to sign in again.
   */
  const handlePasswordSave = async (values: PasswordFormValues) => {
    setPasswordSubmitting(true);
    try {
      const challenge = await authService.requestPasswordChange({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      passwordForm.resetFields();
      navigate('/verify-otp', { state: { flow: 'password-change', challenge } });
    } catch (error) {
      messageApi.error(
        getApiErrorMessage(error, 'We could not start the password change. Please try again.')
      );
    } finally {
      setPasswordSubmitting(false);
    }
  };

  const handleReauthenticate = () => {
    tokenStorage.clear();
    sessionUser.clear();
    navigate('/login', { replace: true });
  };

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await authService.logout();
    } catch {
      // If the backend session revoke fails, still clear local tokens.
    } finally {
      tokenStorage.clear();
      sessionUser.clear();
      navigate('/login', { replace: true });
    }
  };

  const sections: { label: string; rows: SettingsRowItem[] }[] = [
    {
      label: 'Account',
      rows: [
        {
          key: 'personal',
          label: 'Personal details',
          trailing: 'Update',
          editable: true,
          onClick: () => setPanel('personal'),
        },
        {
          key: 'notifications',
          label: 'Notification methods',
          trailing: formatNotificationSummary(profile.notificationPreferences),
          editable: true,
          onClick: () => setPanel('notifications'),
        },
        {
          key: 'language',
          label: 'Language',
          trailing: profile.language,
          editable: true,
          onClick: () => setPanel('language'),
        },
      ],
    },
    {
      label: 'Security',
      rows: [
        {
          key: 'devices',
          label: 'Linked devices',
          trailing: formatDeviceCount(trustedDeviceCount),
          editable: true,
          onClick: () => setPanel('devices'),
        },
        {
          key: 'password',
          label: 'Change password',
          trailing: 'Update',
          editable: true,
          onClick: () => setPanel('password'),
        },
        {
          key: 'freeze',
          label: account?.frozen ? 'Reactivate account' : 'Freeze account',
          trailing: account?.frozen ? 'Reactivate' : 'Protect',
          onClick: accountLoadError || !account ? undefined : handleFreezeToggle,
        },
      ],
    },
  ];

  const renderPanelShell = (title: string, children: React.ReactNode) => (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      {contextHolder}
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 24 }}>
          <LeftOutlined
            onClick={() => setPanel(OVERVIEW_PANEL)}
            style={{ fontSize: 20, color: token.colorText, cursor: 'pointer' }}
          />
          <Title
            level={3}
            className="font-display"
            style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
          >
            {title}
          </Title>
        </Flex>
        {profileLoadError && (
          <div
            style={{
              marginBottom: 16,
              padding: '12px 14px',
              borderRadius: 12,
              background: '#FFF7E6',
              border: '1px solid #FFD591',
            }}
          >
            <Flex vertical gap={10}>
              <Text style={{ color: '#AD6800', fontWeight: 500 }}>
                {sessionExpired
                  ? 'Your session has expired. Please sign in again to continue secure changes.'
                  : `${profileLoadError} Profile updates are disabled until \`user-service\` is running.`}
              </Text>
              {sessionExpired && (
                <Button
                  type="primary"
                  onClick={handleReauthenticate}
                  style={{ alignSelf: 'flex-start' }}
                >
                  Sign in again
                </Button>
              )}
            </Flex>
          </div>
        )}
        {children}
      </div>
      <BottomNav accountsPath={account ? `/accounts/${account.id}` : '/accounts'} />
    </div>
  );

  if (loadingProfile) {
    return (
      <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
        <Flex justify="center" align="center" style={{ minHeight: '100vh' }}>
          <Spin size="large" />
        </Flex>
      </div>
    );
  }

  if (activePanel === 'personal') {
    return renderPanelShell(
      'Personal details',
      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 20,
          border: `1px solid ${token.colorBorder}`,
          padding: 24,
        }}
      >
        <Form form={personalForm} layout="vertical" onFinish={handlePersonalSave}>
          <Flex justify="center" style={{ marginBottom: 28 }}>
            <div style={{ position: 'relative' }}>
              <Avatar
                size={96}
                src={profilePhoto ?? undefined}
                style={{
                  background: NAVY,
                  color: '#FFFFFF',
                  fontWeight: 600,
                  fontSize: 28,
                }}
              >
                {!profilePhoto ? getInitials(profile.fullName) : null}
              </Avatar>
              <label
                htmlFor="profile-photo-input"
                style={{
                  position: 'absolute',
                  right: -2,
                  bottom: -2,
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: token.colorBgContainer,
                  border: `1px solid ${token.colorBorder}`,
                  color: token.colorPrimary,
                  cursor: 'pointer',
                  boxShadow: '0 6px 18px rgba(11, 27, 43, 0.12)',
                }}
              >
                <CameraOutlined />
              </label>
              <input
                id="profile-photo-input"
                type="file"
                accept="image/*"
                onChange={(event) => void handleProfilePhotoChange(event.target.files?.[0])}
                style={{ display: 'none' }}
              />
            </div>
          </Flex>
          <Form.Item label="Full name" name="fullName" rules={[{ required: true }]}>
            <Input size="large" />
          </Form.Item>
          <Form.Item label="Email" name="email" rules={[{ required: true, type: 'email' }]}>
            <Input size="large" />
          </Form.Item>
          <Form.Item label="Phone number" name="phoneNumber">
            <Input size="large" />
          </Form.Item>
          <Form.Item label="Address" name="addressLine">
            <Input size="large" />
          </Form.Item>
          <Form.Item label="City" name="city">
            <Input size="large" />
          </Form.Item>
          <Form.Item label="Country" name="country" style={{ marginBottom: 24 }}>
            <Input size="large" />
          </Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            block
            size="large"
            disabled={Boolean(profileLoadError)}
            loading={submitting}
            style={{ height: 52, fontWeight: 600 }}
          >
            Save
          </Button>
        </Form>
      </div>
    );
  }

  if (activePanel === 'notifications') {
    return renderPanelShell(
      'Notification methods',
      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 20,
          border: `1px solid ${token.colorBorder}`,
          padding: 24,
        }}
      >
        <Form form={notificationForm} layout="vertical" onFinish={handleNotificationSave}>
          <Flex vertical gap={20} style={{ marginBottom: 28 }}>
            <Flex justify="space-between" align="center">
              <Text style={{ fontSize: 16, fontWeight: 600 }}>Email</Text>
              <Form.Item name="email" valuePropName="checked" noStyle>
                <Switch />
              </Form.Item>
            </Flex>
            <Flex justify="space-between" align="center">
              <Text style={{ fontSize: 16, fontWeight: 600 }}>SMS</Text>
              <Form.Item name="sms" valuePropName="checked" noStyle>
                <Switch />
              </Form.Item>
            </Flex>
            <Flex justify="space-between" align="center">
              <Text style={{ fontSize: 16, fontWeight: 600 }}>App alerts</Text>
              <Form.Item name="push" valuePropName="checked" noStyle>
                <Switch />
              </Form.Item>
            </Flex>
          </Flex>
          <Text
            style={{
              display: 'block',
              marginBottom: 24,
              fontSize: 13,
              lineHeight: 1.6,
              color: token.colorTextTertiary,
            }}
          >
            Security alerts are always delivered by SMS and email. Your preferences below apply to
            routine account and banking updates, including in-app alerts.
          </Text>
          <Button
            type="primary"
            htmlType="submit"
            block
            size="large"
            disabled={Boolean(profileLoadError)}
            loading={submitting}
            style={{ height: 52, fontWeight: 600 }}
          >
            Save
          </Button>
        </Form>
      </div>
    );
  }

  if (activePanel === 'language') {
    return renderPanelShell(
      'Language',
      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 20,
          border: `1px solid ${token.colorBorder}`,
          padding: 24,
        }}
      >
        <Form form={languageForm} layout="vertical" onFinish={handleLanguageSave}>
          <Form.Item label="Preferred language" name="language" rules={[{ required: true }]}>
            <Select
              size="large"
              options={LANGUAGE_OPTIONS.map((language) => ({ label: language, value: language }))}
            />
          </Form.Item>
          <Button
            type="primary"
            htmlType="submit"
            block
            size="large"
            disabled={Boolean(profileLoadError)}
            loading={submitting}
            style={{ height: 52, fontWeight: 600 }}
          >
            Save
          </Button>
        </Form>
      </div>
    );
  }

  if (activePanel === 'devices') {
    return renderPanelShell(
      'Linked devices',
      <div>
        {!hasDeviceCapacity && pendingDevices.length > 0 && (
          <div
            style={{
              marginBottom: 20,
              padding: '12px 14px',
              borderRadius: 12,
              background: '#FFF2F0',
              border: '1px solid #FFCCC7',
            }}
          >
            <Text style={{ color: token.colorError, fontWeight: 500 }}>
              Maximum linked devices reached.
            </Text>
          </div>
        )}

        <div
          style={{
            background: token.colorBgContainer,
            borderRadius: 20,
            border: `1px solid ${token.colorBorder}`,
            padding: 24,
          }}
        >
          {pendingDevices.length > 0 && (
            <>
              <Text
                style={{
                  display: 'block',
                  marginBottom: 10,
                  fontSize: 12,
                  fontWeight: 600,
                  letterSpacing: 0.4,
                  textTransform: 'uppercase',
                  color: token.colorTextTertiary,
                }}
              >
                Pending verification
              </Text>
              <Flex vertical gap={12} style={{ marginBottom: linkedDevices.length > 0 ? 24 : 0 }}>
                {pendingDevices.map((device) => (
                  <DeviceRow
                    key={device.id}
                    device={device}
                    busy={submitting && deviceActionId === device.id}
                    disabled={Boolean(profileLoadError)}
                    onVerify={handleVerifyDevice}
                    onRevoke={handleRevokeDevice}
                  />
                ))}
              </Flex>
            </>
          )}

          <Text
            style={{
              display: 'block',
              marginBottom: 10,
              fontSize: 12,
              fontWeight: 600,
              letterSpacing: 0.4,
              textTransform: 'uppercase',
              color: token.colorTextTertiary,
            }}
          >
            Linked devices
          </Text>
          <Flex vertical gap={12}>
            {linkedDevices.map((device) => (
              <DeviceRow
                key={device.id}
                device={device}
                busy={submitting && deviceActionId === device.id}
                disabled={Boolean(profileLoadError)}
                onVerify={handleVerifyDevice}
                onRevoke={handleRevokeDevice}
              />
            ))}
          </Flex>
        </div>
      </div>
    );
  }

  if (activePanel === 'password') {
    return renderPanelShell(
      'Change password',
      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 20,
          border: `1px solid ${token.colorBorder}`,
          padding: 24,
        }}
      >
        <Text
          style={{
            display: 'block',
            marginBottom: 24,
            fontSize: 14,
            lineHeight: 1.6,
            color: token.colorTextSecondary,
          }}
        >
          Enter your current password and choose a new one. After you save, we ask for the code from
          your authenticator app. You then sign in again with the new password.
        </Text>

        <Form<PasswordFormValues>
          form={passwordForm}
          layout="vertical"
          requiredMark={false}
          disabled={passwordSubmitting}
          onFinish={handlePasswordSave}
        >
          <Form.Item
            label="Current password"
            name="currentPassword"
            rules={[{ required: true, message: 'Enter your current password' }]}
          >
            <Input.Password size="large" autoComplete="current-password" />
          </Form.Item>

          <Form.Item
            label="New password"
            name="newPassword"
            style={{ marginBottom: 8 }}
            rules={[
              { required: true, message: 'Create a new password' },
              { pattern: PASSWORD_PATTERN, message: PASSWORD_COMPLEXITY_MESSAGE },
            ]}
          >
            <Input.Password size="large" autoComplete="new-password" />
          </Form.Item>

          <PasswordStrengthMeter password={newPassword} />

          <Form.Item
            label="Confirm new password"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Confirm your new password' },
              ({ getFieldValue }) => ({
                validator: (_, value) =>
                  !value || getFieldValue('newPassword') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('Passwords do not match')),
              }),
            ]}
          >
            <Input.Password size="large" autoComplete="new-password" />
          </Form.Item>

          <Button
            type="primary"
            block
            size="large"
            htmlType="submit"
            loading={passwordSubmitting}
            style={{ height: 52, fontWeight: 600 }}
          >
            Save
          </Button>
        </Form>
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      {contextHolder}
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        {profileLoadError && (
          <div
            style={{
              marginBottom: 16,
              padding: '12px 14px',
              borderRadius: 12,
              background: '#FFF7E6',
              border: '1px solid #FFD591',
            }}
          >
            <Flex vertical gap={10}>
              <Text style={{ color: '#AD6800', fontWeight: 500 }}>
                {sessionExpired
                  ? 'Your session has expired. Please sign in again to continue secure changes.'
                  : `${profileLoadError} Profile updates are disabled until \`user-service\` is running.`}
              </Text>
              {sessionExpired && (
                <Button
                  type="primary"
                  onClick={handleReauthenticate}
                  style={{ alignSelf: 'flex-start' }}
                >
                  Sign in again
                </Button>
              )}
            </Flex>
          </div>
        )}
        {accountLoadError && (
          <div
            style={{
              marginBottom: 16,
              padding: '12px 14px',
              borderRadius: 12,
              background: '#FFF7E6',
              border: '1px solid #FFD591',
            }}
          >
            <Text style={{ color: '#AD6800', fontWeight: 500 }}>
              {`${accountLoadError} Account controls are disabled until \`accounts-service\` is running.`}
            </Text>
          </div>
        )}
        <Flex align="center" gap={16}>
          <Avatar
            size={64}
            src={profilePhoto ?? undefined}
            style={{
              background: TEAL_TINT,
              color: token.colorPrimary,
              border: `2px solid ${token.colorPrimary}`,
              fontWeight: 600,
              fontSize: 22,
              cursor: 'pointer',
            }}
            onClick={() => setPanel('personal')}
          >
            {!profilePhoto ? getInitials(profile.fullName) : null}
          </Avatar>
          <div>
            <Title
              level={3}
              className="font-display"
              style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
            >
              {profile.fullName}
            </Title>
            <Text style={{ display: 'block', marginTop: 4, color: token.colorTextSecondary }}>
              {profile.email}
            </Text>
          </div>
        </Flex>

        <TrustIndicator
          text={
            account?.frozen
              ? 'This account is temporarily frozen for your protection.'
              : profile.idVerified
                ? 'Your identity is verified and protected.'
                : 'Identity verification is pending review.'
          }
        />

        <div
          style={{
            marginTop: 20,
            marginBottom: 24,
            padding: 20,
            borderRadius: 16,
            background: token.colorBgContainer,
            border: `1px solid ${token.colorBorder}`,
          }}
        >
          <Flex justify="space-between" align="center" style={{ marginBottom: 16 }}>
            <div>
              <Text
                style={{
                  display: 'block',
                  fontSize: 12,
                  fontWeight: 700,
                  letterSpacing: 0.4,
                  textTransform: 'uppercase',
                  color: token.colorTextTertiary,
                }}
              >
                Security status
              </Text>
              <Text
                style={{
                  display: 'block',
                  marginTop: 4,
                  color: token.colorTextSecondary,
                  fontSize: 13,
                }}
              >
                Account protection at a glance
              </Text>
            </div>
            <SafetyCertificateOutlined style={{ color: token.colorPrimary, fontSize: 22 }} />
          </Flex>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 10,
            }}
          >
            <SecurityStatusRow
              icon={account?.frozen ? <StopOutlined /> : <CheckCircleOutlined />}
              label="Account status"
              value={account ? (account.frozen ? 'Frozen' : account.status) : 'Active'}
              tone={account?.frozen ? 'danger' : 'success'}
            />
            <SecurityStatusRow
              icon={<SafetyCertificateOutlined />}
              label="Authenticator"
              value="Active"
            />
            <SecurityStatusRow
              icon={profile.idVerified ? <VerifiedOutlined /> : <ClockCircleOutlined />}
              label="Identity"
              value={profile.idVerified ? 'Verified' : 'Pending review'}
              tone={profile.idVerified ? 'success' : 'warning'}
            />
            <SecurityStatusRow
              icon={<CheckCircleOutlined />}
              label="Trusted devices"
              value={`${trustedDeviceCount} device${trustedDeviceCount === 1 ? '' : 's'}`}
            />
          </div>
          <Text
            style={{
              display: 'block',
              marginTop: 14,
              color: token.colorTextTertiary,
              fontSize: 12,
              lineHeight: 1.5,
            }}
          >
            Last verified session: {formatLastVerifiedSession(profile.linkedDevices)}
          </Text>
        </div>

        {sections.map((section) => (
          <div key={section.label} style={{ marginBottom: 20 }}>
            <Text
              style={{
                display: 'block',
                marginBottom: 10,
                fontSize: 12,
                fontWeight: 600,
                letterSpacing: 0.4,
                textTransform: 'uppercase',
                color: token.colorTextTertiary,
              }}
            >
              {section.label}
            </Text>
            <div
              style={{
                background: token.colorBgContainer,
                borderRadius: 16,
                border: `1px solid ${token.colorBorder}`,
                overflow: 'hidden',
              }}
            >
              {section.rows.map((row, index) => (
                <SettingsRow
                  key={row.key}
                  label={row.label}
                  trailing={row.trailing}
                  editable={row.editable}
                  onClick={row.onClick}
                  showDivider={index < section.rows.length - 1}
                />
              ))}
            </div>
          </div>
        ))}

        <Text
          style={{
            display: 'block',
            marginBottom: 10,
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: 0.4,
            textTransform: 'uppercase',
            color: token.colorTextTertiary,
          }}
        >
          Session
        </Text>
        <Flex
          align="center"
          justify="center"
          gap={8}
          onClick={loggingOut ? undefined : handleLogout}
          style={{
            background: token.colorBgContainer,
            borderRadius: 16,
            border: `1px solid ${token.colorError}`,
            padding: '16px 20px',
            cursor: loggingOut ? 'default' : 'pointer',
            opacity: loggingOut ? 0.6 : 1,
          }}
        >
          <LogoutOutlined style={{ color: token.colorError }} />
          <Text style={{ color: token.colorError, fontWeight: 600, fontSize: 15 }}>
            {loggingOut ? 'Logging out...' : 'Log out'}
          </Text>
        </Flex>
      </div>

      <BottomNav accountsPath={account ? `/accounts/${account.id}` : '/accounts'} />

      <Modal
        open={freezeModalOpen}
        onCancel={() => {
          if (!submitting) {
            setFreezeModalOpen(false);
            freezeForm.resetFields();
          }
        }}
        onOk={handleFreezeSubmit}
        okText="Continue"
        confirmLoading={submitting}
        title="Freeze account"
      >
        <Text style={{ display: 'block', marginBottom: 16, color: token.colorTextSecondary }}>
          Add a short reason for the freeze. You will confirm the request with your authenticator
          app.
        </Text>
        <Form form={freezeForm} layout="vertical">
          <Form.Item label="Reason" name="reason">
            <Input.TextArea rows={4} maxLength={180} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Profile;
