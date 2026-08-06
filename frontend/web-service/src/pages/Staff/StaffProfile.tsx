import React, { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Avatar,
  Button,
  Card,
  Flex,
  Form,
  Input,
  Modal,
  Switch,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import { EditOutlined, KeyOutlined, SafetyOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { ADMIN_NAV, MERCHANT_NAV, OFFICER_NAV } from '../../components/staffNavs';
import type { StaffNavItem } from '../../components/StaffLayout';
import userService from '../../api/userService';
import authService from '../../api/authService';
import sessionUser, { homePathForRole } from '../../api/sessionUser';
import { getApiErrorMessage } from '../../api/apiError';
import { PasswordStrengthMeter } from '../../components/PasswordStrength';
import { PASSWORD_COMPLEXITY_MESSAGE, PASSWORD_PATTERN } from '../../components/passwordRules';
import type { NotificationPreferences, Role, UserProfile } from '../../types';

const { Text, Title } = Typography;

const PORTAL_META: Record<Exclude<Role, 'CUSTOMER'>, { nav: StaffNavItem[]; roleLabel: string }> = {
  ADMIN: { nav: ADMIN_NAV, roleLabel: 'ADMIN' },
  BANK_OFFICER: { nav: OFFICER_NAV, roleLabel: 'BANK OFFICER' },
  MERCHANT: { nav: MERCHANT_NAV, roleLabel: 'MERCHANT' },
};

interface PersonalDetailsFormValues {
  fullName: string;
  email: string;
  phoneNumber: string;
}

interface PasswordFormValues {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const getInitials = (name: string) =>
  name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

const DEFAULT_PREFS: NotificationPreferences = {
  email: true,
  sms: true,
  push: true,
};

const DEFAULT_STAFF_PROFILE: UserProfile = {
  id: '',
  fullName: 'Bank Staff',
  email: '',
  phoneNumber: '',
  addressLine: '',
  city: '',
  country: '',
  language: 'English',
  role: 'ADMIN',
  status: 'ACTIVE',
  idVerified: true,
  notificationPreferences: DEFAULT_PREFS,
  linkedDevices: [],
};

const StaffProfile: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const [messageApi, messageContext] = message.useMessage();
  const [form] = Form.useForm<PersonalDetailsFormValues>();

  const session = sessionUser.get();
  const role: Exclude<Role, 'CUSTOMER'> =
    session && session.role !== 'CUSTOMER' ? session.role : 'ADMIN';
  const meta = PORTAL_META[role];
  const profilePath = `${homePathForRole(role)}/profile`;

  const [profile, setProfile] = useState<UserProfile>(DEFAULT_STAFF_PROFILE);
  const [editOpen, setEditOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [prefs, setPrefs] = useState<NotificationPreferences>(DEFAULT_PREFS);
  const [savingPrefs, setSavingPrefs] = useState(false);
  const [passwordSubmitting, setPasswordSubmitting] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [passwordForm] = Form.useForm<PasswordFormValues>();
  const newPassword = Form.useWatch('newPassword', passwordForm) ?? '';

  useEffect(() => {
    const state = location.state as { otpSuccessMessage?: string } | null;
    if (state?.otpSuccessMessage) {
      messageApi.success(state.otpSuccessMessage);
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location, messageApi, navigate]);

  useEffect(() => {
    let cancelled = false;
    userService
      .getProfile()
      .then((data) => {
        if (!cancelled) {
          setProfile(data);
          setPrefs(data.notificationPreferences);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setProfile(DEFAULT_STAFF_PROFILE);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const prefsChanged =
    prefs.email !== profile.notificationPreferences.email ||
    prefs.sms !== profile.notificationPreferences.sms ||
    prefs.push !== profile.notificationPreferences.push;

  const handleEditSubmit = async (values: PersonalDetailsFormValues) => {
    setSubmitting(true);
    try {
      const challenge = await userService.requestProfileUpdate(values);
      navigate('/verify-otp', {
        state: {
          flow: 'profile-change',
          challenge,
          successMessage: 'Your details were updated.',
          returnTo: profilePath,
        },
      });
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'Could not start this change.'));
    } finally {
      setSubmitting(false);
    }
  };

  const handleSavePrefs = async () => {
    setSavingPrefs(true);
    try {
      const challenge = await userService.requestNotificationPreferencesUpdate(prefs);
      navigate('/verify-otp', {
        state: {
          flow: 'profile-change',
          challenge,
          successMessage: 'Your notification alerts were updated.',
          returnTo: profilePath,
        },
      });
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'Could not start this change.'));
    } finally {
      setSavingPrefs(false);
    }
  };

  /**
   * Stages the new password behind the authenticator step. The password only changes once the code
   * is confirmed, which also ends every session and sends the user back to sign-in.
   */
  const handlePasswordChange = async (values: PasswordFormValues) => {
    setPasswordSubmitting(true);
    try {
      const challenge = await authService.requestPasswordChange({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      passwordForm.resetFields();
      setPasswordOpen(false);
      navigate('/verify-otp', { state: { flow: 'password-change', challenge } });
    } catch (error) {
      messageApi.error(
        getApiErrorMessage(error, 'We could not start the password change. Please try again.')
      );
    } finally {
      setPasswordSubmitting(false);
    }
  };

  return (
    <StaffLayout portalName="My profile" roleLabel={meta.roleLabel} navItems={meta.nav}>
      {messageContext}

      <Card size="small" style={{ marginBottom: 16 }}>
        <Flex align="center" gap={14}>
          <Avatar size={56} style={{ background: '#0B1B2B', fontWeight: 600 }}>
            {getInitials(profile.fullName)}
          </Avatar>
          <div style={{ minWidth: 0 }}>
            <Title level={5} style={{ margin: 0 }}>
              {profile.fullName}
            </Title>
            <Text style={{ fontSize: 12, color: token.colorTextSecondary }} ellipsis>
              {profile.email}
            </Text>
            <div style={{ marginTop: 6 }}>
              <Tag color="geekblue" style={{ fontWeight: 600 }}>
                {meta.roleLabel}
              </Tag>
              <Tag color="green">{profile.status.replace('_', ' ')}</Tag>
            </div>
          </div>
        </Flex>
      </Card>

      <Alert
        type="info"
        showIcon
        icon={<SafetyOutlined />}
        style={{ marginBottom: 16 }}
        message="Protected profile"
        description="Any change to your name, email, mobile number, or alerts must be confirmed with the current code from your authenticator app."
      />

      <Card
        size="small"
        style={{ marginBottom: 16 }}
        title="Personal details"
        extra={
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => {
              form.setFieldsValue({
                fullName: profile.fullName,
                email: profile.email,
                phoneNumber: profile.phoneNumber,
              });
              setEditOpen(true);
            }}
          >
            Edit
          </Button>
        }
      >
        <Flex vertical gap={10}>
          <div>
            <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Full name</Text>
            <Text style={{ display: 'block', fontWeight: 500 }}>{profile.fullName}</Text>
          </div>
          <div>
            <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Email</Text>
            <Text style={{ display: 'block', fontWeight: 500 }}>{profile.email}</Text>
          </div>
          <div>
            <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Mobile number</Text>
            <Text style={{ display: 'block', fontWeight: 500 }}>{profile.phoneNumber}</Text>
          </div>
        </Flex>
      </Card>

      <Card size="small" style={{ marginBottom: 16 }} title="Notification alerts">
        <Flex vertical gap={12}>
          <Flex justify="space-between" align="center">
            <Text>Email alerts</Text>
            <Switch
              checked={prefs.email}
              onChange={(checked) => setPrefs((current) => ({ ...current, email: checked }))}
            />
          </Flex>
          <Flex justify="space-between" align="center">
            <Text>SMS alerts</Text>
            <Switch
              checked={prefs.sms}
              onChange={(checked) => setPrefs((current) => ({ ...current, sms: checked }))}
            />
          </Flex>
          <Flex justify="space-between" align="center">
            <Text>Push alerts</Text>
            <Switch
              checked={prefs.push}
              onChange={(checked) => setPrefs((current) => ({ ...current, push: checked }))}
            />
          </Flex>
          <Button
            type="primary"
            block
            loading={savingPrefs}
            disabled={!prefsChanged}
            onClick={handleSavePrefs}
            style={{ fontWeight: 600 }}
          >
            Save alert settings
          </Button>
        </Flex>
      </Card>

      <Card size="small" title="Security">
        <Flex vertical gap={8}>
          <Text style={{ fontSize: 13, color: token.colorTextSecondary }}>
            A password change needs your current password and the code from your authenticator app.
            You then sign in again with the new password.
          </Text>
          <Button icon={<KeyOutlined />} onClick={() => setPasswordOpen(true)} block>
            Change password
          </Button>
        </Flex>
      </Card>

      <Modal
        open={editOpen}
        title="Edit personal details"
        okText="Continue to verification"
        confirmLoading={submitting}
        onOk={() => form.submit()}
        onCancel={() => setEditOpen(false)}
      >
        <Text
          style={{
            display: 'block',
            marginBottom: 16,
            fontSize: 13,
            color: token.colorTextSecondary,
          }}
        >
          After you continue, enter the current code from your authenticator app to confirm this
          change.
        </Text>
        <Form<PersonalDetailsFormValues>
          form={form}
          layout="vertical"
          colon={false}
          requiredMark={false}
          disabled={submitting}
          onFinish={handleEditSubmit}
        >
          <Form.Item
            label="Full name"
            name="fullName"
            rules={[{ required: true, message: 'Please enter your full name' }]}
          >
            <Input size="large" />
          </Form.Item>
          <Form.Item
            label="Email"
            name="email"
            rules={[
              { required: true, message: 'Please enter your email' },
              { type: 'email', message: 'Please enter a valid email' },
            ]}
          >
            <Input size="large" />
          </Form.Item>
          <Form.Item
            label="Mobile number"
            name="phoneNumber"
            rules={[{ required: true, message: 'Please enter your mobile number' }]}
          >
            <Input size="large" />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={passwordOpen}
        title="Change password"
        okText="Save"
        confirmLoading={passwordSubmitting}
        onOk={() => passwordForm.submit()}
        onCancel={() => {
          setPasswordOpen(false);
          passwordForm.resetFields();
        }}
      >
        <Text
          style={{
            display: 'block',
            marginBottom: 16,
            fontSize: 13,
            color: token.colorTextSecondary,
          }}
        >
          After you save, enter the current code from your authenticator app. You then sign in again
          with the new password.
        </Text>
        <Form<PasswordFormValues>
          form={passwordForm}
          layout="vertical"
          colon={false}
          requiredMark={false}
          disabled={passwordSubmitting}
          onFinish={handlePasswordChange}
        >
          <Form.Item
            label="Current password"
            name="currentPassword"
            rules={[{ required: true, message: 'Enter your current password' }]}
          >
            <Input.Password size="large" autoComplete="current-password" />
          </Form.Item>
          <Form.Item
            label="New password"
            name="newPassword"
            style={{ marginBottom: 8 }}
            rules={[
              { required: true, message: 'Create a new password' },
              { pattern: PASSWORD_PATTERN, message: PASSWORD_COMPLEXITY_MESSAGE },
            ]}
          >
            <Input.Password size="large" autoComplete="new-password" />
          </Form.Item>
          <PasswordStrengthMeter password={newPassword} />
          <Form.Item
            label="Confirm new password"
            name="confirmPassword"
            dependencies={['newPassword']}
            rules={[
              { required: true, message: 'Confirm your new password' },
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
        </Form>
      </Modal>
    </StaffLayout>
  );
};

export default StaffProfile;
