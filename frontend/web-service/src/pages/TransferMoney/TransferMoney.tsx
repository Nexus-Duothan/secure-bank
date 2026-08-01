import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, InputNumber, Typography, theme } from 'antd';
import type { AxiosError } from 'axios';
import accountsService, { type Account } from '../../api/accountsService';
import transferService from '../../api/transferService';
import { DEMO_PRIMARY_ACCOUNT } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const MOCK_FROM_ACCOUNT: Account = DEMO_PRIMARY_ACCOUNT;

interface TransferFormValues {
  to: string;
  amount: number;
  note?: string;
}

const fieldLabel = (text: string, color: string) => (
  <span style={{ fontWeight: 600, fontSize: 13, color }}>{text}</span>
);

const TransferMoney: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<TransferFormValues>();
  const [fromAccount, setFromAccount] = useState<Account>(MOCK_FROM_ACCOUNT);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    accountsService
      .getPrimaryAccount()
      .then((data) => {
        if (!cancelled) setFromAccount(data);
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleFinish = async (values: TransferFormValues) => {
    setSubmitting(true);
    setError(null);
    try {
      await transferService.createTransfer({
        fromAccountId: fromAccount.id,
        toAccount: values.to,
        amount: values.amount,
        note: values.note,
      });
      navigate('/dashboard');
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(
        axiosError.response?.data?.message ??
          'Unable to start this transfer right now. Please try again.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Title
          level={2}
          className="font-display"
          style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
        >
          Transfer money
        </Title>
        <Text style={{ display: 'block', marginTop: 8, color: token.colorTextSecondary }}>
          Send funds between your own accounts or to another SecureBank customer.
        </Text>

        {error && <Alert type="error" message={error} showIcon style={{ marginTop: 24 }} />}

        <Card
          style={{
            marginTop: 24,
            boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)',
          }}
          styles={{ body: { padding: 24 } }}
        >
          <Form<TransferFormValues>
            form={form}
            layout="vertical"
            colon={false}
            requiredMark={false}
            disabled={submitting}
            onFinish={handleFinish}
          >
            <Form.Item label={fieldLabel('From', token.colorText)} style={{ marginBottom: 20 }}>
              <div
                style={{
                  height: 44,
                  display: 'flex',
                  alignItems: 'center',
                  padding: '0 12px',
                  borderRadius: 8,
                  border: `1px solid ${token.colorBorder}`,
                  color: token.colorTextTertiary,
                }}
              >
                {fromAccount.nickname} - {fromAccount.currency}{' '}
                {new Intl.NumberFormat('en-US', { minimumFractionDigits: 2 }).format(
                  fromAccount.balance
                )}
              </div>
            </Form.Item>

            <Form.Item
              label={fieldLabel('To', token.colorText)}
              name="to"
              rules={[{ required: true, message: 'Please select or enter a recipient' }]}
            >
              <Input size="large" placeholder="Select recipient or enter account" />
            </Form.Item>

            <Form.Item
              label={fieldLabel('Amount', token.colorText)}
              name="amount"
              rules={[
                { required: true, message: 'Please enter an amount' },
                { type: 'number', min: 0.01, message: 'Amount must be greater than 0' },
              ]}
            >
              <InputNumber<number>
                size="large"
                style={{ width: '100%' }}
                controls={false}
                min={0}
                placeholder="LKR 0.00"
                formatter={(value) => (value === undefined || value === null ? '' : `LKR ${value}`)}
                parser={(value) => {
                  const numeric = Number((value ?? '').replace(/[^0-9.]/g, ''));
                  return Number.isNaN(numeric) ? 0 : numeric;
                }}
              />
            </Form.Item>

            <Form.Item
              label={fieldLabel('Note (optional)', token.colorText)}
              name="note"
              style={{ marginBottom: 0 }}
            >
              <Input size="large" placeholder="e.g. Rent for July" />
            </Form.Item>
          </Form>
        </Card>

        <Flex justify="space-between" align="center" style={{ margin: '20px 4px' }}>
          <Text style={{ color: token.colorTextSecondary, fontSize: 14 }}>Transfer fee</Text>
          <Text style={{ color: token.colorPrimary, fontSize: 14, fontWeight: 500 }}>
            {fromAccount.currency} 0.00 - Instant
          </Text>
        </Flex>

        <Button
          type="primary"
          size="large"
          block
          loading={submitting}
          style={{ fontWeight: 600, height: 52 }}
          onClick={() => form.submit()}
        >
          Review transfer
        </Button>
      </div>
    </div>
  );
};

export default TransferMoney;
