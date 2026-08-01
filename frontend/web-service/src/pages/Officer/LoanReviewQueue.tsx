import React, { useEffect, useState } from 'react';
import { Card, Empty, Flex, Tag, Typography, theme } from 'antd';
import { PercentageOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import lendingService, { type PendingLoanApplication } from '../../api/lendingService';
import { DEMO_LOAN_QUEUE } from '../../mocks/demoStaff';

const { Text, Title } = Typography;

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

const formatDate = (iso: string) =>
  new Date(iso).toLocaleString('en-GB', {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
  });

/**
 * View-only loan queue (FR-22/FR-23). Officers see what is waiting; the actual
 * approve/reject decisions are made in the bank's own lending system.
 */
const LoanReviewQueue: React.FC = () => {
  const { token } = theme.useToken();
  const [queue, setQueue] = useState<PendingLoanApplication[]>(DEMO_LOAN_QUEUE);

  useEffect(() => {
    let cancelled = false;
    lendingService
      .getPendingApplications()
      .then((data) => {
        if (!cancelled) setQueue(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <StaffLayout portalName="Loan applications" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {queue.length === 0 ? (
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
                    {application.applicantName}
                  </Title>
                </Flex>
                <Tag color="processing">Under review</Tag>
              </Flex>
              <Text
                className="font-mono"
                style={{ display: 'block', fontSize: 20, fontWeight: 600, marginBottom: 4 }}
              >
                {formatAmount(application.amount, application.currency)}
              </Text>
              <Text style={{ display: 'block', fontSize: 13, marginBottom: 2 }}>
                {application.purpose} • {application.termMonths} months •{' '}
                {application.estimatedRate}% p.a.
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

export default LoanReviewQueue;
