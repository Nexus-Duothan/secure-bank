import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Avatar, Flex, Typography, theme } from 'antd';
import {
  SwapOutlined,
  DollarOutlined,
  PercentageOutlined,
  CaretUpOutlined,
} from '@ant-design/icons';
import accountsService, { type Account, type Transaction } from '../../api/accountsService';
import userService, { type UserProfile } from '../../api/userService';
import TransactionRow from '../../components/TransactionRow';
import BottomNav from '../../components/BottomNav';
import {
  DEMO_PROFILE,
  DEMO_PRIMARY_ACCOUNT,
  DEMO_RECENT_TRANSACTIONS,
} from '../../mocks/demoCustomer';

const { Text, Title, Link } = Typography;

const TEAL_TINT = '#DCEFEA';
const NAVY = '#0B1B2B';

const MOCK_ACCOUNT: Account = DEMO_PRIMARY_ACCOUNT;
const MOCK_TRANSACTIONS: Transaction[] = DEMO_RECENT_TRANSACTIONS;
const MOCK_PROFILE: UserProfile = DEMO_PROFILE;

const QUICK_ACTIONS = [
  { key: 'transfer', label: 'Transfer', icon: <SwapOutlined />, path: '/transfer' },
  { key: 'pay', label: 'Pay bill', icon: <DollarOutlined />, path: '/pay' },
  { key: 'loans', label: 'Loans', icon: <PercentageOutlined />, path: '/loans/apply' },
];

const getGreeting = () => {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 18) return 'Good afternoon';
  return 'Good evening';
};

const getInitials = (name: string) =>
  name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

const formatCurrency = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

const Dashboard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<UserProfile>(MOCK_PROFILE);
  const [account, setAccount] = useState<Account>(MOCK_ACCOUNT);
  const [transactions, setTransactions] = useState<Transaction[]>(MOCK_TRANSACTIONS);

  useEffect(() => {
    let cancelled = false;

    userService
      .getProfile()
      .then((data) => {
        if (!cancelled) setProfile(data);
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });

    accountsService
      .getPrimaryAccount()
      .then((data) => {
        if (!cancelled) setAccount(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    accountsService
      .getRecentTransactions(4)
      .then((data) => {
        if (!cancelled) setTransactions(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const fullName = profile.fullName;

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Flex justify="space-between" align="center" style={{ marginBottom: 28 }}>
          <div>
            <Text style={{ color: token.colorTextTertiary, fontSize: 15 }}>{getGreeting()}</Text>
            <Title
              level={3}
              className="font-display"
              style={{ margin: '2px 0 0', color: token.colorText, fontWeight: 600 }}
            >
              {fullName}
            </Title>
          </div>
          <Avatar
            size={48}
            style={{ background: NAVY, fontWeight: 600, cursor: 'pointer' }}
            onClick={() => navigate('/profile?panel=personal')}
          >
            {getInitials(fullName)}
          </Avatar>
        </Flex>

        <div
          style={{
            background: NAVY,
            borderRadius: 20,
            padding: '24px 24px 28px',
            marginBottom: 28,
          }}
        >
          <Flex justify="space-between" align="center">
            <Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 14 }}>
              Total balance · {account.nickname}
            </Text>
            <Flex
              align="center"
              gap={6}
              style={{
                background: 'rgba(31,122,108,0.35)',
                padding: '4px 12px',
                borderRadius: 999,
              }}
            >
              <span
                style={{
                  width: 6,
                  height: 6,
                  borderRadius: '50%',
                  background: '#3FD6B8',
                  display: 'inline-block',
                }}
              />
              <Text style={{ color: '#8FE3D2', fontSize: 12, fontWeight: 500 }}>
                Verified {account.verifiedLabel}
              </Text>
            </Flex>
          </Flex>

          <Text
            className="font-mono"
            style={{
              display: 'block',
              color: '#FFFFFF',
              fontSize: 40,
              fontWeight: 600,
              margin: '16px 0 20px',
            }}
          >
            {formatCurrency(account.balance, account.currency)}
          </Text>

          <Flex justify="space-between" align="center">
            <Flex align="center" gap={6}>
              <CaretUpOutlined style={{ color: '#3FD6B8', fontSize: 12 }} />
              <Text style={{ color: '#3FD6B8', fontSize: 13, fontWeight: 500 }}>
                {account.monthlyChangePercent}% this month
              </Text>
            </Flex>
            <Text
              className="font-mono"
              style={{ color: 'rgba(255,255,255,0.55)', fontSize: 14, letterSpacing: 1 }}
            >
              •••• {account.lastFourDigits}
            </Text>
          </Flex>
        </div>

        <Flex justify="space-between" style={{ marginBottom: 32 }}>
          {QUICK_ACTIONS.map((action) => (
            <Flex
              key={action.key}
              vertical
              align="center"
              gap={10}
              style={{ cursor: 'pointer' }}
              onClick={() => navigate(action.path)}
            >
              <Flex
                align="center"
                justify="center"
                style={{
                  width: 64,
                  height: 64,
                  borderRadius: '50%',
                  background: TEAL_TINT,
                  color: token.colorPrimary,
                  fontSize: 22,
                }}
              >
                {action.icon}
              </Flex>
              <Text style={{ fontSize: 13, color: token.colorTextSecondary, fontWeight: 500 }}>
                {action.label}
              </Text>
            </Flex>
          ))}
        </Flex>

        <Flex justify="space-between" align="center" style={{ marginBottom: 16 }}>
          <Title
            level={4}
            className="font-display"
            style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
          >
            Recent activity
          </Title>
          <Link
            onClick={() => navigate('/activity')}
            style={{ color: token.colorPrimary, fontWeight: 500 }}
          >
            View all
          </Link>
        </Flex>

        <div
          style={{
            background: token.colorBgContainer,
            borderRadius: 16,
            border: `1px solid ${token.colorBorder}`,
            overflow: 'hidden',
          }}
        >
          {transactions.map((txn, index) => (
            <TransactionRow
              key={txn.id}
              avatarLabel={txn.merchant.charAt(0).toUpperCase()}
              merchant={txn.merchant}
              category={txn.category}
              date={txn.date}
              amount={txn.amount}
              currency={account.currency}
              verified={txn.verified}
              showDivider={index < transactions.length - 1}
            />
          ))}
        </div>
      </div>

      <BottomNav accountsPath={`/accounts/${account.id}`} />
    </div>
  );
};

export default Dashboard;
