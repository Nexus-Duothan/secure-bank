import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, Flex, Typography, theme } from 'antd';
import { LogoutOutlined, RightOutlined } from '@ant-design/icons';
import authService from '../../api/authService';
import userService, { type UserProfile } from '../../api/userService';
import accountsService, { type Account } from '../../api/accountsService';
import tokenStorage from '../../api/tokenStorage';
import TrustIndicator from '../../components/TrustIndicator';
import BottomNav from '../../components/BottomNav';

const { Text, Title } = Typography;

const NAVY = '#0B1B2B';
const TEAL_TINT = '#DCEFEA';

const MOCK_PROFILE: UserProfile = {
  id: 'usr-001',
  fullName: 'John Doe',
  email: 'john.doe@example.com',
  role: 'Customer',
  idVerified: true,
  twoFactorEnabled: true,
  lastVerifiedSession: 'Today 14:22',
  trustedDevicesCount: 2,
  deviceVerified: true,
};

const MOCK_ACCOUNT: Account = {
  id: 'acc-001',
  nickname: 'Current Account',
  accountType: 'CURRENT',
  lastFourDigits: '4821',
  balance: 48231.76,
  currency: 'USD',
  monthlyChangePercent: 2.4,
  verifiedLabel: '2m ago',
};

const getInitials = (name: string) =>
  name
    .split(' ')
    .map((part) => part.charAt(0))
    .join('')
    .slice(0, 2)
    .toUpperCase();

interface SettingsRowItem {
  key: string;
  label: string;
  trailing?: string;
  onClick?: () => void;
}

interface SettingsRowProps extends SettingsRowItem {
  showDivider: boolean;
}

const SettingsRow: React.FC<SettingsRowProps> = ({ label, trailing, onClick, showDivider }) => {
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
        <RightOutlined style={{ fontSize: 12, color: token.colorTextTertiary }} />
      </Flex>
    </Flex>
  );
};

interface SecurityStatusRowProps {
  label: string;
  value: string;
}

const SecurityStatusRow: React.FC<SecurityStatusRowProps> = ({ label, value }) => (
  <Flex justify="space-between" align="center">
    <Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 14 }}>{label}</Text>
    <Flex
      align="center"
      gap={6}
      style={{ background: 'rgba(31,122,108,0.35)', padding: '4px 12px', borderRadius: 999 }}
    >
      <span style={{ width: 6, height: 6, borderRadius: '50%', background: '#3FD6B8' }} />
      <Text style={{ color: '#8FE3D2', fontSize: 12, fontWeight: 500 }}>{value}</Text>
    </Flex>
  </Flex>
);

const Profile: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile>(MOCK_PROFILE);
  const [account, setAccount] = useState<Account>(MOCK_ACCOUNT);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    let cancelled = false;
    userService
      .getProfile()
      .then((data) => {
        if (!cancelled) setProfile(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    accountsService
      .getPrimaryAccount()
      .then((data) => {
        if (!cancelled) setAccount(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await authService.logout();
    } catch {
      // Endpoint not available yet — proceed to clear the session locally.
    } finally {
      tokenStorage.clear();
      navigate('/login', { replace: true });
    }
  };

  const sections: { label: string; rows: SettingsRowItem[] }[] = [
    {
      label: 'Account',
      rows: [
        { key: 'personal', label: 'Personal details', trailing: 'Update' },
        { key: 'security', label: 'Security & login' },
        { key: 'linked', label: 'Linked accounts', trailing: 'Manage' },
      ],
    },
    {
      label: 'Preferences',
      rows: [
        {
          key: 'notifications',
          label: 'Notification settings',
          trailing: 'Email · SMS · Push',
          onClick: () => navigate('/notifications'),
        },
        { key: 'language', label: 'Language & region', trailing: 'English' },
      ],
    },
    {
      label: 'Support',
      rows: [
        { key: 'help', label: 'Help center' },
        { key: 'contact', label: 'Contact support' },
      ],
    },
  ];

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Flex align="center" gap={16}>
          <Avatar
            size={64}
            style={{
              background: TEAL_TINT,
              color: token.colorPrimary,
              border: `2px solid ${token.colorPrimary}`,
              fontWeight: 600,
              fontSize: 22,
            }}
          >
            {getInitials(profile.fullName)}
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
          text={profile.deviceVerified ? 'This device is verified' : 'This device is not verified'}
        />

        <div
          style={{
            background: NAVY,
            borderRadius: 20,
            padding: '24px 24px 20px',
            margin: '24px 0',
          }}
        >
          <Text
            className="font-display"
            style={{
              display: 'block',
              color: '#FFFFFF',
              fontSize: 18,
              fontWeight: 600,
              marginBottom: 18,
            }}
          >
            Security status
          </Text>
          <Flex vertical gap={16}>
            <SecurityStatusRow
              label="Two-factor authentication"
              value={profile.twoFactorEnabled ? 'Active' : 'Inactive'}
            />
            <SecurityStatusRow label="Last verified session" value={profile.lastVerifiedSession} />
            <SecurityStatusRow
              label="Trusted devices"
              value={`${profile.trustedDevicesCount} device${profile.trustedDevicesCount === 1 ? '' : 's'}`}
            />
          </Flex>
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
            {loggingOut ? 'Logging out…' : 'Log out'}
          </Text>
        </Flex>
      </div>

      <BottomNav accountsPath={`/accounts/${account.id}`} />
    </div>
  );
};

export default Profile;
