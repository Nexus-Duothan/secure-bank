import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd';
import { LockFilled } from '@ant-design/icons';
import type { AxiosError } from 'axios';
import { authService } from '../../api/authService';

const { Title, Text, Link } = Typography;

interface LoginFormValues {
  usernameOrEmail: string;
  password: string;
}

const Login: React.FC = () => {
  const navigate = useNavigate();
  const { token } = theme.useToken();
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
    <div
      style={{
        minHeight: '100vh',
        background: token.colorBgLayout,
        display: 'flex',
        justifyContent: 'center',
        padding: '64px 24px',
      }}
    >
      <Flex vertical style={{ width: '100%', maxWidth: 440 }}>
        <Text
          className="font-display"
          style={{ fontSize: 22, fontWeight: 600, color: token.colorText }}
        >
          SecureBank
        </Text>

        <Title
          level={2}
          className="font-display"
          style={{ margin: '40px 0 32px', color: token.colorText, fontWeight: 500 }}
        >
          Welcome back
        </Title>

        <Card
          styles={{ body: { padding: 32 } }}
          style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
        >
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
                href="#"
                onClick={(e) => e.preventDefault()}
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

        <Flex align="center" gap={8} style={{ marginTop: 24 }}>
          <LockFilled style={{ color: token.colorPrimary }} />
          <Text style={{ color: token.colorPrimary, fontWeight: 500 }}>
            256-bit encrypted &bull; Identity verified
          </Text>
        </Flex>

        <Text style={{ textAlign: 'center', marginTop: 24, color: token.colorTextSecondary }}>
          New to SecureBank?{' '}
          <Link href="#" onClick={(e) => e.preventDefault()} style={{ color: token.colorPrimary }}>
            Create account
          </Link>
        </Text>
      </Flex>
    </div>
  );
};

export default Login;
