import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd';
import { CheckCircleFilled } from '@ant-design/icons';
import type { AxiosError } from 'axios';
import { authService } from '../../api/authService';
import AuthLayout from '../../components/AuthLayout';
import TrustIndicator from '../../components/TrustIndicator';
import { formatCountdown, useCountdown } from '../../hooks/useCountdown';

const { Paragraph, Text, Link } = Typography;

const RESEND_SECONDS = 30;

interface ForgotPasswordFormValues {
  email: string;
}

const ForgotPassword: React.FC = () => {
  const navigate = useNavigate();
  const { token } = theme.useToken();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [sentEmail, setSentEmail] = useState<string | null>(null);
  const { secondsLeft, restart, isFinished } = useCountdown(RESEND_SECONDS);

  const sendResetLink = async (email: string) => {
    setSubmitting(true);
    setError(null);
    try {
      await authService.requestPasswordReset(email);
      setSentEmail(email);
      restart();
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(
        axiosError.response?.data?.message ?? 'Unable to send a reset link. Please try again.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const handleResend = () => {
    if (!isFinished || !sentEmail) return;
    sendResetLink(sentEmail);
  };

  return (
    <AuthLayout heading="Reset your password">
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        {!sentEmail ? (
          <>
            <Paragraph style={{ color: token.colorTextSecondary, marginBottom: 24 }}>
              Enter the email linked to your account and we'll send you a password update link.
            </Paragraph>

            <Form<ForgotPasswordFormValues>
              layout="vertical"
              colon={false}
              requiredMark={false}
              disabled={submitting}
              onFinish={(values) => sendResetLink(values.email)}
            >
              <Form.Item
                label={<span style={{ fontWeight: 600 }}>Email Address</span>}
                name="email"
                rules={[
                  { required: true, message: 'Please enter your email' },
                  { type: 'email', message: 'Enter a valid email address' },
                ]}
              >
                <Input placeholder="you@email.com" size="large" autoComplete="email" />
              </Form.Item>

              <Form.Item style={{ marginBottom: 0 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  loading={submitting}
                  style={{ fontWeight: 600 }}
                >
                  Send update link
                </Button>
              </Form.Item>
            </Form>
          </>
        ) : (
          <Flex vertical align="center" style={{ textAlign: 'center', padding: '8px 0' }}>
            <CheckCircleFilled
              style={{ fontSize: 40, color: token.colorPrimary, marginBottom: 16 }}
            />
            <Paragraph style={{ color: token.colorText, marginBottom: 0 }}>
              If an account exists for <strong>{sentEmail}</strong>, a password update link has been
              sent. Check your inbox.
            </Paragraph>

            <div style={{ marginTop: 20 }}>
              {isFinished ? (
                <Link onClick={handleResend} style={{ color: token.colorPrimary }}>
                  Didn't get it? Resend
                </Link>
              ) : (
                <Text style={{ color: token.colorTextTertiary }}>
                  Didn't get it? Resend in {formatCountdown(secondsLeft)}
                </Text>
              )}
            </div>
          </Flex>
        )}
      </Card>

      <TrustIndicator text="256-bit encrypted • Identity verified" />

      <Text style={{ textAlign: 'center', marginTop: 24, color: token.colorTextSecondary }}>
        <Link onClick={() => navigate('/login')} style={{ color: token.colorPrimary }}>
          Back to sign in
        </Link>
      </Text>
    </AuthLayout>
  );
};

export default ForgotPassword;
