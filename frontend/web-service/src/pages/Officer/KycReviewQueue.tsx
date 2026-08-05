import React, { useEffect, useState } from 'react';
import { Card, Empty, Flex, Tag, Typography, theme } from 'antd';
import { IdcardOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import kycService, { type KycApplication } from '../../api/kycService';

const { Text, Title } = Typography;

const formatDate = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

/**
 * View-only KYC queue (FR-02). Officers see what is waiting, but the actual
 * approve/reject decisions are made in the bank's own core system — this app
 * only mirrors the queue.
 */
const KycReviewQueue: React.FC = () => {
  const { token } = theme.useToken();
  const [queue, setQueue] = useState<KycApplication[]>([]);

  useEffect(() => {
    let cancelled = false;
    kycService
      .getPendingApplications()
      .then((data) => {
        if (!cancelled) setQueue(data || []);
      })
      .catch(() => {
        if (!cancelled) setQueue([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <StaffLayout portalName="KYC queue" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {queue.length === 0 ? (
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
            </Card>
          ))}
        </Flex>
      )}
    </StaffLayout>
  );
};

export default KycReviewQueue;
