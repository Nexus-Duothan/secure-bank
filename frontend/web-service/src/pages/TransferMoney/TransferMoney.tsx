import React, { useEffect, useRef, useState } from 'react';
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
import accountsService, { type Account } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import transferService, { type TransferResponse } from '../../api/transferService';
import { getApiErrorMessage } from '../../api/apiError';
import BottomNav from '../../components/BottomNav';
import { currencyOf, formatMoney } from '../../utils/currency';
const { Text, Title } = Typography;

type Step = 'form' | 'review';

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
  const [fromAccount, setFromAccount] = useState<Account | null>(null);
  const [step, setStep] = useState<Step>('form');
  const [quote, setQuote] = useState<TransferResponse | null>(null);
  const [quoting, setQuoting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [totpOpen, setTotpOpen] = useState(false);
  const [totpCode, setTotpCode] = useState('');
  const [error, setError] = useState<string | null>(null);
  // Fixed by the account being debited; the customer only types the amount.
  const accountCurrency = currencyOf(fromAccount);
  // Stable per-attempt key so a network retry of the same quote request dedupes on the backend
  // instead of creating a duplicate PENDING_CONFIRMATION row; reset whenever the user edits the form.
  const idempotencyKeyRef = useRef<string | null>(null);

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

  const handleQuote = async (values: TransferFormValues) => {
    setQuoting(true);
    setError(null);
    if (!idempotencyKeyRef.current) {
      idempotencyKeyRef.current = crypto.randomUUID();
    }
    if (!fromAccount) {
      setError('An active account is required to make a transfer.');
      setQuoting(false);
      return;
    }
    try {
      const response = await transferService.quoteTransfer(
        {
          fromAccountId: fromAccount.id,
          toAccount: values.to,
          amount: values.amount,
          note: values.note,
        },
        idempotencyKeyRef.current
      );
      setQuote(response);
      setStep('review');
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Unable to start this transfer right now. Please try again.')
      );
    } finally {
      setQuoting(false);
    }
  };

  const requestConfirmation = () => {
    setError(null);
    setTotpCode('');
    setTotpOpen(true);
  };

  const handleConfirm = async () => {
    if (!quote || totpCode.length !== 6) return;
    setConfirming(true);
    setError(null);
    try {
      const response = await transferService.confirmTransfer(quote.id, totpCode);
      if (response.status !== 'COMPLETED') {
        setError(
          response.failureReason ?? 'This transfer could not be completed. Please try again.'
        );
        setQuote(response);
        return;
      }
      setTotpOpen(false);
      navigate('/dashboard');
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Unable to confirm this transfer right now. Please try again.')
      );
    } finally {
      setConfirming(false);
    }
  };

  const handleBack = () => {
    setStep('form');
    setQuote(null);
    setError(null);
    idempotencyKeyRef.current = null;
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Title
          level={2}
          className="font-display"
          style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
        >
          {step === 'form' ? 'Transfer money' : 'Review transfer'}
        </Title>
        <Text style={{ display: 'block', marginTop: 8, color: token.colorTextSecondary }}>
          {step === 'form'
            ? 'Send funds between your own accounts or to another SecureBank customer.'
            : 'Confirm the details below before we move the money.'}
        </Text>

        {error && <Alert type="error" message={error} showIcon style={{ marginTop: 24 }} />}

        {step === 'form' ? (
          <>
            <Card
              style={{ marginTop: 24, boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
              styles={{ body: { padding: 24 } }}
            >
              <Form<TransferFormValues>
                form={form}
                layout="vertical"
                colon={false}
                requiredMark={false}
                disabled={quoting}
                onFinish={handleQuote}
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
                    {fromAccount?.nickname || 'Account'} -{' '}
                    {formatMoney(fromAccount?.balance || 0, accountCurrency)}
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
                    addonBefore={accountCurrency}
                    placeholder="0.00"
                    precision={2}
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

            <Button
              type="primary"
              size="large"
              block
              loading={quoting}
              style={{ marginTop: 20, fontWeight: 600, height: 52 }}
              onClick={() => form.submit()}
            >
              Review transfer
            </Button>
          </>
        ) : (
          quote && (
            <>
              <Card
                style={{ marginTop: 24, boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
                styles={{ body: { padding: 24 } }}
              >
                <Flex vertical gap={16}>
                  <SummaryRow
                    label="From"
                    value={fromAccount?.nickname || 'Account'}
                    token={token}
                  />
                  <SummaryRow label="To" value={quote.toAccount} token={token} />
                  <SummaryRow
                    label="Amount"
                    value={formatMoney(quote.amount, quote.currency)}
                    token={token}
                  />
                  <SummaryRow
                    label="Fee"
                    value={formatMoney(quote.fee, quote.currency)}
                    token={token}
                  />
                  <SummaryRow
                    label="Total debit"
                    value={formatMoney(quote.totalDebit, quote.currency)}
                    token={token}
                    emphasize
                  />
                  {quote.note && <SummaryRow label="Note" value={quote.note} token={token} />}
                </Flex>
              </Card>

              <Flex gap={12} style={{ marginTop: 20 }}>
                <Button
                  size="large"
                  block
                  disabled={confirming}
                  style={{ fontWeight: 600, height: 52 }}
                  onClick={handleBack}
                >
                  Back
                </Button>
                <Button
                  type="primary"
                  size="large"
                  block
                  loading={confirming}
                  style={{ fontWeight: 600, height: 52 }}
                  onClick={requestConfirmation}
                >
                  Confirm transfer
                </Button>
              </Flex>
            </>
          )
        )}

        <Modal
          open={totpOpen}
          title="Verify this transfer"
          footer={null}
          onCancel={() => !confirming && setTotpOpen(false)}
          destroyOnClose
        >
          <Alert
            type="warning"
            showIcon
            icon={<SafetyCertificateOutlined />}
            message="Authenticator code required"
            description="No money will move until the current six-digit code is verified."
            style={{ marginBottom: 20 }}
          />
          {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 16 }} />}
          <Flex vertical gap={16} align="center">
            <Input.OTP length={6} value={totpCode} onChange={setTotpCode} size="large" />
            <Button
              type="primary"
              size="large"
              block
              loading={confirming}
              disabled={totpCode.length !== 6}
              onClick={() => void handleConfirm()}
            >
              Verify and transfer
            </Button>
          </Flex>
        </Modal>
      </div>

      <BottomNav />
    </div>
  );
};

interface SummaryRowProps {
  label: string;
  value: string;
  token: ReturnType<typeof theme.useToken>['token'];
  emphasize?: boolean;
}

const SummaryRow: React.FC<SummaryRowProps> = ({ label, value, token, emphasize }) => (
  <Flex justify="space-between" align="center">
    <Text style={{ color: token.colorTextSecondary, fontSize: 14 }}>{label}</Text>
    <Text
      style={{
        color: emphasize ? token.colorPrimary : token.colorText,
        fontSize: emphasize ? 16 : 14,
        fontWeight: emphasize ? 700 : 500,
      }}
    >
      {value}
    </Text>
  </Flex>
);

export default TransferMoney;
