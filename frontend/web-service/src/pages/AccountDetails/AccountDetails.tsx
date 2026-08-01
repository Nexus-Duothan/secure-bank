import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import { Button, Flex, Modal, Typography, message, theme } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import accountsService, { type AccountDetail } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import BankCardVisual from '../../components/BankCardVisual';
import { buildDemoAccountDetail } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const CARD_MINT = '#8FE3D2';

const buildMockAccount = (id: string): AccountDetail => buildDemoAccountDetail(id);

const formatBalance = (currency: string, value: number) =>
  `${currency} ${new Intl.NumberFormat('en-US', { minimumFractionDigits: 2 }).format(value)}`;

const maskDebitCardNumber = (accountNumber: string) => {
  const digits = accountNumber.replace(/\D/g, '');
  const lastFour = digits.slice(-4).padStart(4, '0');
  return `4910 12** **** ${lastFour}`;
};

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
  const [messageApi, contextHolder] = message.useMessage();
  const navigate = useNavigate();
  const location = useLocation();
  const accountId = id ?? 'acc-demo-primary';
  const [account, setAccount] = useState<AccountDetail>(() => buildMockAccount(accountId));
  const [statementLoading, setStatementLoading] = useState(false);
  const [statementPreviewUrl, setStatementPreviewUrl] = useState<string | null>(null);
  const [statementPreviewOpen, setStatementPreviewOpen] = useState(false);

  useEffect(() => {
    const state = location.state as { otpSuccessMessage?: string } | null;
    if (state?.otpSuccessMessage) {
      messageApi.success(state.otpSuccessMessage);
      window.history.replaceState({}, document.title);
    }
  }, [location.state, messageApi]);

  useEffect(() => {
    let cancelled = false;
    setAccount(buildMockAccount(accountId));

    accountsService
      .getAccountById(accountId)
      .then((data) => {
        if (!cancelled) {
          accountSelection.setSelectedAccountId(data.id);
          setAccount(data);
        }
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });

    return () => {
      cancelled = true;
    };
  }, [accountId]);

  const isActive = account.status === 'Active - Verified';
  const primaryCard = account.cards?.[0];
  const maskedDebitCard = primaryCard?.maskedNumber ?? maskDebitCardNumber(account.accountNumber);

  const createStatementUrl = async () => {
    const blob = await accountsService.downloadStatement(accountId);
    return window.URL.createObjectURL(
      blob.type ? blob : new Blob([blob], { type: 'application/pdf' })
    );
  };

  const handlePreviewStatement = async () => {
    setStatementLoading(true);
    try {
      if (statementPreviewUrl) {
        window.URL.revokeObjectURL(statementPreviewUrl);
      }
      const url = await createStatementUrl();
      setStatementPreviewUrl(url);
      setStatementPreviewOpen(true);
    } catch {
      messageApi.error('We could not load the account statement preview right now.');
    } finally {
      setStatementLoading(false);
    }
  };

  const handleDownloadStatement = async () => {
    setStatementLoading(true);
    try {
      const url = await createStatementUrl();
      const link = document.createElement('a');
      link.href = url;
      link.download = `securebank-statement-${accountId}.pdf`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.setTimeout(() => window.URL.revokeObjectURL(url), 1000);
    } catch {
      messageApi.error('We could not download the account statement right now.');
    } finally {
      setStatementLoading(false);
    }
  };

  const closeStatementPreview = () => {
    setStatementPreviewOpen(false);
    if (statementPreviewUrl) {
      window.URL.revokeObjectURL(statementPreviewUrl);
      setStatementPreviewUrl(null);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      {contextHolder}
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 28 }}>
          <LeftOutlined
            onClick={() => navigate('/accounts')}
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
            position: 'relative',
            overflow: 'hidden',
            background:
              'linear-gradient(140deg, rgba(17,35,58,1) 0%, rgba(18,54,84,1) 42%, rgba(31,122,108,1) 100%)',
            borderRadius: 20,
            padding: '24px 24px 22px',
            marginBottom: 24,
            boxShadow: '0 20px 44px rgba(11, 27, 43, 0.16)',
            aspectRatio: '1.586 / 1',
            minHeight: 232,
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
          }}
        >
          <div
            style={{
              position: 'absolute',
              top: -48,
              right: -42,
              width: 168,
              height: 168,
              borderRadius: '50%',
              background: 'rgba(143, 227, 210, 0.16)',
            }}
          />
          <div
            style={{
              position: 'absolute',
              bottom: -54,
              left: -28,
              width: 140,
              height: 140,
              borderRadius: '50%',
              background: 'rgba(255, 255, 255, 0.08)',
            }}
          />

          <Flex justify="space-between" align="flex-start" style={{ position: 'relative' }}>
            <div>
              <Text
                className="font-display"
                style={{ display: 'block', color: '#FFFFFF', fontSize: 22, fontWeight: 600 }}
              >
                SecureBank
              </Text>
              <Text style={{ display: 'block', marginTop: 4, color: 'rgba(255,255,255,0.74)' }}>
                {primaryCard?.productName ?? `Debit card for ${account.nickname}`}
              </Text>
            </div>
            <div
              style={{
                padding: '6px 12px',
                borderRadius: 999,
                background: 'rgba(255,255,255,0.12)',
                border: '1px solid rgba(255,255,255,0.12)',
              }}
            >
              <Text style={{ color: CARD_MINT, fontSize: 12, fontWeight: 600 }}>
                {primaryCard?.cardType ?? 'DEBIT'}
              </Text>
            </div>
          </Flex>

          <Flex
            align="center"
            gap={14}
            style={{ position: 'relative', marginTop: 24, marginBottom: 22 }}
          >
            <div
              style={{
                width: 46,
                height: 34,
                borderRadius: 8,
                background: 'linear-gradient(135deg, #EED9A5 0%, #C8A96A 48%, #F5E3B3 100%)',
                boxShadow: 'inset 0 0 0 1px rgba(17, 35, 58, 0.12)',
              }}
            >
              <div
                style={{
                  width: '100%',
                  height: '100%',
                  background:
                    'linear-gradient(90deg, transparent 30%, rgba(17,35,58,0.12) 30%, rgba(17,35,58,0.12) 36%, transparent 36%, transparent 64%, rgba(17,35,58,0.12) 64%, rgba(17,35,58,0.12) 70%, transparent 70%)',
                }}
              />
            </div>
            <Text
              style={{
                color: '#FFFFFF',
                fontSize: 28,
                fontWeight: 500,
                letterSpacing: 1.2,
              }}
            >
              )))
            </Text>
          </Flex>

          <Text
            className="font-mono"
            style={{
              display: 'block',
              position: 'relative',
              color: '#FFFFFF',
              fontSize: 28,
              fontWeight: 500,
              marginBottom: 22,
              letterSpacing: 1.4,
            }}
          >
            {maskedDebitCard}
          </Text>

          <Flex align="end" style={{ position: 'relative' }}>
            <div>
              <Text
                style={{
                  display: 'block',
                  color: 'rgba(255,255,255,0.68)',
                  fontSize: 11,
                  fontWeight: 600,
                  textTransform: 'uppercase',
                  letterSpacing: 0.6,
                }}
              >
                Available balance
              </Text>
              <Text
                className="font-mono"
                style={{
                  display: 'block',
                  marginTop: 8,
                  color: '#FFFFFF',
                  fontSize: 28,
                  fontWeight: 600,
                }}
              >
                {formatBalance(account.currency, account.balance)}
              </Text>
            </div>
          </Flex>
        </div>

        {account.cards?.slice(1).map((card) => (
          <div key={card.id} style={{ marginBottom: 24 }}>
            <BankCardVisual card={card} accountName={account.nickname} />
          </div>
        ))}

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
          <InfoRow label="Ownership" value={account.ownershipLabel} />
          <InfoRow
            label="Status"
            value={account.status}
            valueColor={isActive ? token.colorPrimary : token.colorText}
            showDivider={false}
          />
        </div>

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
              marginBottom: 6,
              fontSize: 12,
              fontWeight: 600,
              letterSpacing: 0.4,
              textTransform: 'uppercase',
              color: token.colorTextTertiary,
            }}
          >
            Monthly statement
          </Text>
          <Text style={{ display: 'block', fontSize: 18, fontWeight: 600, color: token.colorText }}>
            July 2026 statement
          </Text>
          <Flex gap={12} style={{ marginTop: 18 }}>
            <Button
              size="large"
              style={{ flex: 1, fontWeight: 600 }}
              loading={statementLoading}
              onClick={() => void handlePreviewStatement()}
            >
              Preview
            </Button>
            <Button
              type="primary"
              size="large"
              style={{ flex: 1, fontWeight: 600 }}
              loading={statementLoading}
              onClick={() => void handleDownloadStatement()}
            >
              Download PDF
            </Button>
          </Flex>
        </div>
      </div>

      <Modal
        open={statementPreviewOpen}
        onCancel={closeStatementPreview}
        title="Statement preview"
        width={960}
        footer={[
          <Button key="close" onClick={closeStatementPreview}>
            Close
          </Button>,
          <Button
            key="download"
            type="primary"
            loading={statementLoading}
            onClick={() => void handleDownloadStatement()}
          >
            Download PDF
          </Button>,
        ]}
      >
        {statementPreviewUrl ? (
          <iframe
            title="Statement preview"
            src={statementPreviewUrl}
            style={{ width: '100%', height: '70vh', border: 'none', borderRadius: 12 }}
          />
        ) : null}
      </Modal>
    </div>
  );
};

export default AccountDetails;
