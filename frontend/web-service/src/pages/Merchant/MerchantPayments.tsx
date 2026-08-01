import React, { useEffect, useMemo, useState } from 'react';
import { Flex, Input, Segmented, Tag, Typography, theme } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { MERCHANT_NAV } from '../../components/staffNavs';
import paymentsService, {
  type MerchantPayment,
  type MerchantPaymentStatus,
} from '../../api/paymentsService';
import { DEMO_MERCHANT_PAYMENTS } from '../../mocks/demoStaff';

const { Text } = Typography;

const statusColor: Record<MerchantPaymentStatus, string> = {
  SETTLED: 'green',
  PENDING: 'gold',
  REFUNDED: 'default',
};

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

/**
 * View-only list of payments received. Refunds and disputes are handled in the
 * bank's merchant system; this app mirrors the money coming in.
 */
const MerchantPayments: React.FC = () => {
  const { token } = theme.useToken();
  const [payments, setPayments] = useState<MerchantPayment[]>(DEMO_MERCHANT_PAYMENTS);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | MerchantPaymentStatus>('ALL');

  useEffect(() => {
    let cancelled = false;
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

  const visible = useMemo(() => {
    const query = search.trim().toLowerCase();
    return payments.filter((payment) => {
      if (statusFilter !== 'ALL' && payment.status !== statusFilter) return false;
      if (!query) return true;
      return (
        payment.payerName.toLowerCase().includes(query) ||
        payment.reference.toLowerCase().includes(query)
      );
    });
  }, [payments, search, statusFilter]);

  return (
    <StaffLayout portalName="Payments in" roleLabel="MERCHANT" navItems={MERCHANT_NAV}>
      <Input
        prefix={<SearchOutlined style={{ color: token.colorTextTertiary }} />}
        placeholder="Search by payer or reference"
        allowClear
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        size="large"
        style={{ marginBottom: 12 }}
      />
      <Segmented
        block
        value={statusFilter}
        onChange={(value) => setStatusFilter(value as 'ALL' | MerchantPaymentStatus)}
        options={[
          { label: 'All', value: 'ALL' },
          { label: 'Settled', value: 'SETTLED' },
          { label: 'Pending', value: 'PENDING' },
          { label: 'Refunded', value: 'REFUNDED' },
        ]}
        style={{ marginBottom: 16 }}
      />

      <div
        style={{
          background: token.colorBgContainer,
          borderRadius: 16,
          border: `1px solid ${token.colorBorder}`,
          overflow: 'hidden',
        }}
      >
        {visible.length === 0 && (
          <Text
            style={{
              display: 'block',
              padding: 24,
              textAlign: 'center',
              color: token.colorTextSecondary,
            }}
          >
            No payments match this view.
          </Text>
        )}
        {visible.map((payment, index) => (
          <div
            key={payment.id}
            style={{
              padding: '14px 16px',
              borderTop: index === 0 ? 'none' : `1px solid ${token.colorBorderSecondary}`,
            }}
          >
            <Flex justify="space-between" align="center" style={{ marginBottom: 2 }}>
              <Text style={{ fontWeight: 600, fontSize: 14 }} ellipsis>
                {payment.payerName}
              </Text>
              <Text className="font-mono" style={{ fontWeight: 600, color: '#1F7A6C' }}>
                +{formatAmount(payment.amount, payment.currency)}
              </Text>
            </Flex>
            <Flex justify="space-between" align="center">
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>
                {payment.reference} • {payment.method} • {formatDateTime(payment.timestamp)}
              </Text>
              <Tag color={statusColor[payment.status]} style={{ marginInlineEnd: 0 }}>
                {payment.status}
              </Tag>
            </Flex>
          </div>
        ))}
      </div>
    </StaffLayout>
  );
};

export default MerchantPayments;
