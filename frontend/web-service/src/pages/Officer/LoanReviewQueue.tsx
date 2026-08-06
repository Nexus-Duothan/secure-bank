import React, { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Card,
  Empty,
  Flex,
  Input,
  Modal,
  Popconfirm,
  Spin,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import { CheckOutlined, PercentageOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import lendingService, { type PendingLoanApplication } from '../../api/lendingService';
import { getApiErrorMessage } from '../../api/apiError';

const { Text, Title } = Typography;
const { TextArea } = Input;

const formatAmount = (value: number, currency = 'LKR') =>
  new Intl.NumberFormat('en-LK', { style: 'currency', currency }).format(value);

const formatDate = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

const LoanReviewQueue: React.FC = () => {
  const { token } = theme.useToken();
  const [messageApi, messageContext] = message.useMessage();
  const [queue, setQueue] = useState<PendingLoanApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selected, setSelected] = useState<PendingLoanApplication | null>(null);
  const [approve, setApprove] = useState(true);
  const [note, setNote] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadQueue = async () => {
    setLoading(true);
    setLoadError('');
    try {
      setQueue((await lendingService.getPendingApplications()) || []);
    } catch (error) {
      setLoadError(getApiErrorMessage(error, 'Loan applications could not be loaded.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadQueue();
  }, []);

  const openDecision = (application: PendingLoanApplication, decision: boolean) => {
    setSelected(application);
    setApprove(decision);
    setNote('');
    setTotpCode('');
  };

  const submitDecision = async () => {
    if (!selected || totpCode.length !== 6 || (!approve && !note.trim())) return;
    setSubmitting(true);
    try {
      await lendingService.reviewApplication(selected.id, approve, note.trim(), totpCode);
      setQueue((current) => current.filter((item) => item.id !== selected.id));
      setSelected(null);
      messageApi.success(approve ? 'Loan approved and disbursed.' : 'Loan application rejected.');
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'The loan decision could not be saved.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StaffLayout portalName="Loan applications" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {messageContext}
      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Unable to load loan applications"
          description={loadError}
          action={<Button onClick={() => void loadQueue()}>Retry</Button>}
          style={{ marginBottom: 16 }}
        />
      )}

      {loading ? (
        <Flex justify="center" style={{ padding: 48 }}>
          <Spin size="large" />
        </Flex>
      ) : queue.length === 0 && !loadError ? (
        <Card>
          <Empty description="No loan applications waiting. All caught up." />
        </Card>
      ) : (
        <Flex vertical gap={12}>
          {queue.map((application) => (
            <Card key={application.id} size="small">
              <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
                <Flex align="center" gap={8}>
                  <PercentageOutlined style={{ color: token.colorPrimary, fontSize: 18 }} />
                  <Title level={5} style={{ margin: 0, fontSize: 14 }}>
                    {application.purpose.replace(/-/g, ' ')}
                  </Title>
                </Flex>
                <Tag color="processing">Under review</Tag>
              </Flex>
              <Text
                className="font-mono"
                style={{ display: 'block', fontSize: 20, fontWeight: 600 }}
              >
                {formatAmount(application.amount, application.currency)}
              </Text>
              <Text style={{ display: 'block', fontSize: 13 }}>
                {application.termMonths} months · {application.annualInterestRate}% p.a.
              </Text>
              <Text style={{ display: 'block', fontSize: 12, color: token.colorTextSecondary }}>
                Applicant {application.applicantUserId} · Account {application.linkedAccountId}
              </Text>
              <Text style={{ display: 'block', fontSize: 12, color: token.colorTextTertiary }}>
                Submitted {formatDate(application.createdAt)}
              </Text>
              <Flex gap={8} justify="flex-end" style={{ marginTop: 14 }}>
                <Button danger onClick={() => openDecision(application, false)}>
                  Reject
                </Button>
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => openDecision(application, true)}
                >
                  Approve
                </Button>
              </Flex>
            </Card>
          ))}
        </Flex>
      )}

      <Modal
        open={selected !== null}
        title={approve ? 'Approve and disburse loan' : 'Reject loan application'}
        onCancel={() => setSelected(null)}
        footer={null}
        destroyOnClose
      >
        <Alert
          type="warning"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message="Authenticator verification required"
          description="This decision changes a customer’s financial position and is recorded in the audit trail."
          style={{ marginBottom: 16 }}
        />
        <Flex vertical gap={12}>
          <TextArea
            rows={3}
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder={approve ? 'Decision note (optional)' : 'Reason for rejection'}
            status={!approve && selected && !note.trim() ? 'error' : undefined}
          />
          <Input.OTP length={6} value={totpCode} onChange={setTotpCode} size="large" />
          <Popconfirm
            title={approve ? 'Disburse this loan now?' : 'Reject this application?'}
            onConfirm={() => void submitDecision()}
            okText="Confirm decision"
          >
            <Button
              type="primary"
              danger={!approve}
              loading={submitting}
              disabled={totpCode.length !== 6 || (!approve && !note.trim())}
              block
              size="large"
            >
              {approve ? 'Verify and approve' : 'Verify and reject'}
            </Button>
          </Popconfirm>
        </Flex>
      </Modal>
    </StaffLayout>
  );
};

export default LoanReviewQueue;
