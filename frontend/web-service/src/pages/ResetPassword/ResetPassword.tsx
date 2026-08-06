import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd';
import { CheckCircleFilled, CloseCircleFilled } from '@ant-design/icons';
import { authService } from '../../api/authService';
import { getApiErrorMessage } from '../../api/apiError';
import AuthLayout from '../../components/AuthLayout';
import { PasswordStrengthMeter } from '../../components/PasswordStrength';
import TrustIndicator from '../../components/TrustIndicator';
import { PASSWORD_COMPLEXITY_MESSAGE, PASSWORD_PATTERN } from '../../components/passwordRules';

const { Paragraph, Text } = Typography;

const OTP_LENGTH = 6;

interface ResetPasswordFormValues {
  newPassword: string;
  confirmPassword: string;
}

/**
 * 'password' collects the new password, 'verify' asks for the authenticator code that applies it.
 * The password is only sent to the backend on the verify step, together with the code.
 */
type ScreenStatus = 'password' | 'verify' | 'invalid' | 'success';

const ResetPassword: React.FC = () => {
  const navigate = useNavigate();
  const { token: resetToken } = useParams<{ token: string }>();
  const { token } = theme.useToken();
  const [form] = Form.useForm<ResetPasswordFormValues>();
  const newPassword = Form.useWatch('newPassword', form) ?? '';
  const [status, setStatus] = useState<ScreenStatus>(resetToken ? 'password' : 'invalid');
  const [stagedPassword, setStagedPassword] = useState('');
  const [otp, setOtp] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handlePasswordStep = (values: ResetPasswordFormValues) => {
    setStagedPassword(values.newPassword);
    setError(null);
    setStatus('verify');
  };

  const handleVerify = async () => {
    if (!resetToken || otp.length !== OTP_LENGTH) return;
    setSubmitting(true);
    setError(null);
    try {
      await authService.resetPassword(resetToken, stagedPassword, otp);
      setStatus('success');
    } catch (err) {
      setOtp('');
      const failure = getApiErrorMessage(
        err,
        'That code did not work. Open your authenticator app and try again.'
      );
      // A dead link cannot be fixed by retyping the code, so send them back for a fresh one.
      if (/token/i.test(failure)) {
        setError(null);
        setStatus('invalid');
        return;
      }
      setError(failure);
    } finally {
      setSubmitting(false);
    }
  };

  const handleBackToPassword = () => {
    setOtp('');
    setError(null);
    setStatus('password');
  };

  return (
    <AuthLayout heading={status === 'verify' ? "Verify it's you" : 'Set a new password'}>
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        {status === 'password' && (
          <>
            <Paragraph style={{ color: token.colorTextSecondary, marginBottom: 24 }}>
              Choose a strong password you haven't used before.
            </Paragraph>

            <Form<ResetPasswordFormValues>
              form={form}
              layout="vertical"
              colon={false}
              requiredMark={false}
              onFinish={handlePasswordStep}
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

              <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
                <Button type="primary" htmlType="submit" block style={{ fontWeight: 600 }}>
                  Apply
                </Button>
              </Form.Item>
            </Form>
          </>
        )}

        {status === 'verify' && (
          <>
            <Text
              style={{
                display: 'block',
                marginBottom: 16,
                color: token.colorTextSecondary,
                fontSize: 13,
              }}
            >
              Open your authenticator app and enter the current six digit code to set your new
              password.
            </Text>

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
              <Text style={{ color: token.colorTextSecondary }}>
                Typed the wrong password?&nbsp;
              </Text>
              <Typography.Link onClick={handleBackToPassword} style={{ color: token.colorPrimary }}>
                Go back
              </Typography.Link>
            </Flex>

            <Button
              type="primary"
              block
              loading={submitting}
              disabled={otp.length !== OTP_LENGTH}
              onClick={handleVerify}
              style={{ fontWeight: 600 }}
            >
              Verify and set password
            </Button>
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
              Your password has been reset. Sign in with your new password.
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
