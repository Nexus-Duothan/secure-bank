import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, Card, Divider, Empty, Flex, Spin, Tag, Typography, message, theme } from 'antd';
import { LeftOutlined, CopyOutlined } from '@ant-design/icons';
import accountsService, { type AccountDetail } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';

const { Text, Title } = Typography;
const NAVY = '#0B1B2B';

const AccountDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { token } = theme.useToken();
  const [messageApi, contextHolder] = message.useMessage();
  const navigate = useNavigate();
  const location = useLocation();
  const accountId = id ?? '';
  const [account, setAccount] = useState<AccountDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const state = location.state as { otpSuccessMessage?: string } | null;
    if (state?.otpSuccessMessage) {
      messageApi.success(state.otpSuccessMessage);
      window.history.replaceState({}, document.title);
    }
  }, [location.state, messageApi]);

  useEffect(() => {
    if (!accountId) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    accountsService
      .getAccountById(accountId)
      .then((data) => {
        if (!cancelled) {
          accountSelection.setSelectedAccountId(data.id);
          setAccount(data);
          setError(null);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('Account details unavailable');
          setAccount(null);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [accountId]);

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    messageApi.success(`${label} copied to clipboard`);
  };

  if (loading) {
    return (
      <Flex
        justify="center"
        align="center"
        style={{ minHeight: '100vh', background: token.colorBgLayout }}
      >
        <Spin size="large" />
      </Flex>
    );
  }

  if (!account) {
    return (
      <div style={{ minHeight: '100vh', background: token.colorBgLayout, padding: '32px 20px' }}>
        <Button
          icon={<LeftOutlined />}
          onClick={() => navigate('/dashboard')}
          style={{ marginBottom: 20 }}
        >
          Back to Dashboard
        </Button>
        <Empty description={error || 'Account not found'} />
      </div>
    );
  }

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      {contextHolder}
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Flex justify="space-between" align="center" style={{ marginBottom: 24 }}>
          <Button shape="circle" icon={<LeftOutlined />} onClick={() => navigate('/dashboard')} />
          <Title level={4} style={{ margin: 0 }}>
            Account Details
          </Title>
          <div style={{ width: 32 }} />
        </Flex>

        <Card style={{ background: NAVY, borderRadius: 20, color: '#FFF', marginBottom: 24 }}>
          <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 14 }}>
            {account.nickname || account.accountTypeLabel || 'Account'}
          </Text>
          <Title level={2} style={{ color: '#FFF', margin: '8px 0 16px' }}>
            ${account.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })}{' '}
            {account.currency}
          </Title>
          <Flex justify="space-between" align="center">
            <Text style={{ color: 'rgba(255,255,255,0.6)', fontFamily: 'monospace' }}>
              {account.accountNumber}
            </Text>
            <Button
              type="text"
              icon={<CopyOutlined style={{ color: '#FFF' }} />}
              onClick={() => copyToClipboard(account.accountNumber, 'Account Number')}
            />
          </Flex>
        </Card>

        <Title level={5} style={{ marginBottom: 12 }}>
          Account Information
        </Title>
        <Card style={{ borderRadius: 16, marginBottom: 24 }}>
          <Flex justify="space-between" style={{ padding: '8px 0' }}>
            <Text type="secondary">Status</Text>
            <Tag color={account.status === 'ACTIVE' ? 'green' : 'red'}>{account.status}</Tag>
          </Flex>
          <Divider style={{ margin: '8px 0' }} />
          <Flex justify="space-between" style={{ padding: '8px 0' }}>
            <Text type="secondary">Product</Text>
            <Text style={{ fontWeight: 600 }}>{account.accountTypeLabel || 'Savings'}</Text>
          </Flex>
          <Divider style={{ margin: '8px 0' }} />
          <Flex justify="space-between" style={{ padding: '8px 0' }}>
            <Text type="secondary">Currency</Text>
            <Text style={{ fontWeight: 600 }}>{account.currency}</Text>
          </Flex>
        </Card>
      </div>
    </div>
  );
};

export default AccountDetails;
