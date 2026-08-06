import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Empty,
  Flex,
  Form,
  InputNumber,
  Select,
  Spin,
  Steps,
  Tag,
  Typography,
  theme,
} from 'antd';
import { CheckCircleFilled, RightOutlined } from '@ant-design/icons';
import accountsService, { type Account } from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import lendingService, {
  type ApplicationStatus,
  type LoanApplicationResponse,
} from '../../api/lendingService';
import { getApiErrorMessage } from '../../api/apiError';
import BottomNav from '../../components/BottomNav';
import { currencyOf, formatMoney } from '../../utils/currency';

const { Text, Title } = Typography;

const TEAL_TINT = '#DCEFEA';

const WIZARD_STEPS = [
  { key: 'amount', title: 'Amount' },
  { key: 'submitted', title: 'Submitted' },
];

const PURPOSE_OPTIONS = [
  { value: 'personal', label: 'Personal expenses' },
  { value: 'business-equipment', label: 'Small business equipment' },
  { value: 'home-improvement', label: 'Home improvement' },
  { value: 'education', label: 'Education' },
  { value: 'debt-consolidation', label: 'Debt consolidation' },
];

const TERM_OPTIONS = [6, 12, 18, 24, 36, 48, 60].map((months) => ({
  value: months,
  label: `${months} months`,
}));

interface LoanAmountFormValues {
  purpose: string;
  amount: number;
  termMonths: number;
}

const fieldLabel = (text: string, color: string) => (
  <span style={{ fontWeight: 600, fontSize: 13, color }}>{text}</span>
);

/**
 * How each application state reads to the customer. DISBURSED is the end of a successful
 * application - the money is already in their account - so it says so rather than "disbursed".
 */
const STATUS_PRESENTATION: Record<
  ApplicationStatus,
  { label: string; color: string; detail: string }
> = {
  SUBMITTED: { label: 'Pending', color: 'gold', detail: 'Waiting to be picked up for review.' },
  UNDER_REVIEW: { label: 'Pending', color: 'gold', detail: 'A bank officer is reviewing this.' },
  APPROVED: { label: 'Approved', color: 'green', detail: 'Approved. Paying out to your account.' },
  DISBURSED: { label: 'Approved', color: 'green', detail: 'Paid into your account.' },
  REJECTED: { label: 'Rejected', color: 'red', detail: 'This application was declined.' },
};

const formatApplicationDate = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

