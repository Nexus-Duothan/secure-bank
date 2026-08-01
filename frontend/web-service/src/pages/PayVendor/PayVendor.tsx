import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Flex,
  Form,
  Input,
  InputNumber,
  Select,
  Typography,
  theme,
} from 'antd';
import type { AxiosError } from 'axios';
import accountsService, { type Account } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import paymentsService from '../../api/paymentsService';
import { DEMO_PRIMARY_ACCOUNT } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const MOCK_FROM_ACCOUNT: Account = DEMO_PRIMARY_ACCOUNT;

type BillerCategory = 'Electricity' | 'Water' | 'Internet' | 'Mobile';

const BILLER_CATEGORIES: BillerCategory[] = ['Electricity', 'Water', 'Internet', 'Mobile'];

const BILLER_PRESETS: Record<
  BillerCategory,
  { biller: string; reference: string; amount: number }
> = {
  Electricity: { biller: 'Ceylon Electricity Board', reference: '204 883 190', amount: 6120.0 },
  Water: {
    biller: 'National Water Supply & Drainage Board',
    reference: '118 402 771',
    amount: 2340.5,
  },
  Internet: { biller: 'SLT Fiber', reference: 'SLT-88213904', amount: 4500.0 },
  Mobile: { biller: 'Dialog Axiata', reference: '077 123 4567', amount: 1250.0 },
};

interface PayBillFormValues {
  biller: string;
  reference: string;
  amount: number;
  fromAccountId: string;
}

const fieldLabel = (text: string, color: string) => (
  <span style={{ fontWeight: 600, fontSize: 13, color }}>{text}</span>
);

const formatCurrency = (currency: string, value: number) =>
  `${currency} ${new Intl.NumberFormat('en-US', { minimumFractionDigits: 2 }).format(value)}`;

const PayVendor: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<PayBillFormValues>();
  const [fromAccount, setFromAccount] = useState<Account>(MOCK_FROM_ACCOUNT);
  const [accounts, setAccounts] = useState<Account[]>([MOCK_FROM_ACCOUNT]);
  const [category, setCategory] = useState<BillerCategory>('Electricity');
  const [amount, setAmount] = useState<number>(BILLER_PRESETS.Electricity.amount);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    accountsService
      .getAccounts()
      .then((data) => {
        if (!cancelled && data.length > 0) {
          const selected =
            data.find((account) => account.id === accountSelection.getSelectedAccountId()) ??
            data[0];
          accountSelection.setSelectedAccountId(selected.id);
          setAccounts(data);
          setFromAccount(selected);
        }
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    form.setFieldValue('fromAccountId', fromAccount.id);
  }, [fromAccount, form]);

  const applyCategory = (nextCategory: BillerCategory) => {
    setCategory(nextCategory);
    const preset = BILLER_PRESETS[nextCategory];
    form.setFieldsValue({
      biller: preset.biller,
      reference: preset.reference,
      amount: preset.amount,
    });
    setAmount(preset.amount);
  };

  const handleFinish = async (values: PayBillFormValues) => {
    setSubmitting(true);
    setError(null);
    try {
      await paymentsService.payBill({
        billerCategory: category,
        billerName: values.biller,
        referenceNumber: values.reference,
        amount: values.amount,
        fromAccountId: values.fromAccountId,
      });
      navigate('/dashboard');
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(
        axiosError.response?.data?.message ??
          'Unable to process this payment right now. Please try again.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  const fromAccountOptions = accounts.map((account) => ({
    value: account.id,
    label: `${account.nickname} - ${formatCurrency(account.currency, account.balance)}`,
  }));

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Title
          level={2}
          className="font-display"
          style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
        >
          Pay a bill
        </Title>
        <Text style={{ display: 'block', marginTop: 8, color: token.colorTextSecondary }}>
          Settle utility, merchant, or service payments securely.
        </Text>

        <Flex gap={10} style={{ marginTop: 24, flexWrap: 'wrap' }}>
          {BILLER_CATEGORIES.map((item) => {
            const isSelected = item === category;
            return (
              <div
                key={item}
                onClick={() => applyCategory(item)}
                style={{
                  cursor: 'pointer',
                  padding: '9px 18px',
                  borderRadius: 999,
                  fontSize: 14,
                  fontWeight: 500,
                  background: isSelected ? token.colorPrimary : token.colorBgContainer,
                  color: isSelected ? '#FFFFFF' : token.colorText,
                  border: isSelected ? 'none' : `1px solid ${token.colorBorder}`,
                }}
              >
                {item}
              </div>
            );
          })}
        </Flex>

        {error && <Alert type="error" message={error} showIcon style={{ marginTop: 24 }} />}

        <Card
          style={{ marginTop: 24, boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
          styles={{ body: { padding: 24 } }}
        >
          <Form<PayBillFormValues>
            form={form}
            layout="vertical"
            colon={false}
            requiredMark={false}
            disabled={submitting}
            initialValues={{
              biller: BILLER_PRESETS.Electricity.biller,
              reference: BILLER_PRESETS.Electricity.reference,
              amount: BILLER_PRESETS.Electricity.amount,
              fromAccountId: fromAccount.id,
            }}
            onValuesChange={(changed) => {
              if (typeof changed.amount === 'number') setAmount(changed.amount);
            }}
            onFinish={handleFinish}
          >
            <Form.Item
              label={fieldLabel('Biller', token.colorText)}
              name="biller"
              rules={[{ required: true, message: 'Please enter a biller' }]}
            >
              <Input size="large" placeholder="e.g. Ceylon Electricity Board" />
            </Form.Item>

            <Form.Item
              label={fieldLabel('Account / Reference number', token.colorText)}
              name="reference"
              rules={[{ required: true, message: 'Please enter an account or reference number' }]}
            >
              <Input size="large" placeholder="e.g. 204 883 190" />
            </Form.Item>

            <Form.Item
              label={fieldLabel('Amount due', token.colorText)}
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
              label={fieldLabel('Pay from', token.colorText)}
              name="fromAccountId"
              style={{ marginBottom: 0 }}
              rules={[{ required: true, message: 'Please select an account to pay from' }]}
            >
              <Select
                size="large"
                options={fromAccountOptions}
                onChange={(accountId) => {
                  const selected = accounts.find((account) => account.id === accountId);
                  if (selected) setFromAccount(selected);
                }}
              />
            </Form.Item>
          </Form>
        </Card>

        <Button
          type="primary"
          size="large"
          block
          loading={submitting}
          style={{ fontWeight: 600, height: 52, marginTop: 24 }}
          onClick={() => form.submit()}
        >
          Pay {formatCurrency(fromAccount.currency, amount || 0)}
        </Button>
      </div>
    </div>
  );
};

export default PayVendor;
