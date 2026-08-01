import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Button, Flex, Typography, theme } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import accountsService, { type AccountDetail } from '../../api/accountsService';
import { buildDemoAccountDetail } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const NAVY = '#0B1B2B';

const buildMockAccount = (id: string): AccountDetail => buildDemoAccountDetail(id);

const formatBalance = (currency: string, value: number) =>
  `${currency} ${new Intl.NumberFormat('en-US', { minimumFractionDigits: 2 }).format(value)}`;

interface InfoRowProps {
  label: string;
  value: string;
  valueColor?: string;
  showDivider?: boolean;
}

const InfoRow: React.FC<InfoRowProps> = ({ label, value, valueColor, showDivider = true }) => {
  const { token } = theme.useToken();

  return (
    <Flex
      justify="space-between"
      align="center"
      style={{
        padding: '18px 0',
        borderBottom: showDivider ? `1px solid ${token.colorBorder}` : 'none',
      }}
    >
      <Text style={{ fontSize: 15, color: token.colorTextSecondary }}>{label}</Text>
      <Text style={{ fontSize: 16, fontWeight: 600, color: valueColor ?? token.colorText }}>
        {value}
      </Text>
    </Flex>
  );
};

const AccountDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const accountId = id ?? 'acc-demo-primary';
  const [account, setAccount] = useState<AccountDetail>(() => buildMockAccount(accountId));

  useEffect(() => {
    let cancelled = false;
    setAccount(buildMockAccount(accountId));

    accountsService
      .getAccountById(accountId)
      .then((data) => {
        if (!cancelled) setAccount(data);
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });

    return () => {
      cancelled = true;
    };
  }, [accountId]);

  const isActive = account.status === 'Active - Verified';

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 28 }}>
          <LeftOutlined
            onClick={() => navigate('/dashboard')}
            style={{ fontSize: 20, color: token.colorText, cursor: 'pointer' }}
          />
          <Title
            level={3}
            className="font-display"
            style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
          >
            {account.nickname}
          </Title>
        </Flex>

        <div
          style={{
            background: token.colorBgContainer,
            borderRadius: 20,
            border: `1px solid ${token.colorBorder}`,
            padding: '24px 24px 20px',
            marginBottom: 24,
          }}
        >
          <Text style={{ fontSize: 14, color: token.colorTextSecondary }}>Available balance</Text>
          <Text
            className="font-mono"
            style={{
              display: 'block',
              color: NAVY,
              fontSize: 36,
              fontWeight: 600,
              margin: '10px 0 12px',
            }}
          >
            {formatBalance(account.currency, account.balance)}
          </Text>
          <Text className="font-mono" style={{ fontSize: 14, color: token.colorTextTertiary }}>
            Account - {account.accountNumber} - IFSC {account.ifscCode}
          </Text>
        </div>

        <div
          style={{
            background: token.colorBgContainer,
            borderRadius: 20,
            border: `1px solid ${token.colorBorder}`,
            padding: '4px 24px',
            marginBottom: 24,
          }}
        >
          <InfoRow label="Account type" value={account.accountTypeLabel} />
          <InfoRow label="Opened on" value={account.openedOn} />
          <InfoRow label="Home branch" value={account.homeBranch} />
          <InfoRow
            label="Status"
            value={account.status}
            valueColor={isActive ? token.colorPrimary : token.colorText}
            showDivider={false}
          />
        </div>

        <Button type="primary" size="large" block style={{ fontWeight: 600, height: 52 }}>
          Download statement
        </Button>
      </div>
    </div>
  );
};

export default AccountDetails;
