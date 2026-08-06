import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Divider,
  Flex,
  Input,
  Skeleton,
  Space,
  Typography,
  theme,
} from 'antd';
import {
  CheckCircleOutlined,
  CopyOutlined,
  LockOutlined,
  MobileOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons';
import { getApiErrorMessage } from '../../api/apiError';
import totpService, { type TotpSetupResponse } from '../../api/totpService';
import AuthLayout from '../../components/AuthLayout';
import TrustIndicator from '../../components/TrustIndicator';

const { Text, Title, Paragraph } = Typography;

const OTP_LENGTH = 6;

const toQrImageSrc = (qrCodeBase64: string) =>
  qrCodeBase64.startsWith('data:image/') ? qrCodeBase64 : `data:image/png;base64,${qrCodeBase64}`;

interface TotpSetupLocationState {
  userId?: string;
  username?: string;
  email?: string;
}

const StepRow: React.FC<{
  icon: React.ReactNode;
  title: string;
  description: string;
}> = ({ icon, title, description }) => {
  const { token } = theme.useToken();

  return (
    <Flex gap={12} align="flex-start">
      <Flex
        align="center"
        justify="center"
        style={{
          width: 36,
          height: 36,
          borderRadius: 8,
          background: '#DCEFEA',
          color: token.colorPrimary,
          flexShrink: 0,
        }}
      >
        {icon}
      </Flex>
      <div>
        <Text style={{ display: 'block', fontWeight: 600, color: token.colorText }}>{title}</Text>
        <Text style={{ color: token.colorTextSecondary, fontSize: 13 }}>{description}</Text>
      </div>
    </Flex>
  );
};

const TotpSetup: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const routeState = (location.state as TotpSetupLocationState | null) ?? null;
  const [setup, setSetup] = useState<TotpSetupResponse | null>(null);
  const [otp, setOtp] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!routeState?.userId) {
      navigate('/signup', { replace: true });
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    totpService
      .setup(routeState.userId, routeState.username ?? routeState.email)
      .then((response) => {
        if (!cancelled) setSetup(response);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(
            getApiErrorMessage(err, 'Unable to start authenticator setup. Please try again.')
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [navigate, routeState?.email, routeState?.userId, routeState?.username]);

  const handleCopySecret = async () => {
    if (!setup?.secretKey || !navigator.clipboard) return;
    await navigator.clipboard.writeText(setup.secretKey);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1800);
  };

  const handleEnable = async () => {
    if (!routeState?.userId || otp.length !== OTP_LENGTH) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await totpService.enable(routeState.userId, otp);
      if (!response.valid) {
        setError(response.message || 'Invalid authenticator code. Please try again.');
        return;
      }
      navigate('/login', {
        replace: true,
        state: {
          setupCompleteMessage:
            'Authenticator app setup complete. Sign in with your password and current code.',
        },
      });
    } catch (err) {
      setError(getApiErrorMessage(err, 'Invalid authenticator code. Please try again.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout heading="Set up authenticator app" maxWidth={520}>
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        <Alert
          type="info"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message="Authenticator setup is required before you can sign in."
          style={{ marginBottom: 24 }}
        />

        <Flex vertical gap={18}>
          <StepRow
            icon={<MobileOutlined />}
            title="Scan the QR code"
            description="Use Google Authenticator, Microsoft Authenticator, Authy, 1Password, or another TOTP app."
          />
          <StepRow
            icon={<LockOutlined />}
            title="Enter the current code"
            description="Your app will show a six digit code that refreshes every 30 seconds."
          />
        </Flex>

        <Divider />

        {loading ? (
          <Skeleton active paragraph={{ rows: 5 }} />
        ) : setup ? (
          <>
            <Flex vertical align="center" gap={16}>
              <div
                style={{
                  width: 220,
                  height: 220,
                  padding: 12,
                  borderRadius: 12,
                  border: `1px solid ${token.colorBorder}`,
                  background: token.colorBgContainer,
                }}
              >
                <img
                  src={toQrImageSrc(setup.qrCodeBase64)}
                  alt="Authenticator app setup QR code"
                  style={{ width: '100%', height: '100%', display: 'block' }}
                />
              </div>

              <Paragraph
                style={{
                  width: '100%',
                  margin: 0,
                  padding: '12px 14px',
                  borderRadius: 8,
                  border: `1px solid ${token.colorBorder}`,
                  background: token.colorFillAlter,
                  fontFamily: "'IBM Plex Mono', monospace",
                  color: token.colorText,
                  wordBreak: 'break-all',
                  textAlign: 'center',
                }}
              >
                {setup.secretKey}
              </Paragraph>

              <Button
                icon={copied ? <CheckCircleOutlined /> : <CopyOutlined />}
                onClick={handleCopySecret}
              >
                {copied ? 'Copied secret' : 'Copy setup key'}
              </Button>
            </Flex>

            <Divider />

            <Space direction="vertical" size={12} style={{ width: '100%' }}>
              <Text style={{ display: 'block', fontWeight: 600 }}>Authenticator code</Text>
              <div className="otp-boxes">
                <Input.OTP
                  length={OTP_LENGTH}
                  value={otp}
                  onInput={(cells) => setOtp(cells.join(''))}
                  formatter={(value) => value.replace(/\D/g, '')}
                  size="large"
                  disabled={submitting}
                  autoFocus
                />
              </div>
              <Button
                type="primary"
                block
                loading={submitting}
                disabled={otp.length !== OTP_LENGTH}
                onClick={handleEnable}
                style={{ fontWeight: 600 }}
              >
                Finish setup
              </Button>
            </Space>
          </>
        ) : null}
      </Card>

      {setup?.scratchCodes?.length ? (
        <Card
          styles={{ body: { padding: 24 } }}
          style={{ marginTop: 20, boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
        >
          <Title level={5} style={{ marginTop: 0, marginBottom: 8 }}>
            Recovery codes
          </Title>
          <Text style={{ display: 'block', color: token.colorTextSecondary, marginBottom: 16 }}>
            Keep these somewhere private. Each code can be used once if you lose access to your
            authenticator app.
          </Text>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
              gap: 8,
            }}
          >
            {setup.scratchCodes.map((code) => (
              <Text
                key={code}
                className="font-mono"
                style={{
                  padding: '8px 10px',
                  borderRadius: 8,
                  background: token.colorFillAlter,
                  textAlign: 'center',
                }}
              >
                {code}
              </Text>
            ))}
          </div>
        </Card>
      ) : null}

      <TrustIndicator text="SecureBank requires authenticator app verification for every sign in." />
    </AuthLayout>
  );
};

export default TotpSetup;
