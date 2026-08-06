import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Alert, Button, Card, Empty, Flex, Progress, Spin, Switch, Typography, theme } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import lendingService, { type LoanDetail } from '../../api/lendingService';
import { getApiErrorMessage } from '../../api/apiError';

const { Text, Title } = Typography;
const NAVY = '#0B1B2B';

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
  const navigate = useNavigate();
  const { token } = theme.useToken();
  const loanId = id ?? '';
  const [loan, setLoan] = useState<LoanDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [updatingAutopay, setUpdatingAutopay] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!loanId) {
      setLoading(false);
      return;
    }
    let cancelled = false;

    lendingService
      .getLoanDetails(loanId)
      .then((data) => {
        if (!cancelled) {
          setLoan(data);
          setError(null);
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(getApiErrorMessage(err, 'Failed to load loan details'));
          setLoan(null);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [loanId]);

  const handleMakePayment = async () => {
    if (!loan) return;
    setPaying(true);
    setError(null);
    try {
      const updated = await lendingService.payNow(loan.id);
      setLoan(updated);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Repayment failed'));
    } finally {
      setPaying(false);
    }
  };

  const handleToggleAutopay = async (checked: boolean) => {
    if (!loan) return;
    setUpdatingAutopay(true);
    setError(null);
    try {
      const updated = await lendingService.setAutopay(loan.id, checked);
      setLoan(updated);
    } catch (err) {
      setError(getApiErrorMessage(err, 'Autopay update failed'));
    } finally {
      setUpdatingAutopay(false);
    }
  };

  if (loading) {
    return (
      <Flex
        justify="center"
        align="center"
        style={{ minHeight: '100vh', background: token.colorBgLayout }}
      >
        <Spin size="large" />
      </Flex>
    );
  }

  if (!loan) {
    return (
      <div style={{ minHeight: '100vh', background: token.colorBgLayout, padding: '32px 20px' }}>
        <Button
          icon={<LeftOutlined />}
          onClick={() => navigate('/dashboard')}
          style={{ marginBottom: 20 }}
        >
          Back to Dashboard
        </Button>
        <Empty description={error || 'Loan not found'} />
      </div>
    );
  }

  const progressPercent = Math.round(
    ((loan.principal - loan.remainingBalance) / (loan.principal || 1)) * 100
  );

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 112px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 24 }}>
          <Button shape="circle" icon={<LeftOutlined />} onClick={() => navigate('/dashboard')} />
          <Title level={4} style={{ margin: 0 }}>
            Loan Tracker
          </Title>
        </Flex>

        {error && <Alert type="error" message={error} style={{ marginBottom: 20 }} showIcon />}

        <Card style={{ background: NAVY, borderRadius: 20, color: '#FFF', marginBottom: 24 }}>
          <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 14 }}>
            {formatPurposeLabel(loan.purpose)}
          </Text>
          <Title level={2} style={{ color: '#FFF', margin: '8px 0 16px' }}>
            {formatAmount(loan.currency, loan.remainingBalance)}
          </Title>
          <Text style={{ color: 'rgba(255,255,255,0.6)', fontSize: 13 }}>
            Remaining of {formatAmount(loan.currency, loan.principal)}
          </Text>
          <Progress percent={progressPercent} strokeColor="#3FD6B8" style={{ marginTop: 16 }} />
        </Card>

        <Card style={{ borderRadius: 16, marginBottom: 24 }}>
          <Flex justify="space-between" align="center" style={{ marginBottom: 16 }}>
            <div>
              <Text type="secondary" style={{ display: 'block', fontSize: 13 }}>
                Next Payment Due
              </Text>
              <Text style={{ fontSize: 16, fontWeight: 600 }}>
                {formatDate(loan.nextInstallmentDueDate)}
              </Text>
            </div>
            <Text style={{ fontSize: 18, fontWeight: 600, color: token.colorPrimary }}>
              {formatAmount(loan.currency, loan.nextInstallmentAmount || 0)}
            </Text>
          </Flex>

          <Button
            type="primary"
            block
            size="large"
            loading={paying}
            onClick={handleMakePayment}
            style={{ borderRadius: 12, fontWeight: 600, height: 48 }}
          >
            Pay Next Installment
          </Button>

          <Flex justify="space-between" align="center" style={{ marginTop: 20 }}>
            <Text style={{ fontWeight: 500 }}>Auto-debit from primary account</Text>
            <Switch
              checked={loan.autopayEnabled}
              loading={updatingAutopay}
              onChange={handleToggleAutopay}
            />
          </Flex>
        </Card>
      </div>
    </div>
  );
};

export default LoanRepaymentTracker;
