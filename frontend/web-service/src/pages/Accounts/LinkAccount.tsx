import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Typography, theme } from 'antd';
import { LeftOutlined, LockOutlined } from '@ant-design/icons';
import accountsService from '../../api/accountsService';
import { getApiErrorMessage } from '../../api/apiError';

const { Text, Title } = Typography;

interface LinkAccountFormValues {
  accountNumber: string;
  nationalIdOrPassport: string;
  nickname?: string;
}

const LinkAccount: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<LinkAccountFormValues>();
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFinish = async (values: LinkAccountFormValues) => {
    setSubmitting(true);
    setError(null);
    try {
      const challenge = await accountsService.requestAccountLink(values);
      navigate('/verify-otp', {
        state: {
          flow: 'account-link',
          challenge,
          successMessage: 'Account linked and selected',
          returnTo: '/accounts',
        },
      });
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'We could not verify this account. Check the details and try again.'
        )
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 28 }}>
          <Button
            type="text"
            shape="circle"
            icon={<LeftOutlined />}
            aria-label="Back to accounts"
            onClick={() => navigate('/accounts')}
          />
          <Title
            level={3}
            className="font-display"
            style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
          >
            Add bank account
          </Title>
        </Flex>

        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 20 }} />}

        <Card
          styles={{ body: { padding: 28 } }}
          style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
        >
          <Form<LinkAccountFormValues>
            form={form}
            layout="vertical"
            requiredMark={false}
            disabled={submitting}
            onFinish={handleFinish}
          >
            <Form.Item
              label="Bank account number"
              name="accountNumber"
              rules={[
                { required: true, message: 'Enter your bank account number' },
                { min: 10, message: 'Enter the full account number' },
              ]}
            >
              <Input
                size="large"
                inputMode="numeric"
                autoComplete="off"
                placeholder="Full account number"
              />
            </Form.Item>

            <Form.Item
              label="National ID or passport number"
              name="nationalIdOrPassport"
              rules={[{ required: true, message: 'Enter the identity number held by the bank' }]}
            >
              <Input size="large" autoComplete="off" placeholder="Identity document number" />
            </Form.Item>

            <Form.Item
              label="Account nickname (optional)"
              name="nickname"
              extra="Your own name for this account. Left empty, we keep the name the bank holds."
              rules={[{ max: 120, message: 'Keep the nickname under 120 characters' }]}
            >
              <Input size="large" autoComplete="off" placeholder="e.g. Salary account" />
            </Form.Item>

            <Button
              type="primary"
              size="large"
              block
              htmlType="submit"
              loading={submitting}
              style={{ height: 50, marginTop: 4, fontWeight: 600 }}
            >
              Verify account
            </Button>
          </Form>
        </Card>

        <Flex align="flex-start" gap={10} style={{ margin: '24px 4px 0' }}>
          <LockOutlined style={{ color: token.colorPrimary, marginTop: 3 }} />
          <Text style={{ color: token.colorTextSecondary, fontSize: 13, lineHeight: 1.6 }}>
            You confirm this with the current six digit code from your authenticator app.
          </Text>
        </Flex>
      </div>
    </div>
  );
};

export default LinkAccount;
