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
import { CheckOutlined, IdcardOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import kycService, { type KycApplication } from '../../api/kycService';
import { getApiErrorMessage } from '../../api/apiError';

const { Text, Title } = Typography;
const { TextArea } = Input;

const formatDate = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

const KycReviewQueue: React.FC = () => {
  const { token } = theme.useToken();
  const [messageApi, messageContext] = message.useMessage();
  const [queue, setQueue] = useState<KycApplication[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [selected, setSelected] = useState<KycApplication | null>(null);
  const [action, setAction] = useState<'APPROVED' | 'REJECTED'>('APPROVED');
  const [reason, setReason] = useState('');
  const [totpCode, setTotpCode] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const loadQueue = async () => {
    setLoading(true);
    setLoadError('');
    try {
      setQueue((await kycService.getPendingApplications()) || []);
    } catch (error) {
      setLoadError(getApiErrorMessage(error, 'Identity checks could not be loaded.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadQueue();
  }, []);

  const openDecision = (application: KycApplication, nextAction: 'APPROVED' | 'REJECTED') => {
    setSelected(application);
    setAction(nextAction);
    setReason('');
    setTotpCode('');
  };

  const submitDecision = async () => {
    if (!selected || totpCode.length !== 6 || (action === 'REJECTED' && !reason.trim())) return;
    setSubmitting(true);
    try {
      await kycService.reviewApplication(selected.applicationId, action, reason.trim(), totpCode);
      setQueue((current) =>
        current.filter((item) => item.applicationId !== selected.applicationId)
      );
      setSelected(null);
      messageApi.success(
        action === 'APPROVED'
          ? 'Identity verified and account activated.'
          : 'Identity check rejected.'
      );
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'The identity decision could not be saved.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <StaffLayout portalName="KYC queue" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {messageContext}
      {loadError && (
        <Alert
          type="error"
          showIcon
          message="Unable to load identity checks"
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
          <Empty description="No identity checks waiting. All caught up." />
        </Card>
      ) : (
        <Flex vertical gap={12}>
          {queue.map((application) => (
            <Card key={application.applicationId} size="small">
              <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
                <Flex align="center" gap={8}>
                  <IdcardOutlined style={{ color: token.colorPrimary, fontSize: 18 }} />
                  <Title level={5} style={{ margin: 0, fontSize: 14 }}>
                    {application.documentType === 'NATIONAL_ID' ? 'National ID' : 'Passport'}
                  </Title>
                </Flex>
                <Tag color="processing">Under review</Tag>
              </Flex>
              <Text
                className="font-mono"
                style={{ display: 'block', fontSize: 14, marginBottom: 4 }}
              >
                {application.documentNumber}
              </Text>
              <Text style={{ display: 'block', fontSize: 12, color: token.colorTextSecondary }}>
                Applicant ID: {application.userId}
              </Text>
              <Text style={{ display: 'block', fontSize: 12, color: token.colorTextTertiary }}>
                Submitted {formatDate(application.submittedAt)}
              </Text>
              <Flex gap={8} justify="flex-end" style={{ marginTop: 14 }}>
                <Button danger onClick={() => openDecision(application, 'REJECTED')}>
                  Reject
                </Button>
                <Button
                  type="primary"
                  icon={<CheckOutlined />}
                  onClick={() => openDecision(application, 'APPROVED')}
                >
                  Verify identity
                </Button>
              </Flex>
            </Card>
          ))}
        </Flex>
      )}

      <Modal
        open={selected !== null}
        title={action === 'APPROVED' ? 'Verify customer identity' : 'Reject identity check'}
        onCancel={() => setSelected(null)}
        footer={null}
        destroyOnClose
      >
        <Alert
          type="warning"
          showIcon
          icon={<SafetyCertificateOutlined />}
          message="Authenticator verification required"
          description="This decision activates or rejects the customer account and is security audited."
          style={{ marginBottom: 16 }}
        />
        <Flex vertical gap={12}>
          {action === 'REJECTED' && (
            <TextArea
              rows={3}
              value={reason}
              onChange={(event) => setReason(event.target.value)}
              placeholder="Reason for rejection"
            />
          )}
          <Input.OTP length={6} value={totpCode} onChange={setTotpCode} size="large" />
          <Button
            type="primary"
            danger={action === 'REJECTED'}
            block
            size="large"
            loading={submitting}
            disabled={totpCode.length !== 6 || (action === 'REJECTED' && !reason.trim())}
            onClick={() => void submitDecision()}
          >
            {action === 'APPROVED' ? 'Verify and activate' : 'Verify and reject'}
          </Button>
        </Flex>
      </Modal>
    </StaffLayout>
  );
};

export default KycReviewQueue;