const LoanApplication: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [form] = Form.useForm<LoanAmountFormValues>();
  const [currentStep, setCurrentStep] = useState(0);
  const [applicationId, setApplicationId] = useState<string | null>(null);
  const [linkedAccount, setLinkedAccount] = useState<Account | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [applications, setApplications] = useState<LoanApplicationResponse[]>([]);
  const [loadingApplications, setLoadingApplications] = useState(true);
  // Fixed by the account the loan is disbursed to; the customer only types the amount.
  const accountCurrency = currencyOf(linkedAccount);

  /** Re-read after applying, so a new application shows up in the history straight away. */
  const loadApplications = useCallback(() => {
    setLoadingApplications(true);
    return lendingService
      .listApplications()
      .then((data) => setApplications(data ?? []))
      .catch(() => setApplications([]))
      .finally(() => setLoadingApplications(false));
  }, []);

  useEffect(() => {
    void loadApplications();
  }, [loadApplications]);

  useEffect(() => {
    let cancelled = false;
    accountsService
      .getAccounts()
      .then((data) => {
        if (!cancelled && data.length > 0) {
          // The loan is always tied to the account the customer picked on the accounts page.
          const selected =
            data.find((account) => account.id === accountSelection.getSelectedAccountId()) ??
            data[0];
          accountSelection.setSelectedAccountId(selected.id);
          setLinkedAccount(selected);
        }
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleFinishAmountStep = async (values: LoanAmountFormValues) => {
    setSubmitting(true);
    setError(null);
    if (!linkedAccount) {
      setError('An active bank account is required to apply for a loan.');
      setSubmitting(false);
      return;
    }
    try {
      const response = await lendingService.applyForLoan({
        ...values,
        linkedAccountId: linkedAccount.id,
      });
      setApplicationId(response.id);
      setCurrentStep(1);
      void loadApplications();
    } catch (err) {
      setError(
        getApiErrorMessage(
          err,
          'Unable to start your loan application right now. Please try again.'
        )
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Title
          level={2}
          className="font-display"
          style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
        >
          Apply for a loan
        </Title>
        <Text style={{ display: 'block', marginTop: 8, color: token.colorTextSecondary }}>
          Personal and small-business loans with transparent terms. Decisions are logged to your
          audit trail.
        </Text>

        <Steps
          progressDot
          current={currentStep}
          style={{ marginTop: 28, marginBottom: 24 }}
          items={WIZARD_STEPS.map((step, index) => ({
            title: (
              <span
                style={{
                  color: index <= currentStep ? token.colorPrimary : token.colorTextTertiary,
                  fontWeight: 500,
                  fontSize: 14,
                }}
              >
                {step.title}
              </span>
            ),
          }))}
        />

        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        {currentStep === 0 && (
          <>
            <Card
              style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
              styles={{ body: { padding: 24 } }}
            >
              <Form<LoanAmountFormValues>
                form={form}
                layout="vertical"
                colon={false}
                requiredMark={false}
                disabled={submitting}
                initialValues={{
                  purpose: 'business-equipment',
                  amount: 500000,
                  termMonths: 24,
                }}
                onFinish={handleFinishAmountStep}
              >
                <Form.Item label={fieldLabel('Disburse to', token.colorText)}>
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
                    {linkedAccount?.nickname || 'Account'} -{' '}
                    {formatMoney(linkedAccount?.balance || 0, accountCurrency)}
                  </div>
                </Form.Item>

                <Form.Item
                  label={fieldLabel('Loan purpose', token.colorText)}
                  name="purpose"
                  rules={[{ required: true, message: 'Please select a loan purpose' }]}
                >
                  <Select size="large" options={PURPOSE_OPTIONS} />
                </Form.Item>

                <Form.Item
                  label={fieldLabel('Loan amount', token.colorText)}
                  name="amount"
                  rules={[
                    { required: true, message: 'Please enter a loan amount' },
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
                  label={fieldLabel('Repayment term', token.colorText)}
                  name="termMonths"
                  rules={[{ required: true, message: 'Please select a repayment term' }]}
                  style={{ marginBottom: 20 }}
                >
                  <Select size="large" options={TERM_OPTIONS} />
                </Form.Item>

                <Flex
                  align="center"
                  gap={10}
                  style={{
                    background: TEAL_TINT,
                    borderRadius: 12,
                    padding: '12px 16px',
                  }}
                >
                  <span
                    style={{
                      width: 6,
                      height: 6,
                      borderRadius: '50%',
                      background: token.colorPrimary,
                      flexShrink: 0,
                    }}
                  />
                  <Text style={{ color: token.colorPrimary, fontSize: 13, fontWeight: 500 }}>
                    Pre-approved up to LKR 750,000 · Estimated rate 11.5% p.a.
                  </Text>
                </Flex>
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
              Continue to details
            </Button>

            <Title
              level={5}
              className="font-display"
              style={{ marginTop: 36, marginBottom: 12, color: token.colorText, fontWeight: 600 }}
            >
              Your applications
            </Title>

            {loadingApplications ? (
              <Flex justify="center" style={{ padding: '24px 0' }}>
                <Spin />
              </Flex>
            ) : applications.length === 0 ? (
              <Card styles={{ body: { padding: 20 } }}>
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description={
                    <Text style={{ color: token.colorTextSecondary, fontSize: 13 }}>
                      You have not applied for a loan yet.
                    </Text>
                  }
                />
              </Card>
            ) : (
              <Flex vertical gap={12}>
                {applications.map((application) => {
                  const presentation = STATUS_PRESENTATION[application.status];
                  // Only a disbursed application has a loan to open a schedule for.
                  const openable = application.status === 'DISBURSED' && application.loanId;
                  return (
                    <Card
                      key={application.id}
                      styles={{ body: { padding: 16 } }}
                      style={{
                        cursor: openable ? 'pointer' : 'default',
                        boxShadow: '0 4px 12px rgba(11, 27, 43, 0.04)',
                      }}
                      onClick={
                        openable
                          ? () => navigate(`/loans/${application.loanId}/repayments`)
                          : undefined
                      }
                    >
                      <Flex justify="space-between" align="flex-start" gap={12}>
                        <div style={{ minWidth: 0 }}>
                          <Flex align="center" gap={8} style={{ marginBottom: 4 }}>
                            <Text style={{ fontWeight: 600, fontSize: 15 }}>
                              {formatMoney(application.amount, accountCurrency)}
                            </Text>
                            <Tag color={presentation.color} style={{ marginInlineEnd: 0 }}>
                              {presentation.label}
                            </Tag>
                          </Flex>
                          <Text
                            style={{
                              display: 'block',
                              fontSize: 12,
                              color: token.colorTextSecondary,
                            }}
                          >
                            {application.termMonths} months ·{' '}
                            {formatApplicationDate(application.createdAt)}
                          </Text>
                          <Text
                            style={{
                              display: 'block',
                              marginTop: 6,
                              fontSize: 12,
                              color: token.colorTextTertiary,
                            }}
                          >
                            {application.status === 'REJECTED' && application.rejectionReason
                              ? application.rejectionReason
                              : presentation.detail}
                          </Text>
                        </div>
                        {openable && (
                          <RightOutlined
                            style={{ color: token.colorTextTertiary, fontSize: 12, marginTop: 4 }}
                          />
                        )}
                      </Flex>
                    </Card>
                  );
                })}
              </Flex>
            )}
          </>
        )}

        {currentStep === 1 && (
          <Card
            style={{ boxShadow: '0 8px 24px rgba(11, 27, 43, 0.06)' }}
            styles={{ body: { padding: 24 } }}
          >
            <Flex vertical align="center" style={{ textAlign: 'center', padding: '8px 0' }}>
              <CheckCircleFilled
                style={{ fontSize: 40, color: token.colorPrimary, marginBottom: 16 }}
              />
              <Title
                level={4}
                className="font-display"
                style={{ margin: 0, color: token.colorText }}
              >
                Application submitted
              </Title>
              <Text
                style={{
                  display: 'block',
                  marginTop: 8,
                  color: token.colorTextSecondary,
                }}
              >
                Application {applicationId} is under review. We'll notify you once a decision has
                been made.
              </Text>
              <Button
                type="primary"
                block
                style={{ fontWeight: 600, marginTop: 24 }}
                onClick={() => navigate('/dashboard')}
              >
                Back to dashboard
              </Button>
            </Flex>
          </Card>
        )}
      </div>

      <BottomNav />
    </div>
  );
};

export default LoanApplication;
