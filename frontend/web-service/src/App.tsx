import {
  ArrowLeftOutlined,
  BellOutlined,
  DollarOutlined,
  EditOutlined,
  LockOutlined,
  MenuOutlined,
  PercentageOutlined,
  SafetyOutlined,
  SettingOutlined,
  SwapOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons';
import { useCallback, useEffect, useState } from 'react';
import type { ReactNode } from 'react';
import {
  ApiError,
  confirmChange,
  fetchAllUsers,
  fetchProfile,
  requestChange,
  updateUserRole,
  updateUserStatus,
} from './api/userService';
import type { Identity } from './api/userService';
import type { NotificationPreferences, Role, UserDevice, UserProfile, UserStatus } from './types';

type View =
  'dashboard' | 'settings' | 'notifications' | 'activity' | 'otp' | 'admin' | 'admin-settings';
type Panel = 'none' | 'personal' | 'preferences' | 'language' | 'devices';

type PersonalForm = Pick<
  UserProfile,
  'fullName' | 'email' | 'phoneNumber' | 'addressLine' | 'city' | 'country'
>;

type Notice = {
  tone: 'success' | 'error';
  text: string;
};

type PendingDevice = Pick<UserDevice, 'id' | 'deviceName' | 'deviceType' | 'browser' | 'location'>;
type ActivityItem = {
  letter: string;
  title: string;
  category: string;
  date: string;
  amount: string;
  type: 'debit' | 'credit';
};

type PendingOtp = {
  title: string;
  message: string;
  /** Code echoed back by the service in demo mode, or the offline stand-in. */
  hint: string | null;
  /** Absent when the change could not be staged server side and is previewed locally only. */
  changeRequestId: string | null;
  applyLocal: () => UserProfile;
  afterConfirm?: () => void;
};

/** Stand-in code for the offline preview, when no service is available to issue a real one. */
const OFFLINE_DEMO_CODE = '492000';
const MAX_LINKED_DEVICES = 3;

const ROLES: Role[] = ['CUSTOMER', 'MERCHANT', 'BANK_OFFICER', 'ADMIN'];
const STATUSES: UserStatus[] = ['ACTIVE', 'FROZEN', 'SUSPENDED', 'PENDING_REVIEW'];

const demoProfile: UserProfile = {
  id: '7f921b0c-6904-49c1-a289-d9e06b102f80',
  fullName: 'John Doe',
  email: 'john.doe@securebank.lk',
  phoneNumber: '+94 77 123 4567',
  addressLine: '42 Lake Drive',
  city: 'Colombo',
  country: 'Sri Lanka',
  language: 'English',
  role: 'CUSTOMER',
  status: 'ACTIVE',
  idVerified: true,
  frozen: false,
  freezeReason: null,
  notificationPreferences: {
    email: true,
    sms: true,
    push: true,
  },
  linkedDevices: [
    {
      id: 'device-windows',
      deviceName: 'Chrome on Windows',
      deviceType: 'Desktop',
      browser: 'Chrome',
      location: 'Colombo, LK',
      trusted: true,
      lastVerifiedAt: new Date(Date.now() - 120000).toISOString(),
    },
    {
      id: 'device-ios',
      deviceName: 'SecureBank iOS',
      deviceType: 'Mobile',
      browser: 'Mobile App',
      location: 'Colombo, LK',
      trusted: true,
      lastVerifiedAt: new Date(Date.now() - 259200000).toISOString(),
    },
  ],
};

const demoPendingDevices: PendingDevice[] = [
  {
    id: '8f7d2f4e-6a3c-4a8f-9d21-5b7e3c1f9042',
    deviceName: 'Pixel 9',
    deviceType: 'Mobile',
    browser: 'SecureBank App',
    location: 'Kandy, LK',
  },
];

const activityHistory: ActivityItem[] = [
  {
    letter: 'C',
    title: 'Ceylon Electricity Board',
    category: 'Utilities',
    date: 'Today',
    amount: '-LKR 84.20',
    type: 'debit',
  },
  {
    letter: 'S',
    title: 'Salary Deposit',
    category: 'Income',
    date: 'Yesterday',
    amount: '+LKR 3,200.00',
    type: 'credit',
  },
  {
    letter: 'K',
    title: "Kumar's Grocers",
    category: 'Groceries',
    date: 'Jul 20',
    amount: '-LKR 46.75',
    type: 'debit',
  },
  {
    letter: 'T',
    title: 'Transfer to A. Silva',
    category: 'Transfer',
    date: 'Jul 19',
    amount: '-LKR 150.00',
    type: 'debit',
  },
  {
    letter: 'H',
    title: 'Highway Fuel Stop',
    category: 'Transport',
    date: 'Jul 18',
    amount: '-LKR 18.40',
    type: 'debit',
  },
  {
    letter: 'M',
    title: 'Mobile Reload',
    category: 'Bills',
    date: 'Jul 17',
    amount: '-LKR 12.00',
    type: 'debit',
  },
  {
    letter: 'R',
    title: 'Rent Collection',
    category: 'Income',
    date: 'Jul 15',
    amount: '+LKR 950.00',
    type: 'credit',
  },
  {
    letter: 'P',
    title: 'Pharmacy Care',
    category: 'Health',
    date: 'Jul 14',
    amount: '-LKR 36.90',
    type: 'debit',
  },
  {
    letter: 'D',
    title: 'Dialog Broadband',
    category: 'Internet',
    date: 'Jul 13',
    amount: '-LKR 42.50',
    type: 'debit',
  },
  {
    letter: 'B',
    title: 'Bookshop',
    category: 'Education',
    date: 'Jul 12',
    amount: '-LKR 22.10',
    type: 'debit',
  },
  {
    letter: 'F',
    title: 'Freelance Project',
    category: 'Income',
    date: 'Jul 11',
    amount: '+LKR 420.00',
    type: 'credit',
  },
  {
    letter: 'G',
    title: 'Green Market',
    category: 'Groceries',
    date: 'Jul 10',
    amount: '-LKR 31.25',
    type: 'debit',
  },
];

const demoProfileImage = null as string | null;

const moneyFormatter = new Intl.NumberFormat('en-LK', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

function App() {
  const [view, setView] = useState<View>('dashboard');
  const [panel, setPanel] = useState<Panel>('none');
  const [profile, setProfile] = useState<UserProfile>(demoProfile);
  const [offline, setOffline] = useState(false);
  const [notice, setNotice] = useState<Notice | null>(null);
  const [pendingOtp, setPendingOtp] = useState<PendingOtp | null>(null);
  const [otpCode, setOtpCode] = useState('');
  const [otpError, setOtpError] = useState<string | null>(null);
  const [activityPage, setActivityPage] = useState(1);
  const [personalForm, setPersonalForm] = useState<PersonalForm>(toPersonalForm(demoProfile));
  const [languageForm, setLanguageForm] = useState(demoProfile.language);
  const [profileImage, setProfileImage] = useState<string | null>(demoProfileImage);
  const [pendingDevices, setPendingDevices] = useState<PendingDevice[]>(demoPendingDevices);
  const [preferencesForm, setPreferencesForm] = useState<NotificationPreferences>(
    demoProfile.notificationPreferences
  );

  /** Keeps the edit forms in step with whatever the service last confirmed. */
  const adoptProfile = useCallback((next: UserProfile) => {
    setProfile(next);
    setPersonalForm(toPersonalForm(next));
    setLanguageForm(next.language);
    setPreferencesForm(next.notificationPreferences);
  }, []);

  useEffect(() => {
    let cancelled = false;
    fetchProfile()
      .then((data) => {
        if (!cancelled) {
          adoptProfile(data);
          setOffline(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setOffline(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [adoptProfile]);

  useEffect(() => {
    setView((current) => {
      if (current === 'otp') {
        return current;
      }
      if (isAdminRole(profile.role)) {
        return current === 'admin-settings' ? current : 'admin';
      }
      return current === 'settings' || current === 'notifications' || current === 'activity'
        ? current
        : 'dashboard';
    });
  }, [profile.role]);

  const trustedDeviceCount = profile.linkedDevices.filter((device) => device.trusted).length;
  const initials = toInitials(profile.fullName);

  const openOtp = async (
    title: string,
    endpoint: string,
    payload: unknown,
    applyLocal: () => UserProfile,
    afterConfirm?: () => void
  ) => {
    setNotice(null);
    try {
      const challenge = await requestChange(endpoint, payload);
      setPendingOtp({
        title,
        message: challenge.message,
        hint: challenge.demoCode,
        changeRequestId: challenge.changeRequestId,
        applyLocal,
        afterConfirm,
      });
    } catch (error) {
      if (error instanceof ApiError) {
        // The service refused the change outright, so there is nothing to confirm.
        setNotice({ tone: 'error', text: error.message });
        return;
      }
      setOffline(true);
      setPendingOtp({
        title,
        message: 'SecureBank services are unreachable. This confirmation is a local preview only.',
        hint: OFFLINE_DEMO_CODE,
        changeRequestId: null,
        applyLocal,
        afterConfirm,
      });
    }
    setOtpCode('');
    setOtpError(null);
    setView('otp');
  };

  const confirmOtp = async () => {
    if (!pendingOtp) {
      return;
    }
    if (otpCode.length !== 6) {
      setOtpError('Enter all six digits to continue.');
      return;
    }

    if (pendingOtp.changeRequestId) {
      try {
        adoptProfile(await confirmChange(pendingOtp.changeRequestId, otpCode));
        pendingOtp.afterConfirm?.();
        closeOtp({ tone: 'success', text: 'Change confirmed.' });
        return;
      } catch (error) {
        if (error instanceof ApiError) {
          // Surface the service's own wording, which carries the remaining attempt count.
          setOtpError(error.message);
          return;
        }
        setOffline(true);
      }
    }

    if (otpCode === (pendingOtp.hint ?? OFFLINE_DEMO_CODE)) {
      adoptProfile(pendingOtp.applyLocal());
      pendingOtp.afterConfirm?.();
      closeOtp({ tone: 'success', text: 'Change applied in offline preview, not saved.' });
      return;
    }
    setOtpError('That code is not correct.');
  };

  const closeOtp = (result: Notice | null) => {
    setPendingOtp(null);
    setOtpCode('');
    setOtpError(null);
    setNotice(result);
    setView(settingsViewForRole(profile.role));
  };

  const savePersonalDetails = () => {
    setPanel('none');
    void openOtp('Confirm personal details', '/me/profile-change', personalForm, () => ({
      ...profile,
      ...personalForm,
    }));
  };

  const savePreferences = () => {
    setPanel('none');
    void openOtp(
      'Confirm notification preferences',
      '/me/notification-preferences-change',
      preferencesForm,
      () => ({ ...profile, notificationPreferences: preferencesForm })
    );
  };

  const saveLanguage = () => {
    setPanel('none');
    void openOtp(
      'Confirm language preference',
      '/me/profile-change',
      { language: languageForm },
      () => ({
        ...profile,
        language: languageForm,
      })
    );
  };

  const revokeDevice = (deviceId: string) => {
    void openOtp('Confirm device removal', '/me/devices/revoke', { deviceId }, () => ({
      ...profile,
      linkedDevices: profile.linkedDevices.filter((device) => device.id !== deviceId),
    }));
  };

  const verifyPendingDevice = (deviceId: string) => {
    const pendingDevice = pendingDevices.find((device) => device.id === deviceId);
    if (!pendingDevice) {
      return;
    }
    if (profile.linkedDevices.length >= MAX_LINKED_DEVICES) {
      setNotice({
        tone: 'error',
        text: `Only ${MAX_LINKED_DEVICES} linked devices are allowed. Remove one before verifying a new device.`,
      });
      return;
    }

    void openOtp(
      'Verify new device',
      '/me/devices/link',
      {
        deviceName: pendingDevice.deviceName,
        deviceType: pendingDevice.deviceType,
        browser: pendingDevice.browser,
        location: pendingDevice.location,
      },
      () => ({
        ...profile,
        linkedDevices: [
          {
            ...pendingDevice,
            trusted: true,
            lastVerifiedAt: new Date().toISOString(),
          },
          ...profile.linkedDevices,
        ],
      }),
      () => setPendingDevices((current) => current.filter((device) => device.id !== deviceId))
    );
  };

  const freezeAccount = () => {
    const reason = 'Customer security request';
    void openOtp('Confirm account freeze', '/me/freeze', { reason }, () => ({
      ...profile,
      frozen: true,
      status: 'FROZEN',
      freezeReason: reason,
    }));
  };

  const unfreezeAccount = () => {
    void openOtp('Confirm account unfreeze', '/me/unfreeze', {}, () => ({
      ...profile,
      frozen: false,
      status: 'ACTIVE',
      freezeReason: null,
    }));
  };

  return (
    <main className="app-shell">
      <section className="phone-frame">
        {view === 'dashboard' && (
          <Dashboard
            profileImage={profileImage}
            initials={initials}
            offline={offline}
            profile={profile}
            openPersonalDetails={() => {
              setPanel('personal');
              setView('settings');
            }}
            setActivityPage={setActivityPage}
            setView={setView}
          />
        )}
        {view === 'settings' && !isAdminRole(profile.role) && (
          <CustomerSettings
            profileImage={profileImage}
            initials={initials}
            notice={notice}
            offline={offline}
            panel={panel}
            profile={profile}
            personalForm={personalForm}
            preferencesForm={preferencesForm}
            languageForm={languageForm}
            setPanel={setPanel}
            setProfileImage={setProfileImage}
            setPersonalForm={setPersonalForm}
            setLanguageForm={setLanguageForm}
            setPreferencesForm={setPreferencesForm}
            setView={setView}
            savePersonalDetails={savePersonalDetails}
            saveLanguage={saveLanguage}
            savePreferences={savePreferences}
            pendingDevices={pendingDevices}
            revokeDevice={revokeDevice}
            verifyPendingDevice={verifyPendingDevice}
            freezeAccount={freezeAccount}
            logout={() =>
              setNotice({
                tone: 'success',
                text: 'You have been signed out of SecureBank.',
              })
            }
            unfreezeAccount={unfreezeAccount}
            requestDataExport={() =>
              setNotice({
                tone: 'success',
                text: 'Data export requested. You will be emailed when it is ready.',
              })
            }
            trustedDeviceCount={trustedDeviceCount}
          />
        )}
        {view === 'notifications' && !isAdminRole(profile.role) && (
          <Notifications setView={setView} />
        )}
        {view === 'activity' && !isAdminRole(profile.role) && (
          <ActivityHistory page={activityPage} setPage={setActivityPage} setView={setView} />
        )}
        {view === 'admin' && isAdminRole(profile.role) && (
          <AdminRbac
            currentProfile={profile}
            currentProfileId={profile.id}
            onProfileUpdated={adoptProfile}
            setView={setView}
          />
        )}
        {view === 'admin-settings' && isAdminRole(profile.role) && (
          <AdminSettings notice={notice} offline={offline} profile={profile} setView={setView} />
        )}
        {view === 'otp' && (
          <OtpVerification
            otpCode={otpCode}
            otpError={otpError}
            pendingOtp={pendingOtp}
            setOtpCode={setOtpCode}
            onCancel={() => closeOtp(null)}
            confirmOtp={confirmOtp}
          />
        )}
      </section>
    </main>
  );
}

function Dashboard({
  profileImage,
  initials,
  offline,
  openPersonalDetails,
  profile,
  setActivityPage,
  setView,
}: {
  profileImage: string | null;
  initials: string;
  offline: boolean;
  openPersonalDetails: () => void;
  profile: UserProfile;
  setActivityPage: (page: number) => void;
  setView: (view: View) => void;
}) {
  return (
    <div className="screen dashboard-screen">
      <header className="dashboard-header">
        <div className="dashboard-identity">
          <button
            className="avatar-button"
            onClick={openPersonalDetails}
            aria-label="Open personal details"
          >
            <AvatarBadge
              className="avatar-button static-avatar"
              image={profileImage}
              initials={initials}
            />
          </button>
          <div>
            <p className="eyebrow greeting-label">Hi 👋</p>
            <h1>{profile.fullName}</h1>
          </div>
        </div>
        <div className="header-actions">
          <button
            className="plain-icon round-icon"
            onClick={() => setView('notifications')}
            aria-label="Open notifications"
          >
            <BellOutlined />
          </button>
          <button
            className="plain-icon round-icon"
            onClick={() => setView('settings')}
            aria-label="Open settings"
          >
            <SettingOutlined />
          </button>
        </div>
      </header>

      {offline && <OfflineBanner />}
      {profile.frozen && (
        <p className="frozen-banner" role="status">
          <LockOutlined /> Account frozen{profile.freezeReason ? ` · ${profile.freezeReason}` : ''}
        </p>
      )}

      <section className="balance-card">
        <div className="balance-topline">
          <span className="bank-name">SecureBank</span>
          <span className="verified-pill">Verified</span>
        </div>
        <strong>LKR {moneyFormatter.format(48231.76)}</strong>
        <div className="balance-foot">
          <span className="positive">▲ 2.4% this month</span>
          <span>12****67</span>
        </div>
      </section>

      <nav className="quick-actions" aria-label="Banking actions">
        <ActionButton icon={<SwapOutlined />} label="Transfer" />
        <ActionButton icon={<DollarOutlined />} label="Pay" />
        <ActionButton icon={<PercentageOutlined />} label="Loans" />
        <ActionButton icon={<MenuOutlined />} label="History" />
      </nav>

      <section className="activity-heading">
        <h2>Recent activity</h2>
        <button
          onClick={() => {
            setActivityPage(1);
            setView('activity');
          }}
        >
          See all
        </button>
      </section>

      <section className="activity-list">
        {activityHistory.slice(0, 4).map((item) => (
          <ActivityRow item={item} key={`${item.title}-${item.date}`} />
        ))}
      </section>
    </div>
  );
}

function ActionButton({ icon, label }: { icon: ReactNode; label: string }) {
  return (
    <button className="action-button">
      <span>{icon}</span>
      <b>{label}</b>
    </button>
  );
}

function AvatarBadge({
  className,
  image,
  initials,
}: {
  className: string;
  image: string | null;
  initials: string;
}) {
  return image ? (
    <span className={`${className} has-photo`} aria-hidden="true">
      <img alt="" src={image} />
    </span>
  ) : (
    <span className={className} aria-hidden="true">
      {initials}
    </span>
  );
}

function ActivityRow({ item }: { item: ActivityItem }) {
  return (
    <article className="activity-item">
      <span className="activity-avatar">{item.letter}</span>
      <span>
        <strong>{item.title}</strong>
        <small>
          {item.category} · {item.date}
        </small>
      </span>
      <b className={item.type === 'credit' ? 'credit' : ''}>{item.amount}</b>
    </article>
  );
}

function ActivityHistory({
  page,
  setPage,
  setView,
}: {
  page: number;
  setPage: (page: number) => void;
  setView: (view: View) => void;
}) {
  const pageSize = 10;
  const totalPages = Math.max(1, Math.ceil(activityHistory.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const start = (currentPage - 1) * pageSize;
  const visible = activityHistory.slice(start, start + pageSize);

  return (
    <div className="screen notification-screen">
      <button
        className="plain-icon top-back"
        onClick={() => setView('dashboard')}
        aria-label="Back to dashboard"
      >
        <ArrowLeftOutlined />
      </button>
      <h1>Previous activities</h1>
      <section className="activity-list">
        {visible.map((item) => (
          <ActivityRow item={item} key={`${item.title}-${item.date}`} />
        ))}
      </section>
      <div className="pager">
        <button disabled={currentPage === 1} onClick={() => setPage(currentPage - 1)}>
          Previous
        </button>
        <span>
          Page {currentPage} of {totalPages}
        </span>
        <button disabled={currentPage === totalPages} onClick={() => setPage(currentPage + 1)}>
          Next
        </button>
      </div>
    </div>
  );
}

function CustomerSettings({
  profileImage,
  initials,
  notice,
  offline,
  panel,
  profile,
  personalForm,
  preferencesForm,
  languageForm,
  setPanel,
  setProfileImage,
  setPersonalForm,
  setLanguageForm,
  setPreferencesForm,
  setView,
  savePersonalDetails,
  saveLanguage,
  savePreferences,
  pendingDevices,
  revokeDevice,
  verifyPendingDevice,
  freezeAccount,
  logout,
  unfreezeAccount,
  requestDataExport,
  trustedDeviceCount,
}: {
  profileImage: string | null;
  initials: string;
  notice: Notice | null;
  offline: boolean;
  panel: Panel;
  profile: UserProfile;
  personalForm: PersonalForm;
  preferencesForm: NotificationPreferences;
  languageForm: string;
  setPanel: (panel: Panel) => void;
  setProfileImage: (image: string | null) => void;
  setPersonalForm: (form: PersonalForm) => void;
  setLanguageForm: (language: string) => void;
  setPreferencesForm: (form: NotificationPreferences) => void;
  setView: (view: View) => void;
  savePersonalDetails: () => void;
  saveLanguage: () => void;
  savePreferences: () => void;
  pendingDevices: PendingDevice[];
  revokeDevice: (deviceId: string) => void;
  verifyPendingDevice: (deviceId: string) => void;
  freezeAccount: () => void;
  logout: () => void;
  unfreezeAccount: () => void;
  requestDataExport: () => void;
  trustedDeviceCount: number;
}) {
  if (panel === 'personal') {
    return (
      <SettingsDetailScreen title="Personal details" onBack={() => setPanel('none')}>
        <section className="photo-upload">
          <h2>Profile picture</h2>
          <label className="photo-upload-card">
            <AvatarBadge
              className="profile-avatar upload-avatar"
              image={profileImage}
              initials={initials}
            />
            <span className="photo-upload-edit" aria-hidden="true">
              <EditOutlined />
            </span>
            <input
              accept="image/*"
              capture="environment"
              type="file"
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (!file) {
                  return;
                }
                const reader = new FileReader();
                reader.onload = () => {
                  if (typeof reader.result === 'string') {
                    setProfileImage(reader.result);
                  }
                };
                reader.readAsDataURL(file);
              }}
            />
          </label>
        </section>
        {(['fullName', 'email', 'phoneNumber', 'addressLine', 'city', 'country'] as const).map(
          (field) => (
            <label className="field" key={field}>
              {fieldLabel(field)}
              <input
                value={personalForm[field]}
                onChange={(event) =>
                  setPersonalForm({
                    ...personalForm,
                    [field]: event.target.value,
                  })
                }
              />
            </label>
          )
        )}
        <button className="primary-button" onClick={savePersonalDetails}>
          Save
        </button>
      </SettingsDetailScreen>
    );
  }

  if (panel === 'preferences') {
    return (
      <SettingsDetailScreen title="Notification methods" onBack={() => setPanel('none')}>
        {(['email', 'sms', 'push'] as const).map((channel) => (
          <label className="toggle-row" key={channel}>
            <span>{channel.toUpperCase()}</span>
            <input
              checked={preferencesForm[channel]}
              type="checkbox"
              onChange={(event) =>
                setPreferencesForm({
                  ...preferencesForm,
                  [channel]: event.target.checked,
                })
              }
            />
          </label>
        ))}
        <button className="primary-button" onClick={savePreferences}>
          Save
        </button>
      </SettingsDetailScreen>
    );
  }

  if (panel === 'language') {
    return (
      <SettingsDetailScreen title="Language" onBack={() => setPanel('none')}>
        <label className="field">
          Preferred language
          <input value={languageForm} onChange={(event) => setLanguageForm(event.target.value)} />
        </label>
        <button className="primary-button" onClick={saveLanguage}>
          Save
        </button>
      </SettingsDetailScreen>
    );
  }

  if (panel === 'devices') {
    return (
      <SettingsDetailScreen title="Linked devices" onBack={() => setPanel('none')}>
        <DevicePanel
          devices={profile.linkedDevices}
          pendingDevices={pendingDevices}
          onClose={() => setPanel('none')}
          revokeDevice={revokeDevice}
          verifyPendingDevice={verifyPendingDevice}
        />
      </SettingsDetailScreen>
    );
  }

  return (
    <div className="screen profile-screen">
      <header className="profile-identity">
        <button
          className="plain-icon"
          onClick={() => setView('dashboard')}
          aria-label="Back to dashboard"
        >
          <ArrowLeftOutlined />
        </button>
        <div>
          <h1>Settings</h1>
          <p>{profile.idVerified ? 'ID verified' : 'ID pending'}</p>
        </div>
      </header>

      {offline && <OfflineBanner />}
      {notice && (
        <p className={`notice ${notice.tone}`} role="status">
          {notice.text}
        </p>
      )}

      <section className="admin-card settings-hero">
        <AvatarBadge className="profile-avatar small" image={profileImage} initials={initials} />
        <div>
          <h2>{profile.fullName}</h2>
          <p>{profile.email}</p>
        </div>
      </section>

      <section className="security-card">
        <h2>Security status</h2>
        <StatusRow label="Account status" value={toTitle(profile.status)} />
        <StatusRow label="Two-factor authentication" value="Active" />
        <StatusRow label="Trusted devices" value={`${trustedDeviceCount} devices`} />
      </section>

      <section className="settings-list">
        <SettingsRow label="Personal details" value="Update" onClick={() => setPanel('personal')} />
        <SettingsRow
          label="Notification methods"
          value={preferenceLabel(profile.notificationPreferences)}
          onClick={() => setPanel('preferences')}
        />
        <SettingsRow
          label="Language"
          value={profile.language}
          onClick={() => setPanel('language')}
        />
        <SettingsRow
          label="Linked devices"
          value={`${profile.linkedDevices.length} linked`}
          onClick={() => setPanel('devices')}
        />
        <SettingsRow label="Download my data" value="Request" onClick={requestDataExport} />
      </section>

      {profile.frozen ? (
        <button className="primary-button block" onClick={unfreezeAccount}>
          Unfreeze account
        </button>
      ) : (
        <button className="danger-button" onClick={freezeAccount}>
          Freeze account instantly
        </button>
      )}
      <button className="secondary-button" onClick={logout}>
        Log out
      </button>
    </div>
  );
}

function DevicePanel({
  devices,
  pendingDevices,
  onClose,
  revokeDevice,
  verifyPendingDevice,
}: {
  devices: UserDevice[];
  pendingDevices: PendingDevice[];
  onClose: () => void;
  revokeDevice: (deviceId: string) => void;
  verifyPendingDevice: (deviceId: string) => void;
}) {
  const atLimit = devices.length >= MAX_LINKED_DEVICES;

  return (
    <section className="detail-panel">
      <PanelHeader title="Linked devices" onClose={onClose} />
      {pendingDevices.length > 0 && (
        <>
          <p className="section-label device-section-label">PENDING VERIFICATION</p>
          {pendingDevices.map((device) => (
            <article className="device-row pending" key={device.id}>
              <span className="device-icon">
                <SafetyOutlined />
              </span>
              <span>
                <strong>{device.deviceName}</strong>
                <small>
                  {device.browser} · {device.location} · Waiting for primary-device approval
                </small>
              </span>
              <span className="row-actions">
                <button disabled={atLimit} onClick={() => verifyPendingDevice(device.id)}>
                  Verify
                </button>
              </span>
            </article>
          ))}
        </>
      )}

      <p className="section-label device-section-label">LINKED DEVICES</p>
      {devices.length === 0 && <p className="empty-hint">No devices are linked yet.</p>}
      {devices.map((device) => (
        <article className="device-row" key={device.id}>
          <span className="device-icon">
            <SafetyOutlined />
          </span>
          <span>
            <strong>{device.deviceName}</strong>
            <small>
              {device.browser} · {device.location} · {device.trusted ? 'Trusted' : 'Not trusted'}
            </small>
          </span>
          <span className="row-actions">
            <button onClick={() => revokeDevice(device.id)}>Revoke</button>
          </span>
        </article>
      ))}

      {atLimit && <p className="notice error device-limit">Maximum linked devices reached.</p>}
    </section>
  );
}

function StatusRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="status-row">
      <span>{label}</span>
      <b>{value}</b>
    </div>
  );
}

function SettingsRow({
  label,
  value,
  onClick,
}: {
  label: string;
  value: string;
  onClick: () => void;
}) {
  return (
    <button className="settings-row" onClick={onClick}>
      <strong>{label}</strong>
      <span className="settings-row-value">
        {value}
        <EditOutlined />
      </span>
    </button>
  );
}

function SettingsDetailScreen({
  title,
  onBack,
  children,
}: {
  title: string;
  onBack: () => void;
  children: ReactNode;
}) {
  return (
    <div className="screen profile-screen">
      <header className="profile-identity">
        <button className="plain-icon" onClick={onBack} aria-label="Back to settings">
          <ArrowLeftOutlined />
        </button>
        <div>
          <h1>{title}</h1>
        </div>
      </header>
      <section className="detail-panel standalone">{children}</section>
    </div>
  );
}

function PanelHeader({ title, onClose }: { title: string; onClose: () => void }) {
  return (
    <header className="panel-header">
      <h2>{title}</h2>
      <button onClick={onClose}>Close</button>
    </header>
  );
}

function OfflineBanner() {
  return (
    <p className="notice offline" role="status">
      Offline preview — the user service is unreachable, so changes are not saved.
    </p>
  );
}

function Notifications({ setView }: { setView: (view: View) => void }) {
  return (
    <div className="screen notification-screen">
      <button
        className="plain-icon top-back"
        onClick={() => setView('dashboard')}
        aria-label="Back to dashboard"
      >
        <ArrowLeftOutlined />
      </button>
      <h1>Notifications</h1>
      <p className="section-label">TODAY</p>
      <NotificationCard
        important
        title="New Device Sign-In Confirmed"
        body="A sign-in from Chrome on Windows in Colombo, LK has been confirmed. If you do not recognise this activity, please secure your account immediately."
        meta="Security Alert · 14:22"
      />
      <NotificationCard
        important
        title="Card Transaction Declined"
        body="A card transaction of LKR 92,000.00 from an unrecognised location was declined for your protection."
        meta="Security Alert · 09:47"
      />
      <p className="section-label">EARLIER</p>
      <NotificationCard
        title="Loan Instalment Processed"
        body="Your July loan instalment of LKR 24,350.00 has been processed successfully."
        meta="05 Jul · 08:00"
      />
      <NotificationCard
        title="Account Statement Available"
        body="Your June statement for your Everyday Current account is now available for download."
        meta="01 Jul · 06:12"
      />
    </div>
  );
}

function NotificationCard({
  title,
  body,
  meta,
  important = false,
}: {
  title: string;
  body: string;
  meta: string;
  important?: boolean;
}) {
  return (
    <article className={important ? 'notification-card important' : 'notification-card'}>
      <h2>
        {important && <span />}
        {title}
      </h2>
      <p>{body}</p>
      <small>{meta}</small>
    </article>
  );
}

/**
 * Operator console for role and status administration (FR-08).
 *
 * <p>Acts as the seeded administrator: the user list is fetched with an administrator role header,
 * and the administrator's own id is then sent as the acting identity for every mutation so the
 * service can apply its self-escalation guard.
 */
function AdminRbac({
  currentProfile,
  currentProfileId,
  onProfileUpdated,
  setView,
}: {
  currentProfile: UserProfile;
  currentProfileId: string;
  onProfileUpdated: (profile: UserProfile) => void;
  setView: (view: View) => void;
}) {
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    fetchAllUsers({ role: 'ADMIN' })
      .then((list) => {
        if (cancelled) {
          return;
        }
        setUsers(list);
        setSelectedId(list.find((user) => user.role !== 'ADMIN')?.id ?? list[0]?.id ?? null);
      })
      .catch((cause: unknown) => {
        if (!cancelled) {
          setError(cause instanceof Error ? cause.message : 'Unable to load users');
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const operator = users.find((user) => user.role === 'ADMIN');
  const identity: Identity = { userId: operator?.id, role: 'ADMIN' };
  const selected = users.find((user) => user.id === selectedId) ?? null;

  const apply = async (action: () => Promise<UserProfile>) => {
    setBusy(true);
    setError(null);
    try {
      const updated = await action();
      setUsers((current) => current.map((user) => (user.id === updated.id ? updated : user)));
      if (updated.id === currentProfileId) {
        onProfileUpdated(updated);
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : 'Update failed');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="screen admin-screen">
      <header className="dashboard-header admin-header">
        <div>
          <p className="eyebrow">Admin workspace</p>
          <h1>User access</h1>
        </div>
        <div className="header-actions">
          <span className="avatar-button static-avatar" aria-hidden="true">
            {toInitials(currentProfile.fullName)}
          </span>
          <button
            className="plain-icon round-icon"
            onClick={() => setView('admin-settings')}
            aria-label="Open admin settings"
          >
            <SettingOutlined />
          </button>
        </div>
      </header>

      <section className="admin-card settings-hero">
        <span className="profile-avatar small">
          <TeamOutlined />
        </span>
        <div>
          <h2>{currentProfile.fullName}</h2>
          <p>{toTitle(currentProfile.role)} account assigned by system administration</p>
        </div>
      </section>

      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
      {loading && <p className="empty-hint">Loading users…</p>}
      {!loading && users.length === 0 && !error && (
        <p className="empty-hint">No users are available to administer.</p>
      )}

      {users.length > 0 && (
        <section className="detail-panel inline">
          <h2>Directory</h2>
          <div className="user-list">
            {users.map((user) => (
              <button
                className={user.id === selectedId ? 'user-row active' : 'user-row'}
                key={user.id}
                onClick={() => setSelectedId(user.id)}
              >
                <span className="profile-avatar small">
                  <UserOutlined />
                </span>
                <span>
                  <strong>{user.fullName}</strong>
                  <small>
                    {toTitle(user.role)} · {toTitle(user.status)}
                  </small>
                </span>
              </button>
            ))}
          </div>
        </section>
      )}

      {selected && (
        <>
          <section className="admin-card">
            <span className="profile-avatar small">
              <UserOutlined />
            </span>
            <div>
              <h2>{selected.fullName}</h2>
              <p>{selected.email}</p>
            </div>
          </section>

          <section className="detail-panel inline">
            <h2>Role</h2>
            <div className="choice-grid">
              {ROLES.map((role) => (
                <button
                  className={selected.role === role ? 'choice active' : 'choice'}
                  disabled={busy}
                  key={role}
                  onClick={() => void apply(() => updateUserRole(identity, selected.id, role))}
                >
                  {toTitle(role)}
                </button>
              ))}
            </div>
          </section>

          <section className="detail-panel inline">
            <h2>Status</h2>
            <div className="choice-grid">
              {STATUSES.map((status) => (
                <button
                  className={selected.status === status ? 'choice active' : 'choice'}
                  disabled={busy}
                  key={status}
                  onClick={() => void apply(() => updateUserStatus(identity, selected.id, status))}
                >
                  {toTitle(status)}
                </button>
              ))}
            </div>
          </section>
        </>
      )}
    </div>
  );
}

function AdminSettings({
  notice,
  offline,
  profile,
  setView,
}: {
  notice: Notice | null;
  offline: boolean;
  profile: UserProfile;
  setView: (view: View) => void;
}) {
  return (
    <div className="screen profile-screen">
      <header className="profile-identity">
        <button
          className="plain-icon"
          onClick={() => setView('admin')}
          aria-label="Back to admin dashboard"
        >
          <ArrowLeftOutlined />
        </button>
        <div>
          <h1>Admin settings</h1>
          <p>{toTitle(profile.role)} access</p>
        </div>
      </header>

      {offline && <OfflineBanner />}
      {notice && (
        <p className={`notice ${notice.tone}`} role="status">
          {notice.text}
        </p>
      )}

      <section className="admin-card settings-hero">
        <span className="profile-avatar small">
          <TeamOutlined />
        </span>
        <div>
          <h2>{profile.fullName}</h2>
          <p>{profile.email}</p>
        </div>
      </section>

      <section className="security-card">
        <h2>Admin account</h2>
        <StatusRow label="Assigned role" value={toTitle(profile.role)} />
        <StatusRow label="Access status" value={toTitle(profile.status)} />
        <StatusRow label="Provisioning" value="Assigned by super admin" />
      </section>

      <section className="settings-list">
        <SettingsRow
          label="Role visibility"
          value={toTitle(profile.role)}
          onClick={() => undefined}
        />
        <SettingsRow label="Support channel" value="Security desk" onClick={() => undefined} />
        <SettingsRow
          label="Notifications"
          value="Email and system alerts"
          onClick={() => undefined}
        />
      </section>
    </div>
  );
}

function OtpVerification({
  otpCode,
  otpError,
  pendingOtp,
  setOtpCode,
  onCancel,
  confirmOtp,
}: {
  otpCode: string;
  otpError: string | null;
  pendingOtp: PendingOtp | null;
  setOtpCode: (code: string) => void;
  onCancel: () => void;
  confirmOtp: () => void;
}) {
  const digits = Array.from({ length: 6 }, (_, index) => otpCode[index] ?? '');

  return (
    <div className="screen otp-screen">
      <button className="plain-icon top-back" onClick={onCancel} aria-label="Cancel confirmation">
        <ArrowLeftOutlined />
      </button>
      <p className="brand">SecureBank</p>
      <h1>{pendingOtp?.title ?? "Verify it's you"}</h1>
      <label className="otp-entry">
        <span className="sr-only">One time password</span>
        <input
          autoFocus
          inputMode="numeric"
          maxLength={6}
          value={otpCode}
          onChange={(event) => setOtpCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
        />
        <div className="otp-boxes">
          {digits.map((digit, index) => (
            <span className={digit ? 'filled' : ''} key={`${index}-${digit}`}>
              {digit}
            </span>
          ))}
        </div>
      </label>
      <p className="otp-help">{pendingOtp?.message ?? 'Enter the six digit code to continue.'}</p>
      {pendingOtp?.hint && <p className="otp-help demo-code">Demo code: {pendingOtp.hint}</p>}
      {otpError && (
        <p className="notice error" role="alert">
          {otpError}
        </p>
      )}
      <button className="primary-button" onClick={confirmOtp}>
        Verify and continue
      </button>
      <p className="secure-note">
        <LockOutlined /> This device will be remembered for 30 days
      </p>
    </div>
  );
}

function toPersonalForm(profile: UserProfile): PersonalForm {
  return {
    fullName: profile.fullName,
    email: profile.email,
    phoneNumber: profile.phoneNumber,
    addressLine: profile.addressLine,
    city: profile.city,
    country: profile.country,
  };
}

function toInitials(fullName: string) {
  return fullName
    .split(' ')
    .filter(Boolean)
    .map((name) => name[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

function preferenceLabel(preferences: NotificationPreferences) {
  const enabled = [
    preferences.email ? 'Email' : '',
    preferences.sms ? 'SMS' : '',
    preferences.push ? 'Push' : '',
  ].filter(Boolean);
  return enabled.length > 0 ? enabled.join(' · ') : 'All off';
}

function fieldLabel(field: keyof PersonalForm) {
  const labels: Record<keyof PersonalForm, string> = {
    fullName: 'Full name',
    email: 'Email',
    phoneNumber: 'Phone number',
    addressLine: 'Address',
    city: 'City',
    country: 'Country',
  };
  return labels[field];
}

function toTitle(value: string) {
  return value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');
}

function isAdminRole(role: Role) {
  return role === 'ADMIN' || role === 'BANK_OFFICER';
}

function settingsViewForRole(role: Role): View {
  return isAdminRole(role) ? 'admin-settings' : 'settings';
}

export default App;
