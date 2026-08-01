import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, Select, Typography, theme } from 'antd';
import { CreditCardOutlined, LeftOutlined } from '@ant-design/icons';
import accountsService, {
  type Account,
  type LinkCreditCardPayload,
} from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import { getApiErrorMessage } from '../../api/apiError';

const { Text, Title } = Typography;

const LinkCreditCard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<LinkCreditCardPayload>();
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    accountsService
      .getAccounts()
      .then((data) => {
        setAccounts(data);
        const selected =
          data.find((item) => item.id === accountSelection.getSelectedAccountId()) ?? data[0];
        if (selected) form.setFieldValue('accountId', selected.id);
      })
      .catch(() => setError('We could not load your linked accounts.'));
  }, [form]);

  const handleFinish = async (values: LinkCreditCardPayload) => {
    setSubmitting(true);
    setError(null);
    try {
      const challenge = await accountsService.requestCreditCardLink(values);
      navigate('/verify-otp', {
        state: {
          flow: 'credit-card-link',
          challenge,
          successMessage: 'Credit card linked successfully',
          returnTo: `/accounts/${values.accountId}`,
          accountId: values.accountId,
        },
      });
    } catch (err) {
      setError(getApiErrorMessage(err, 'We could not verify this credit card.'));
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
            onClick={() => navigate('/accounts')}
          />
          <Title level={3} className="font-display" style={{ margin: 0, fontWeight: 600 }}>
            Add credit card
          </Title>
        </Flex>

        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 20 }} />}

        <Card styles={{ body: { padding: 28 } }}>
          <Form<LinkCreditCardPayload>
            form={form}
            layout="vertical"
            requiredMark={false}
            disabled={submitting}
            onFinish={handleFinish}
          >
            <Form.Item
              label="Credit card number"
              name="cardNumber"
              rules={[{ required: true, message: 'Enter the full card number' }]}
            >
              <Input size="large" inputMode="numeric" maxLength={19} placeholder="Card number" />
            </Form.Item>
            <Flex gap={12}>
              <Form.Item
                label="Expiry"
                name="expiryDate"
                style={{ flex: 1 }}
                rules={[{ required: true, message: 'Enter MM/YY' }]}
              >
                <Input size="large" maxLength={5} placeholder="MM/YY" />
              </Form.Item>
              <Form.Item
                label="National ID"
                name="nationalIdOrPassport"
                style={{ flex: 2 }}
                rules={[{ required: true, message: 'Enter your ID' }]}
              >
                <Input size="large" placeholder="Identity number" />
              </Form.Item>
            </Flex>
            <Form.Item
              label="Associated account"
              name="accountId"
              rules={[{ required: true, message: 'Choose an account' }]}
            >
              <Select
                size="large"
                options={accounts.map((account) => ({
                  value: account.id,
                  label: account.nickname,
                }))}
              />
            </Form.Item>
            <Button type="primary" size="large" block htmlType="submit" loading={submitting}>
              Verify credit card
            </Button>
          </Form>
        </Card>

        <Flex gap={10} style={{ margin: '24px 4px 0' }}>
          <CreditCardOutlined style={{ color: token.colorPrimary, marginTop: 3 }} />
          <Text style={{ color: token.colorTextSecondary, fontSize: 13 }}>
            Only a SecureBank credit card held in your name can be linked.
          </Text>
        </Flex>
      </div>
    </div>
  );
};

export default LinkCreditCard;
