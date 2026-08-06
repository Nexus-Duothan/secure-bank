import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Flex,
  Input,
  Modal,
  Spin,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import { CheckOutlined, FlagOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import paymentsService, { type HeldPayment } from '../../api/paymentsService';
import { getApiErrorMessage } from '../../api/apiError';

const { Text, Title } = Typography;
const { TextArea } = Input;

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-LK', { style: 'currency', currency }).format(value);

const FlaggedTransactions: React.FC = () => {
  const { token } = theme.useToken();
  const [messageApi, messageContext] = message.useMessage();
  const [held, setHeld] = useState<HeldPayment[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selected, setSelected] = useState<HeldPayment | null>(null);
  const [approve, setApprove] = useState(true);
  const [note, setNote] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadHeld = async () => {
    setLoading(true);
    setLoadError('');
    try {
      setHeld((await paymentsService.getHeldPayments()) || []);
    } catch (error) {
      setLoadError(getApiErrorMessage(error, 'Held payments could not be loaded.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadHeld();
  }, []);

  const openDecision = (payment: HeldPayment, decision: boolean) => {
    setSelected(payment);
    setApprove(decision);
    setNote('');
    setTotpCode('');
  };

  const submitDecision = async () => {
    if (!selected || totpCode.length !== 6 || (!approve && !note.trim())) return;
    setSubmitting(true);
    try {
      await paymentsService.reviewHeldPayment(selected.id, approve, note.trim(), totpCode);
      setHeld((current) => current.filter((payment) => payment.id !== selected.id));
      setSelected(null);
      messageApi.success(approve ? 'Payment released successfully.' : 'Payment declined.');
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'The payment decision could not be saved.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StaffLayout portalName="Flagged transactions" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {messageContext}
      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Unable to load held payments"
          description={loadError}
          action={<Button onClick={() => void loadHeld()}>Retry</Button>}
          style={{ marginBottom: 16 }}
        />
      )}
      {loading ? (
        <Flex justify="center" style={{ padding: 48 }}>
          <Spin size="large" />
        </Flex>
      ) : held.length === 0 && !loadError ? (
        <Card>
          <Empty description="No payments are waiting for review." />
        </Card>
      ) : (
        <Flex vertical gap={12}>
          {held.map((payment) => (
            <Card key={payment.id} size="small">
              <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
                <Flex align="center" gap={8}>
                  <FlagOutlined style={{ color: token.colorError, fontSize: 16 }} />
                  <Text
                    className="font-mono"
                    style={{ fontSize: 12, color: token.colorTextSecondary }}
                  >
                    {payment.referenceNumber}
                  </Text>
                </Flex>
                <Tag color="red">On hold</Tag>
              </Flex>
              <Title level={5} style={{ margin: '0 0 2px', fontSize: 14 }}>
                {payment.merchantName}
              </Title>
              <Text
                className="font-mono"
                style={{ display: 'block', fontSize: 18, fontWeight: 600 }}
              >
                {formatAmount(payment.amount, payment.currency)}
              </Text>
              <Text style={{ display: 'block', fontSize: 12, color: token.colorTextSecondary }}>
                {payment.channel} · Payer {payment.payerUserId}
              </Text>
              <Flex gap={8} justify="flex-end" style={{ marginTop: 14 }}>
                <Button danger onClick={() => openDecision(payment, false)}>
                  Decline
                </Button>
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => openDecision(payment, true)}
                >
                  Release payment
                </Button>
              </Flex>
            </Card>
          ))}
        </Flex>
      )}

      <Modal
        open={selected !== null}
        title={approve ? 'Release held payment' : 'Decline held payment'}
        onCancel={() => setSelected(null)}
        footer={null}
        destroyOnClose
      >
        <Alert
          type="warning"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message="Authenticator verification required"
          description="Releasing or declining held funds is a high-security action."
          style={{ marginBottom: 16 }}
        />
        <Flex vertical gap={12}>
          <TextArea
            rows={3}
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder={approve ? 'Review note (optional)' : 'Reason for declining the payment'}
          />
          <Input.OTP length={6} value={totpCode} onChange={setTotpCode} size="large" />
          <Button
            type="primary"
            danger={!approve}
            block
            size="large"
            loading={submitting}
            disabled={totpCode.length !== 6 || (!approve && !note.trim())}
            onClick={() => void submitDecision()}
          >
            {approve ? 'Verify and release' : 'Verify and decline'}
          </Button>
        </Flex>
      </Modal>
    </StaffLayout>
  );
};

export default FlaggedTransactions;
