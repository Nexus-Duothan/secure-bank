import React, { useEffect, useState } from 'react';
import { Card, Empty, Flex, Tag, Typography, theme } from 'antd';
import { FlagOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import auditService, { type AuditTransaction } from '../../api/auditService';

const { Text, Title } = Typography;

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

/**
 * View-only fraud holds (FR-31). Transactions held by the anomaly rules are
 * shown here; releasing or keeping a hold happens in the bank's core system.
 */
const FlaggedTransactions: React.FC = () => {
  const { token } = theme.useToken();
  const [flagged, setFlagged] = useState<AuditTransaction[]>([]);

  useEffect(() => {
    let cancelled = false;
    auditService
      .getTransactions()
      .then((data) => {
        if (!cancelled) setFlagged((data || []).filter((entry) => entry.flagged));
      })
      .catch(() => {
        if (!cancelled) setFlagged([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <StaffLayout portalName="Flagged transactions" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {flagged.length === 0 ? (
        <Card>
          <Empty description="No flagged transactions. All caught up." />
        </Card>
      ) : (
        <Flex vertical gap={12}>
          {flagged.map((entry) => (
            <Card key={entry.id} size="small">
              <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
                <Flex align="center" gap={8}>
                  <FlagOutlined style={{ color: token.colorError, fontSize: 16 }} />
                  <Text
                    className="font-mono"
                    style={{ fontSize: 12, color: token.colorTextSecondary }}
                  >
                    {entry.journalId}
                  </Text>
                </Flex>
                <Tag color="red">On hold</Tag>
              </Flex>
              <Title level={5} style={{ margin: '0 0 2px', fontSize: 14 }}>
                {entry.merchant}
              </Title>
              <Text
                className="font-mono"
                style={{ display: 'block', fontSize: 18, fontWeight: 600, marginBottom: 2 }}
              >
                {formatAmount(entry.amount, entry.currency)}
              </Text>
              <Text style={{ display: 'block', fontSize: 12, color: token.colorTextSecondary }}>
                {entry.category} • {entry.location} • {entry.dateGroupLabel}
              </Text>
            </Card>
          ))}
        </Flex>
      )}
    </StaffLayout>
  );
};

export default FlaggedTransactions;
