import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Flex,
  Input,
  Modal,
  Segmented,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import { SafetyCertificateOutlined, SearchOutlined, UndoOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { MERCHANT_NAV } from '../../components/staffNavs';
import paymentsService, {
  type MerchantPayment,
  type MerchantPaymentStatus,
} from '../../api/paymentsService';
import { getApiErrorMessage } from '../../api/apiError';

const { Text } = Typography;

const statusColor: Record<MerchantPaymentStatus, string> = {
  COMPLETED: 'green',
  PENDING: 'gold',
  HELD_FOR_REVIEW: 'orange',
  DECLINED: 'red',
  REFUNDED: 'default',
};

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-LK', { style: 'currency', currency }).format(value);

const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

const MerchantPayments: React.FC = () => {
  const { token } = theme.useToken();
  const [messageApi, messageContext] = message.useMessage();
  const [payments, setPayments] = useState<MerchantPayment[]>([]);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<'ALL' | MerchantPaymentStatus>('ALL');
  const [selected, setSelected] = useState<MerchantPayment | null>(null);
  const [totpCode, setTotpCode] = useState('');
  const [refunding, setRefunding] = useState(false);

  useEffect(() => {
    let cancelled = false;
    paymentsService
      .getMerchantPayments()
      .then((data) => {
        if (!cancelled) setPayments(data || []);
      })
      .catch((error) => {
        if (!cancelled)
          messageApi.error(getApiErrorMessage(error, 'Payments could not be loaded.'));
      });
    return () => {
      cancelled = true;
    };
  }, [messageApi]);

  const visible = useMemo(() => {
    const query = search.trim().toLowerCase();
    return payments.filter((payment) => {
      if (statusFilter !== 'ALL' && payment.status !== statusFilter) return false;
      return (
        !query ||
        payment.payerUserId.toLowerCase().includes(query) ||
        payment.referenceNumber.toLowerCase().includes(query)
      );
    });
  }, [payments, search, statusFilter]);

  const refundPayment = async () => {
    if (!selected || totpCode.length !== 6) return;
    setRefunding(true);
    try {
      const updated = await paymentsService.refundMerchantPayment(selected.id, totpCode);
      setPayments((current) =>
        current.map((payment) => (payment.id === updated.id ? updated : payment))
      );
      setSelected(null);
      messageApi.success('The payment was refunded to the customer.');
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'The refund could not be completed.'));
    } finally {
      setRefunding(false);
    }
  };

  return (
    <StaffLayout portalName="Payments in" roleLabel="MERCHANT" navItems={MERCHANT_NAV}>
      {messageContext}
      <Input
        prefix={<SearchOutlined style={{ color: token.colorTextTertiary }} />}
        placeholder="Search by customer or reference"
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
          { label: 'Completed', value: 'COMPLETED' },
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
            <Flex justify="space-between" align="center" style={{ marginBottom: 4 }}>
              <Text style={{ fontWeight: 600, fontSize: 14 }}>
                Customer {payment.payerUserId.slice(0, 8)}
              </Text>
              <Text className="font-mono" style={{ fontWeight: 600, color: '#1F7A6C' }}>
                +{formatAmount(payment.amount, payment.currency)}
              </Text>
            </Flex>
            <Text style={{ display: 'block', fontSize: 12, color: token.colorTextSecondary }}>
              {payment.referenceNumber} · {payment.channel} · {formatDateTime(payment.createdAt)}
            </Text>
            <Flex justify="space-between" align="center" style={{ marginTop: 8 }}>
              <Tag color={statusColor[payment.status]}>{payment.status.replace(/_/g, ' ')}</Tag>
              {payment.status === 'COMPLETED' && (
                <Button
                  size="small"
                  icon={<UndoOutlined />}
                  onClick={() => {
                    setSelected(payment);
                    setTotpCode('');
                  }}
                >
                  Refund
                </Button>
              )}
            </Flex>
          </div>
        ))}
      </div>

      <Modal
        open={selected !== null}
        title="Refund customer payment"
        footer={null}
        onCancel={() => setSelected(null)}
        destroyOnClose
      >
        <Alert
          type="warning"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message="Authenticator verification required"
          description="The full payment will move from your settlement account back to the customer."
          style={{ marginBottom: 16 }}
        />
        <Flex vertical gap={12}>
          <Text strong>{selected && formatAmount(selected.amount, selected.currency)}</Text>
          <Input.OTP length={6} value={totpCode} onChange={setTotpCode} size="large" />
          <Button
            danger
            type="primary"
            block
            size="large"
            loading={refunding}
            disabled={totpCode.length !== 6}
            onClick={() => void refundPayment()}
          >
            Verify and refund
          </Button>
        </Flex>
      </Modal>
    </StaffLayout>
  );
};

export default MerchantPayments;
