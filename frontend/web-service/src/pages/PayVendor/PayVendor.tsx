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
  Modal,
  Typography,
  theme,
} from 'antd';
import { SafetyCertificateOutlined } from '@ant-design/icons';
import type { AxiosError } from 'axios';
import accountsService, { type Account } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import paymentsService from '../../api/paymentsService';
import { currencyOf, formatMoney } from '../../utils/currency';
const { Text, Title } = Typography;

type BillerCategory = 'Electricity' | 'Water' | 'Internet' | 'Mobile';

const BILLER_CATEGORIES: BillerCategory[] = ['Electricity', 'Water', 'Internet', 'Mobile'];

/**
 * Only a hint for the biller field, so picking a category saves typing a well known name. There
 * is deliberately no suggested amount or reference: what a bill comes to is the customer's own
 * figure off their bill, and pre-filling it invites paying a number the bank made up.
 */
const BILLER_NAME_HINTS: Record<BillerCategory, string> = {
  Electricity: 'Ceylon Electricity Board',
  Water: 'National Water Supply & Drainage Board',
  Internet: 'SLT Fiber',
  Mobile: 'Dialog Axiata',
};

interface PayBillFormValues {
  biller: string;
  reference: string;
  amount: number;
}

const fieldLabel = (text: string, color: string) => (
  <span style={{ fontWeight: 600, fontSize: 13, color }}>{text}</span>
);

const PayVendor: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<PayBillFormValues>();
  const [fromAccount, setFromAccount] = useState<Account | null>(null);
  const [category, setCategory] = useState<BillerCategory>('Electricity');
  const [amount, setAmount] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [pendingPayment, setPendingPayment] = useState<PayBillFormValues | null>(null);
  const [totpCode, setTotpCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  // Fixed by the account being debited; the customer only types the amount.
  const accountCurrency = currencyOf(fromAccount);

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

  const applyCategory = (nextCategory: BillerCategory) => {
    setCategory(nextCategory);
    // Fills the biller name only; the reference and amount stay whatever the customer entered.
    form.setFieldValue('biller', BILLER_NAME_HINTS[nextCategory]);
  };

  const handleFinish = (values: PayBillFormValues) => {
    if (!fromAccount) {
      setError('An active account is required to pay a bill.');
      return;
    }
    setError(null);
    setTotpCode('');
    setPendingPayment(values);
  };

  const verifyAndPay = async () => {
    if (!fromAccount || !pendingPayment || totpCode.length !== 6) return;
    setSubmitting(true);
    setError(null);
    try {
      await paymentsService.payBill({
        billerCategory: category,
        billerName: pendingPayment.biller,
        referenceNumber: pendingPayment.reference,
        amount: pendingPayment.amount,
        fromAccountId: fromAccount.id,
        totpCode,
      });
      setPendingPayment(null);
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
            initialValues={{ biller: BILLER_NAME_HINTS.Electricity }}
            onValuesChange={(changed) => {
              if ('amount' in changed) {
                setAmount(typeof changed.amount === 'number' ? changed.amount : null);
              }
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
                addonBefore={accountCurrency}
                placeholder="0.00"
                precision={2}
              />
            </Form.Item>

            <Form.Item label={fieldLabel('Pay from', token.colorText)} style={{ marginBottom: 0 }}>
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
                {fromAccount?.nickname || 'Account'} -{' '}
                {formatMoney(fromAccount?.balance || 0, accountCurrency)}
              </div>
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
          {amount && amount > 0 ? `Pay ${formatMoney(amount, accountCurrency)}` : 'Pay bill'}
        </Button>

        <Modal
          open={pendingPayment !== null}
          title="Verify bill payment"
          footer={null}
          onCancel={() => !submitting && setPendingPayment(null)}
          destroyOnClose
        >
          <Alert
            type="warning"
            showIcon
            icon={<SafetyCertificateOutlined />}
            message="Authenticator code required"
            description="Your account will only be debited after this code is verified."
            style={{ marginBottom: 20 }}
          />
          {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
          <Flex vertical gap={16} align="center">
            <Text strong>
              {pendingPayment && formatMoney(pendingPayment.amount, accountCurrency)}
            </Text>
            <Input.OTP length={6} value={totpCode} onChange={setTotpCode} size="large" />
            <Button
              type="primary"
              size="large"
              block
              loading={submitting}
              disabled={totpCode.length !== 6}
              onClick={() => void verifyAndPay()}
            >
              Verify and pay bill
            </Button>
          </Flex>
        </Modal>
      </div>
    </div>
  );
};

export default PayVendor;
