import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Col, Flex, Row, Statistic, Typography, theme } from 'antd';
import { DollarOutlined, QrcodeOutlined, WalletOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { MERCHANT_NAV } from '../../components/staffNavs';
import paymentsService, {
  type MerchantPayment,
  type MerchantSummary,
} from '../../api/paymentsService';
import { DEMO_MERCHANT_PAYMENTS, DEMO_MERCHANT_SUMMARY } from '../../mocks/demoStaff';

const { Text, Title } = Typography;

const NAVY = '#0B1B2B';

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

const formatTime = (iso: string) =>
  new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });

/** Merchant business overview: today's takings and the latest payments in. */
const MerchantDashboard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [summary, setSummary] = useState<MerchantSummary>(DEMO_MERCHANT_SUMMARY);
  const [payments, setPayments] = useState<MerchantPayment[]>(DEMO_MERCHANT_PAYMENTS);

  useEffect(() => {
    let cancelled = false;

    paymentsService
      .getMerchantSummary()
      .then((data) => {
        if (!cancelled) setSummary(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    paymentsService
      .getMerchantPayments()
      .then((data) => {
        if (!cancelled) setPayments(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <StaffLayout portalName={summary.merchantName} roleLabel="MERCHANT" navItems={MERCHANT_NAV}>
      <div
        style={{
          background: NAVY,
          borderRadius: 20,
          padding: '24px 24px 28px',
          marginBottom: 20,
        }}
      >
        <Flex justify="space-between" align="center">
          <Text style={{ color: 'rgba(255,255,255,0.65)', fontSize: 13 }}>Today's takings</Text>
          <QrcodeOutlined style={{ color: '#8FE3D2', fontSize: 18 }} />
        </Flex>
        <Text
          className="font-mono"
          style={{
            display: 'block',
            color: '#FFFFFF',
            fontSize: 36,
            fontWeight: 600,
            margin: '12px 0 16px',
          }}
        >
          {formatAmount(summary.todayTotal, summary.currency)}
        </Text>
        <Flex justify="space-between">
          <Text style={{ color: '#3FD6B8', fontSize: 13, fontWeight: 500 }}>
            {summary.paymentsToday} payments in
          </Text>
          <Text style={{ color: 'rgba(255,255,255,0.55)', fontSize: 13 }}>
            {summary.refundsToday} refunds
          </Text>
        </Flex>
      </div>

      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col span={12}>
          <Card size="small" hoverable onClick={() => navigate('/merchant/settlements')}>
            <Statistic
              title="Waiting to be paid out"
              value={formatAmount(summary.pendingSettlement, summary.currency)}
              prefix={<WalletOutlined style={{ color: token.colorPrimary }} />}
              valueStyle={{ fontSize: 18 }}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small" hoverable onClick={() => navigate('/merchant/settlements')}>
            <Statistic
              title="Next payout"
              value={summary.nextPayoutDate}
              valueStyle={{ fontSize: 18 }}
            />
          </Card>
        </Col>
      </Row>

      <Flex justify="space-between" align="center" style={{ marginBottom: 12 }}>
        <Title level={4} className="font-display" style={{ margin: 0, fontWeight: 600 }}>
          Latest payments
        </Title>
        <Typography.Link
          onClick={() => navigate('/merchant/payments')}
          style={{ color: token.colorPrimary, fontWeight: 500 }}
        >
          View all
        </Typography.Link>
      </Flex>

      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 16,
          border: `1px solid ${token.colorBorder}`,
          overflow: 'hidden',
        }}
      >
        {payments.slice(0, 4).map((payment, index) => (
          <Flex
            key={payment.id}
            justify="space-between"
            align="center"
            style={{
              padding: '14px 16px',
              borderTop: index === 0 ? 'none' : `1px solid ${token.colorBorderSecondary}`,
            }}
          >
            <Flex align="center" gap={12} style={{ minWidth: 0 }}>
              <Flex
                align="center"
                justify="center"
                style={{
                  width: 38,
                  height: 38,
                  borderRadius: '50%',
                  background: '#DCEFEA',
                  color: token.colorPrimary,
                }}
              >
                <DollarOutlined />
              </Flex>
              <div style={{ minWidth: 0 }}>
                <Text style={{ display: 'block', fontWeight: 600, fontSize: 14 }} ellipsis>
                  {payment.payerName}
                </Text>
                <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>
                  {payment.method} • {formatTime(payment.timestamp)}
                </Text>
              </div>
            </Flex>
            <Text className="font-mono" style={{ fontWeight: 600, color: '#1F7A6C' }}>
              +{formatAmount(payment.amount, payment.currency)}
            </Text>
          </Flex>
        ))}
      </div>
    </StaffLayout>
  );
};

export default MerchantDashboard;
