import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Dropdown, Flex, Skeleton, Typography, message, theme } from 'antd';
import {
  BankOutlined,
  CheckCircleFilled,
  CreditCardOutlined,
  LinkOutlined,
  PlusOutlined,
  RightOutlined,
} from '@ant-design/icons';
import accountsService, { type Account } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import BottomNav from '../../components/BottomNav';

const { Text, Title } = Typography;
const NAVY = '#0B1B2B';

interface AccountsLocationState {
  otpSuccessMessage?: string;
}

const formatCurrency = (account: Account) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: account.currency,
    minimumFractionDigits: 2,
  }).format(account.balance);

const maskAccountNumber = (account: Account) => {
  const digits = (account.accountNumber ?? account.lastFourDigits).replace(/\D/g, '');
  return `**** **** *${digits.slice(-4).padStart(4, '*')}`;
};

const Accounts: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const [messageApi, contextHolder] = message.useMessage();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [selectedId, setSelectedId] = useState(accountSelection.getSelectedAccountId());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const state = location.state as AccountsLocationState | null;
    if (state?.otpSuccessMessage) {
      messageApi.success(state.otpSuccessMessage);
      window.history.replaceState({}, document.title);
    }
  }, [location.state, messageApi]);

  useEffect(() => {
    let cancelled = false;
    accountsService
      .getAccounts()
      .then((data) => {
        if (cancelled) return;
        const available = data || [];
        if (available.length > 0) {
          const nextSelected = available.some((account) => account.id === selectedId)
            ? selectedId
            : available[0].id;
          accountSelection.setSelectedAccountId(nextSelected);
          setSelectedId(nextSelected);
        }
        setAccounts(available);
      })
      .catch(() => {
        if (!cancelled) setAccounts([]);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  const selectAccount = (account: Account) => {
    accountSelection.setSelectedAccountId(account.id);
    setSelectedId(account.id);
    navigate(`/accounts/${account.id}`);
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      {contextHolder}
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Flex justify="space-between" align="center" style={{ marginBottom: 28 }}>
          <div>
            <Title
              level={2}
              className="font-display"
              style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
            >
              Accounts
            </Title>
            <Text style={{ color: token.colorTextSecondary }}>Choose the account to use</Text>
          </div>
          <Dropdown
            trigger={['click']}
            placement="bottomRight"
            menu={{
              items: [
                { key: 'open', icon: <BankOutlined />, label: 'Open new account' },
                { key: 'link', icon: <LinkOutlined />, label: 'Link existing account' },
                { key: 'card', icon: <CreditCardOutlined />, label: 'Add credit card' },
              ],
              onClick: ({ key }) => {
                if (key === 'open') navigate('/accounts/open');
                if (key === 'link') navigate('/accounts/link');
                if (key === 'card') navigate('/accounts/cards/link');
              },
            }}
          >
            <Button
              type="primary"
              shape="circle"
              size="large"
              icon={<PlusOutlined />}
              aria-label="Add account or card"
              title="Add account or card"
              style={{ width: 48, height: 48, flex: '0 0 48px' }}
            />
          </Dropdown>
        </Flex>

        {loading ? (
          <Skeleton active paragraph={{ rows: 5 }} />
        ) : (
          <Flex vertical gap={14}>
            {accounts.map((account) => {
              const selected = account.id === selectedId;
              return (
                <button
                  key={account.id}
                  type="button"
                  onClick={() => selectAccount(account)}
                  style={{
                    width: '100%',
                    minHeight: 154,
                    padding: 22,
                    textAlign: 'left',
                    borderRadius: 16,
                    border: selected
                      ? `2px solid ${token.colorPrimary}`
                      : `1px solid ${token.colorBorder}`,
                    background: selected ? NAVY : token.colorBgContainer,
                    color: selected ? '#FFFFFF' : token.colorText,
                    boxShadow: selected ? '0 12px 30px rgba(11, 27, 43, 0.12)' : 'none',
                  }}
                >
                  <Flex justify="space-between" align="flex-start" gap={12}>
                    <div style={{ minWidth: 0 }}>
                      <Text
                        style={{
                          display: 'block',
                          color: selected ? '#FFFFFF' : token.colorText,
                          fontSize: 17,
                          fontWeight: 600,
                        }}
                      >
                        {account.nickname}
                      </Text>
                      <Text
                        className="font-mono"
                        style={{
                          color: selected ? 'rgba(255,255,255,0.64)' : token.colorTextTertiary,
                        }}
                      >
                        {maskAccountNumber(account)}
                      </Text>
                    </div>
                    {selected ? (
                      <CheckCircleFilled style={{ color: '#8FE3D2', fontSize: 20 }} />
                    ) : (
                      <RightOutlined style={{ color: token.colorTextTertiary, marginTop: 4 }} />
                    )}
                  </Flex>
                  <Text
                    className="font-mono"
                    style={{
                      display: 'block',
                      marginTop: 26,
                      color: selected ? '#FFFFFF' : token.colorText,
                      fontSize: 25,
                      fontWeight: 600,
                    }}
                  >
                    {formatCurrency(account)}
                  </Text>
                  <Text
                    style={{
                      display: 'block',
                      marginTop: 8,
                      color: selected ? '#8FE3D2' : token.colorPrimary,
                      fontSize: 12,
                      fontWeight: 600,
                    }}
                  >
                    {selected ? 'Selected account' : 'Tap to select'}
                  </Text>
                </button>
              );
            })}
          </Flex>
        )}
      </div>
      <BottomNav />
    </div>
  );
};

export default Accounts;
