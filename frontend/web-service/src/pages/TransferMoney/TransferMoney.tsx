import React, { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Alert, Button, Card, Flex, Form, Input, InputNumber, Typography, theme } from 'antd';
import accountsService, { type Account } from '../../api/accountsService';
import transferService, { type TransferResponse } from '../../api/transferService';
import { getApiErrorMessage } from '../../api/apiError';
import { DEMO_PRIMARY_ACCOUNT } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const MOCK_FROM_ACCOUNT: Account = DEMO_PRIMARY_ACCOUNT;

type Step = 'form' | 'review';

interface TransferFormValues {
  to: string;
  amount: number;
  note?: string;
}

const fieldLabel = (text: string, color: string) => (
  <span style={{ fontWeight: 600, fontSize: 13, color }}>{text}</span>
);

const formatCurrency = (currency: string, value: number) =>
  `${currency} ${new Intl.NumberFormat('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)}`;

const TransferMoney: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<TransferFormValues>();
  const [fromAccount, setFromAccount] = useState<Account>(MOCK_FROM_ACCOUNT);
  const [step, setStep] = useState<Step>('form');
  const [quote, setQuote] = useState<TransferResponse | null>(null);
  const [quoting, setQuoting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // Stable per-attempt key so a network retry of the same quote request dedupes on the backend
  // instead of creating a duplicate PENDING_CONFIRMATION row; reset whenever the user edits the form.
  const idempotencyKeyRef = useRef<string | null>(null);

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

  const handleQuote = async (values: TransferFormValues) => {
    setQuoting(true);
    setError(null);
    if (!idempotencyKeyRef.current) {
      idempotencyKeyRef.current = crypto.randomUUID();
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
      setError(getApiErrorMessage(err, 'Unable to start this transfer right now. Please try again.'));
    } finally {
      setQuoting(false);
    }
  };

  const handleConfirm = async () => {
    if (!quote) return;
    setConfirming(true);
    setError(null);
    try {
      const response = await transferService.confirmTransfer(quote.id);
      if (response.status !== 'COMPLETED') {
        setError(
          response.failureReason ?? 'This transfer could not be completed. Please try again.'
        );
        setQuote(response);
        return;
      }
      navigate('/dashboard');
    } catch (err) {
      setError(getApiErrorMessage(err, 'Unable to confirm this transfer right now. Please try again.'));
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
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
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
                    {fromAccount.nickname} - {formatCurrency(fromAccount.currency, fromAccount.balance)}
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
                    placeholder={`${fromAccount.currency} 0.00`}
                    formatter={(value) =>
                      value === undefined || value === null ? '' : `${fromAccount.currency} ${value}`
                    }
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
                  <SummaryRow label="From" value={fromAccount.nickname} token={token} />
                  <SummaryRow label="To" value={quote.toAccount} token={token} />
                  <SummaryRow
                    label="Amount"
                    value={formatCurrency(quote.currency, quote.amount)}
                    token={token}
                  />
                  <SummaryRow
                    label="Fee"
                    value={formatCurrency(quote.currency, quote.fee)}
                    token={token}
                  />
                  <SummaryRow
                    label="Total debit"
                    value={formatCurrency(quote.currency, quote.totalDebit)}
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
                  onClick={handleConfirm}
                >
                  Confirm transfer
                </Button>
              </Flex>
            </>
          )
        )}
      </div>
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
