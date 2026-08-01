import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { authService } from '../../api/authService';
import AuthLayout from '../../components/AuthLayout';
import { PasswordStrengthMeter } from '../../components/PasswordStrength';
import TrustIndicator from '../../components/TrustIndicator';
import { PASSWORD_COMPLEXITY_MESSAGE, PASSWORD_PATTERN } from '../../components/passwordRules';

const { Paragraph } = Typography;

const SUCCESS_REDIRECT_DELAY_MS = 2000;

interface ResetPasswordFormValues {
  newPassword: string;
  confirmPassword: string;
  totpCode: string;
}

type ScreenStatus = 'form' | 'invalid' | 'success';

const ResetPassword: React.FC = () => {
  const navigate = useNavigate();
  const { token: resetToken } = useParams<{ token: string }>();
  const { token } = theme.useToken();
  const [form] = Form.useForm<ResetPasswordFormValues>();
  const newPassword = Form.useWatch('newPassword', form) ?? '';
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<ScreenStatus>(resetToken ? 'form' : 'invalid');

  useEffect(() => {
    if (status !== 'success') return;
    const timer = setTimeout(() => navigate('/login'), SUCCESS_REDIRECT_DELAY_MS);
    return () => clearTimeout(timer);
  }, [status, navigate]);

  const handleFinish = async (values: ResetPasswordFormValues) => {
    if (!resetToken) return;
    setSubmitting(true);
    setError(null);
    try {
      await authService.resetPassword(resetToken, values.newPassword, values.totpCode);
      setStatus('success');
    } catch {
      setStatus('invalid');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout heading="Set a new password">
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {status === 'form' && (
          <>
            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

            <Paragraph style={{ color: token.colorTextSecondary, marginBottom: 24 }}>
              Choose a strong password you haven't used before.
            </Paragraph>

            <Form<ResetPasswordFormValues>
              form={form}
              layout="vertical"
              colon={false}
              requiredMark={false}
              disabled={submitting}
              onFinish={handleFinish}
            >
              <Form.Item
                label={<span style={{ fontWeight: 600 }}>New Password</span>}
                name="newPassword"
                style={{ marginBottom: 8 }}
                rules={[
                  { required: true, message: 'Please create a new password' },
                  { pattern: PASSWORD_PATTERN, message: PASSWORD_COMPLEXITY_MESSAGE },
                ]}
              >
                <Input.Password size="large" autoComplete="new-password" />
              </Form.Item>

              <PasswordStrengthMeter password={newPassword} />

              <Form.Item
                label={<span style={{ fontWeight: 600 }}>Confirm Password</span>}
                name="confirmPassword"
                dependencies={['newPassword']}
                rules={[
                  { required: true, message: 'Please confirm your password' },
                  ({ getFieldValue }) => ({
                    validator: (_, value) =>
                      !value || getFieldValue('newPassword') === value
                        ? Promise.resolve()
                        : Promise.reject(new Error('Passwords do not match')),
                  }),
                ]}
              >
                <Input.Password size="large" autoComplete="new-password" />
              </Form.Item>

              <Form.Item
                label={<span style={{ fontWeight: 600 }}>Authenticator Code</span>}
                name="totpCode"
                style={{ marginBottom: 0 }}
                rules={[
                  { required: true, message: 'Please enter your 6-digit authenticator code' },
                  { pattern: /^\d{6}$/, message: 'Code must be 6 digits' },
                ]}
              >
                <Input
                  size="large"
                  maxLength={6}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                />
              </Form.Item>

              <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  block
                  loading={submitting}
                  style={{ fontWeight: 600 }}
                >
                  Reset password
                </Button>
              </Form.Item>
            </Form>
          </>
        )}

        {status === 'invalid' && (
          <Flex vertical align="center" style={{ textAlign: 'center', padding: '8px 0' }}>
            <CloseCircleFilled
              style={{ fontSize: 40, color: token.colorError, marginBottom: 16 }}
            />
            <Paragraph style={{ color: token.colorText, marginBottom: 24 }}>
              This reset link has expired or is invalid.
            </Paragraph>
            <Button
              type="primary"
              block
              onClick={() => navigate('/forgot-password')}
              style={{ fontWeight: 600 }}
            >
              Request a new link
            </Button>
          </Flex>
        )}

        {status === 'success' && (
          <Flex vertical align="center" style={{ textAlign: 'center', padding: '8px 0' }}>
            <CheckCircleFilled
              style={{ fontSize: 40, color: token.colorPrimary, marginBottom: 16 }}
            />
            <Paragraph style={{ color: token.colorText, marginBottom: 24 }}>
              Your password has been reset.
            </Paragraph>
            <Button
              type="primary"
              block
              onClick={() => navigate('/login')}
              style={{ fontWeight: 600 }}
            >
              Continue to sign in
            </Button>
          </Flex>
        )}
      </Card>

      <TrustIndicator text="256-bit encrypted • Identity verified" />
    </AuthLayout>
  );
};

export default ResetPassword;
