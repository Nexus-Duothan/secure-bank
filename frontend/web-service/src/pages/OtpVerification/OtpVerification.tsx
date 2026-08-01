import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Input, Typography, theme } from 'antd';
import type { AxiosError } from 'axios';
import { authService } from '../../api/authService';
import tokenStorage from '../../api/tokenStorage';
import AuthLayout from '../../components/AuthLayout';
import TrustIndicator from '../../components/TrustIndicator';
import { formatCountdown, useCountdown } from '../../hooks/useCountdown';

const { Text, Link } = Typography;

const OTP_LENGTH = 6;
const RESEND_SECONDS = 30;

interface LocationState {
  preAuthToken?: string;
  usernameOrEmail?: string;
}

const OtpVerification: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const { preAuthToken } = (location.state as LocationState | null) ?? {};

  const [otp, setOtp] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { secondsLeft, restart, isFinished } = useCountdown(RESEND_SECONDS);

  useEffect(() => {
    if (!preAuthToken) {
      navigate('/login', { replace: true });
    }
  }, [preAuthToken, navigate]);

  const handleResend = () => {
    if (!isFinished) return;
    setOtp('');
    setError(null);
    restart();
  };

  const handleSubmit = async () => {
    if (!preAuthToken || otp.length !== OTP_LENGTH) return;
    setSubmitting(true);
    setError(null);
    try {
      const response = await authService.verifyOtp({ preAuthToken, totpCode: otp });
      tokenStorage.setTokens(response.accessToken, response.refreshToken);
      navigate('/dashboard');
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(axiosError.response?.data?.message ?? 'Invalid or expired code. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!preAuthToken) {
    return null;
  }

  return (
    <AuthLayout heading="Verify it's you">
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

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

        <Flex style={{ marginTop: 20, marginBottom: 24 }}>
          <Text style={{ color: token.colorTextSecondary }}>Didn't get a code?&nbsp;</Text>
          {isFinished ? (
            <Link onClick={handleResend} style={{ color: token.colorPrimary }}>
              Resend code
            </Link>
          ) : (
            <Text style={{ color: token.colorTextTertiary }}>
              Resend in {formatCountdown(secondsLeft)}
            </Text>
          )}
        </Flex>

        <Button
          type="primary"
          block
          loading={submitting}
          disabled={otp.length !== OTP_LENGTH}
          onClick={handleSubmit}
          style={{ fontWeight: 600 }}
        >
          Verify and continue
        </Button>
      </Card>

      <TrustIndicator text="This device will be remembered for 30 days" />
    </AuthLayout>
  );
};

export default OtpVerification;
