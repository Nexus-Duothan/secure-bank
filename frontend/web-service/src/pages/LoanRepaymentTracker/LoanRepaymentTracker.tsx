import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Button, Flex, Progress, Switch, Typography, theme } from 'antd';
import lendingService, { type LoanDetail } from '../../api/lendingService';
import { getApiErrorMessage } from '../../api/apiError';
import { buildDemoLoanDetail } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

const NAVY = '#0B1B2B';

const buildMockLoan = (id: string): LoanDetail => buildDemoLoanDetail(id);

const formatAmount = (currency: string, value: number) =>
  `${currency} ${new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)}`;

const formatDate = (isoDate?: string) => {
  if (!isoDate) return '-';
  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(isoDate));
};

const formatPurposeLabel = (purpose: string) =>
  `${purpose
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')} Loan`;

const LoanRepaymentTracker: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { token } = theme.useToken();
  const loanId = id ?? 'loan-demo-001';
  const [loan, setLoan] = useState<LoanDetail>(() => buildMockLoan(loanId));
  const [paying, setPaying] = useState(false);
  const [updatingAutopay, setUpdatingAutopay] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoan(buildMockLoan(loanId));

    lendingService
      .getLoanDetails(loanId)
      .then((data) => {
        if (!cancelled) setLoan(data);
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });

    return () => {
      cancelled = true;
    };
  }, [loanId]);

  const handlePayNow = async () => {
    setPaying(true);
    setError(null);
    try {
      const updated = await lendingService.payNow(loanId);
      setLoan(updated);
    } catch (err) {
      setError(
        getApiErrorMessage(err, 'Unable to process this payment right now. Please try again.')
      );
    } finally {
      setPaying(false);
    }
  };

  const handleAutopayChange = async (enabled: boolean) => {
    setUpdatingAutopay(true);
    setError(null);
    try {
      const updated = await lendingService.setAutopay(loanId, enabled);
      setLoan(updated);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Unable to update auto-pay right now. Please try again.'));
    } finally {
      setUpdatingAutopay(false);
    }
  };

  const installmentsPercent = Math.round((loan.installmentsPaid / loan.installmentsTotal) * 100);
  const fullyPaid = loan.status === 'PAID_OFF';

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Title
          level={3}
          className="font-display"
          style={{ margin: '0 0 24px', color: token.colorText, fontWeight: 600 }}
        >
          {formatPurposeLabel(loan.purpose)}
        </Title>

        {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 24 }} />}

        <div
          style={{
            background: NAVY,
            borderRadius: 20,
            padding: '24px 24px 20px',
            marginBottom: 24,
          }}
        >
          <Text style={{ fontSize: 14, color: 'rgba(255, 255, 255, 0.65)' }}>
            Remaining balance
          </Text>
          <Text
            className="font-mono"
            style={{
              display: 'block',
              color: '#FFFFFF',
              fontSize: 32,
              fontWeight: 600,
              margin: '10px 0 18px',
            }}
          >
            {formatAmount(loan.currency, loan.remainingBalance)}
          </Text>
          <Progress
            percent={installmentsPercent}
            showInfo={false}
            strokeColor={token.colorPrimary}
            trailColor="rgba(255, 255, 255, 0.18)"
            strokeLinecap="round"
          />
          <Text
            style={{
              display: 'block',
              marginTop: 10,
              fontSize: 13,
              color: 'rgba(255, 255, 255, 0.65)',
            }}
          >
            {loan.installmentsPaid} of {loan.installmentsTotal} installments paid
          </Text>
        </div>

        {!fullyPaid && (
          <Flex
            justify="space-between"
            align="center"
            style={{
              background: token.colorBgContainer,
              borderRadius: 20,
              border: `1px solid ${token.colorBorder}`,
              padding: '20px 24px',
              marginBottom: 24,
            }}
          >
            <div>
              <Text style={{ fontSize: 14, color: token.colorTextSecondary }}>
                Next payment due
              </Text>
              <Text
                style={{
                  display: 'block',
                  marginTop: 6,
                  fontSize: 16,
                  fontWeight: 600,
                  color: token.colorText,
                }}
              >
                {formatDate(loan.nextInstallmentDueDate)}
              </Text>
            </div>
            <Text
              className="font-mono"
              style={{ fontSize: 18, fontWeight: 600, color: token.colorText }}
            >
              {loan.nextInstallmentAmount != null
                ? formatAmount(loan.currency, loan.nextInstallmentAmount)
                : '-'}
            </Text>
          </Flex>
        )}

        <Flex justify="space-between" align="center" style={{ marginBottom: 24 }}>
          <Text style={{ fontSize: 15, color: token.colorText }}>Auto-pay from linked account</Text>
          <Switch
            checked={loan.autopayEnabled}
            loading={updatingAutopay}
            disabled={fullyPaid}
            onChange={handleAutopayChange}
          />
        </Flex>

        <Button
          type="primary"
          size="large"
          block
          loading={paying}
          disabled={fullyPaid}
          style={{ fontWeight: 600, height: 52 }}
          onClick={handlePayNow}
        >
          {fullyPaid ? 'Loan fully repaid' : 'Make a payment now'}
        </Button>
      </div>
    </div>
  );
};

export default LoanRepaymentTracker;
