import React, { useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Flex,
  Form,
  InputNumber,
  Select,
  Steps,
  Typography,
  theme,
} from 'antd';
import type { AxiosError } from 'axios';
import lendingService from '../../api/lendingService';

const { Text, Title } = Typography;

const TEAL_TINT = '#DCEFEA';

const WIZARD_STEPS = [
  { key: 'amount', title: 'Amount' },
  { key: 'details', title: 'Details' },
  { key: 'review', title: 'Review' },
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

const LoanApplication: React.FC = () => {
  const { token } = theme.useToken();
  const [form] = Form.useForm<LoanAmountFormValues>();
  const [currentStep, setCurrentStep] = useState(0);
  const [applicationId, setApplicationId] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleFinishAmountStep = async (values: LoanAmountFormValues) => {
    setSubmitting(true);
    setError(null);
    try {
      const response = await lendingService.applyForLoan(values);
      setApplicationId(response.id);
      setCurrentStep(1);
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      setError(
        axiosError.response?.data?.message ??
          'Unable to start your loan application right now. Please try again.'
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
                    placeholder="LKR 0.00"
                    formatter={(value) =>
                      value === undefined || value === null ? '' : `LKR ${value}`
                    }
                    parser={(value) => {
                      const numeric = Number((value ?? '').replace(/[^0-9.]/g, ''));
                      return Number.isNaN(numeric) ? 0 : numeric;
                    }}
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
          </>
        )}

        {currentStep === 1 && (
          <Card styles={{ body: { padding: 24 } }}>
            <Title level={4} className="font-display" style={{ margin: 0, color: token.colorText }}>
              Details
            </Title>
            <Text style={{ display: 'block', marginTop: 8, color: token.colorTextSecondary }}>
              Application {applicationId} started. The employment and personal details step will go
              here.
            </Text>
            <Button
              type="text"
              style={{ marginTop: 16, paddingLeft: 0 }}
              onClick={() => setCurrentStep(0)}
            >
              Back to amount
            </Button>
          </Card>
        )}

        {currentStep === 2 && (
          <Card styles={{ body: { padding: 24 } }}>
            <Title level={4} className="font-display" style={{ margin: 0, color: token.colorText }}>
              Review
            </Title>
            <Text style={{ display: 'block', marginTop: 8, color: token.colorTextSecondary }}>
              The review and confirmation step will go here.
            </Text>
            <Button
              type="text"
              style={{ marginTop: 16, paddingLeft: 0 }}
              onClick={() => setCurrentStep(1)}
            >
              Back to details
            </Button>
          </Card>
        )}
      </div>
    </div>
  );
};

export default LoanApplication;
