import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import dayjs, { type Dayjs } from 'dayjs';
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Divider,
  Form,
  Input,
  Select,
  Space,
  Typography,
  theme,
} from 'antd';
import type { AxiosError } from 'axios';
import { authService } from '../../api/authService';
import AuthLayout from '../../components/AuthLayout';
import TrustIndicator from '../../components/TrustIndicator';
import { PasswordStrengthMeter } from '../../components/PasswordStrength';
import { PASSWORD_COMPLEXITY_MESSAGE, PASSWORD_PATTERN } from '../../components/passwordRules';

const { Text, Link } = Typography;

const COUNTRY_CODES = [
  { value: '+94', label: '+94 (LK)' },
  { value: '+1', label: '+1 (US)' },
  { value: '+44', label: '+44 (UK)' },
  { value: '+91', label: '+91 (IN)' },
  { value: '+61', label: '+61 (AU)' },
];

interface CreateAccountFormValues {
  fullName: string;
  dateOfBirth: Dayjs;
  email: string;
  countryCode: string;
  phoneNumber: string;
  address: string;
  nationalId: string;
  username: string;
  password: string;
  confirmPassword: string;
}

const SectionHeading: React.FC<{ title: string }> = ({ title }) => {
  const { token } = theme.useToken();
  return (
    <Text
      style={{
        display: 'block',
        fontSize: 13,
        fontWeight: 600,
        color: token.colorTextSecondary,
        marginBottom: 16,
      }}
    >
      {title}
    </Text>
  );
};

const CreateAccount: React.FC = () => {
  const navigate = useNavigate();
  const { token } = theme.useToken();
  const [form] = Form.useForm<CreateAccountFormValues>();
  const password = Form.useWatch('password', form) ?? '';
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFinish = async (values: CreateAccountFormValues) => {
    setSubmitting(true);
    setError(null);
    try {
      const response = await authService.createAccount({
        fullName: values.fullName,
        dateOfBirth: values.dateOfBirth.format('YYYY-MM-DD'),
        email: values.email,
        phoneNumber: `${values.countryCode}${values.phoneNumber}`,
        address: values.address,
        nationalIdOrPassport: values.nationalId,
        username: values.username,
        password: values.password,
      });
      navigate('/verify-otp', {
        state: {
          preAuthToken: response.userId,
          username: response.username,
          usernameOrEmail: values.email,
          email: values.email,
          registration: true,
        },
      });
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(
        axiosError.response?.data?.message ??
          'Unable to create your account. Please review your details and try again.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <AuthLayout heading="Create your account" maxWidth={480}>
      <Card
        styles={{ body: { padding: 32 } }}
        style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
      >
        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        <Form<CreateAccountFormValues>
          form={form}
          layout="vertical"
          colon={false}
          requiredMark={false}
          disabled={submitting}
          onFinish={handleFinish}
          initialValues={{ countryCode: '+94' }}
        >
          <SectionHeading title="Personal & Contact Information" />

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Full Name</span>}
            name="fullName"
            extra="Must match your government ID"
            rules={[{ required: true, message: 'Please enter your full name' }]}
          >
            <Input placeholder="Jane Doe" size="large" autoComplete="name" />
          </Form.Item>

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Date of Birth</span>}
            name="dateOfBirth"
            rules={[
              { required: true, message: 'Please select your date of birth' },
              {
                validator: (_, value?: Dayjs) => {
                  if (!value) return Promise.resolve();
                  return dayjs().diff(value, 'year') >= 18
                    ? Promise.resolve()
                    : Promise.reject(new Error('You must be at least 18 years old'));
                },
              },
            ]}
          >
            <DatePicker
              size="large"
              style={{ width: '100%' }}
              format="YYYY-MM-DD"
              disabledDate={(current) => !!current && current > dayjs().endOf('day')}
            />
          </Form.Item>

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

          <Form.Item label={<span style={{ fontWeight: 600 }}>Phone Number</span>} required>
            <Space.Compact style={{ width: '100%' }}>
              <Form.Item name="countryCode" noStyle>
                <Select size="large" style={{ width: 110 }} options={COUNTRY_CODES} />
              </Form.Item>
              <Form.Item
                name="phoneNumber"
                noStyle
                rules={[
                  { required: true, message: 'Phone number is required' },
                  { pattern: /^\d{6,12}$/, message: 'Enter a valid phone number' },
                ]}
              >
                <Input
                  placeholder="712345678"
                  size="large"
                  autoComplete="tel"
                  style={{ flex: 1 }}
                />
              </Form.Item>
            </Space.Compact>
          </Form.Item>

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Residential Address</span>}
            name="address"
            rules={[{ required: true, message: 'Please enter your residential address' }]}
          >
            <Input.TextArea placeholder="Street, city, postal code, country" rows={3} />
          </Form.Item>

          <Divider style={{ margin: '8px 0 24px' }} />
          <SectionHeading title="Identity Verification (KYC)" />

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>National ID / Passport Number</span>}
            name="nationalId"
            rules={[
              { required: true, message: 'Please enter your national ID or passport number' },
            ]}
          >
            <Input placeholder="e.g. 200012345678" size="large" />
          </Form.Item>

          <Divider style={{ margin: '8px 0 24px' }} />
          <SectionHeading title="Security Credentials" />

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Username / User ID</span>}
            name="username"
            rules={[
              { required: true, message: 'Please choose a username' },
              { min: 4, max: 50, message: 'Username must be between 4 and 50 characters' },
            ]}
          >
            <Input placeholder="janedoe92" size="large" autoComplete="off" />
          </Form.Item>

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Password</span>}
            name="password"
            style={{ marginBottom: 8 }}
            rules={[
              { required: true, message: 'Please create a password' },
              {
                pattern: PASSWORD_PATTERN,
                message: PASSWORD_COMPLEXITY_MESSAGE,
              },
            ]}
          >
            <Input.Password size="large" autoComplete="new-password" />
          </Form.Item>

          <PasswordStrengthMeter password={password} />

          <Form.Item
            label={<span style={{ fontWeight: 600 }}>Confirm Password</span>}
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: 'Please confirm your password' },
              ({ getFieldValue }) => ({
                validator: (_, value) =>
                  !value || getFieldValue('password') === value
                    ? Promise.resolve()
                    : Promise.reject(new Error('Passwords do not match')),
              }),
            ]}
          >
            <Input.Password size="large" autoComplete="new-password" />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, marginTop: 8 }}>
            <Button
              type="primary"
              htmlType="submit"
              block
              loading={submitting}
              style={{ fontWeight: 600 }}
            >
              Create account
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <TrustIndicator text="256-bit encrypted • Identity verified" />

      <Text style={{ textAlign: 'center', marginTop: 24, color: token.colorTextSecondary }}>
        Already have an account?{' '}
        <Link onClick={() => navigate('/login')} style={{ color: token.colorPrimary }}>
          Sign in
        </Link>
      </Text>
    </AuthLayout>
  );
};

export default CreateAccount;
