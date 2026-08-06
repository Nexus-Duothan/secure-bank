import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd';
import type { AxiosError } from 'axios';
import { authService } from '../../api/authService';
import AuthLayout from '../../components/AuthLayout';
import TrustIndicator from '../../components/TrustIndicator';

const { Text, Link } = Typography;

interface LoginFormValues {
  usernameOrEmail: string;
  password: string;
}

interface LoginLocationState {
  setupCompleteMessage?: string;
}

const Login: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { token } = theme.useToken();
  const routeState = (location.state as LoginLocationState | null) ?? null;
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFinish = async (values: LoginFormValues) => {
    setSubmitting(true);
    setError(null);
    try {
      const response = await authService.login(values);
      navigate('/verify-otp', {
        state: {
          preAuthToken: response.preAuthToken,
          usernameOrEmail: values.usernameOrEmail,
        },
      });
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(
        axiosError.response?.data?.message ??
          'Unable to sign in. Please check your credentials and try again.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout heading="Welcome back">
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {routeState?.setupCompleteMessage && (
          <Alert
            type="success"
            message={routeState.setupCompleteMessage}
            showIcon
            style={{ marginBottom: 24 }}
          />
        )}

        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        <Form<LoginFormValues>
          layout="vertical"
          colon={false}
          requiredMark={false}
          disabled={submitting}
          onFinish={handleFinish}
        >
          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Email</span>}
            name="usernameOrEmail"
            rules={[{ required: true, message: 'Please enter your email' }]}
          >
            <Input placeholder="you@email.com" size="large" autoComplete="username" />
          </Form.Item>

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Password</span>}
            name="password"
            rules={[{ required: true, message: 'Please enter your password' }]}
            style={{ marginBottom: 8 }}
          >
            <Input.Password size="large" autoComplete="current-password" />
          </Form.Item>

          <Flex justify="flex-end" style={{ marginBottom: 24 }}>
            <Link
              onClick={() => navigate('/forgot-password')}
              style={{ color: token.colorPrimary }}
            >
              Forgot password?
            </Link>
          </Flex>

          <Form.Item style={{ marginBottom: 0 }}>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={submitting}
              style={{ fontWeight: 600 }}
            >
              Sign in
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <TrustIndicator text="256-bit encrypted • Identity verified" />

      <Text style={{ textAlign: 'center', marginTop: 24, color: token.colorTextSecondary }}>
        New to SecureBank?{' '}
        <Link onClick={() => navigate('/signup')} style={{ color: token.colorPrimary }}>
          Create account
        </Link>
      </Text>
    </AuthLayout>
  );
};

export default Login;
