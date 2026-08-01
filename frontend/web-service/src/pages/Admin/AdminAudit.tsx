import React, { useEffect, useMemo, useState } from 'react';
import { Card, Flex, Segmented, Tag, Typography, theme } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { ADMIN_NAV } from '../../components/staffNavs';
import auditService, { type AuditTransaction } from '../../api/auditService';
import { DEMO_AUDIT_JOURNAL } from '../../mocks/demoStaff';

const { Text, Title } = Typography;

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

/** Read-only view of the immutable journal (FR-30). Nothing here can be edited. */
const AdminAudit: React.FC = () => {
  const { token } = theme.useToken();
  const [entries, setEntries] = useState<AuditTransaction[]>(DEMO_AUDIT_JOURNAL);
  const [filter, setFilter] = useState<'ALL' | 'FLAGGED'>('ALL');

  useEffect(() => {
    let cancelled = false;
    auditService
      .getTransactions()
      .then((data) => {
        if (!cancelled) setEntries(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const visible = useMemo(
    () => (filter === 'FLAGGED' ? entries.filter((entry) => entry.flagged) : entries),
    [entries, filter]
  );

  return (
    <StaffLayout portalName="Audit journal" roleLabel="ADMIN" navItems={ADMIN_NAV}>
      <Card size="small" style={{ marginBottom: 16 }}>
        <Flex align="center" gap={8}>
          <LockOutlined style={{ color: token.colorPrimary }} />
          <Text style={{ fontSize: 13, color: token.colorTextSecondary }}>
            Append-only journal. Entries can never be changed or deleted (FR-30).
          </Text>
        </Flex>
      </Card>

      <Segmented
        block
        value={filter}
        onChange={(value) => setFilter(value as 'ALL' | 'FLAGGED')}
        options={[
          { label: `All entries (${entries.length})`, value: 'ALL' },
          {
            label: `Flagged (${entries.filter((entry) => entry.flagged).length})`,
            value: 'FLAGGED',
          },
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
            No journal entries to show.
          </Text>
        )}
        {visible.map((entry, index) => (
          <div
            key={entry.id}
            style={{
              padding: '14px 16px',
              borderTop: index === 0 ? 'none' : `1px solid ${token.colorBorderSecondary}`,
            }}
          >
            <Flex justify="space-between" align="center" style={{ marginBottom: 4 }}>
              <Text className="font-mono" style={{ fontSize: 12, color: token.colorTextSecondary }}>
                {entry.journalId}
              </Text>
              {entry.flagged && <Tag color="red">Flagged</Tag>}
            </Flex>
            <Flex justify="space-between" align="center">
              <div style={{ minWidth: 0, paddingRight: 12 }}>
                <Title level={5} style={{ margin: 0, fontSize: 14 }}>
                  {entry.merchant}
                </Title>
                <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>
                  {entry.category} • {entry.location}
                </Text>
              </div>
              <Text
                className="font-mono"
                style={{
                  fontWeight: 600,
                  color: entry.amount < 0 ? token.colorText : '#1F7A6C',
                  whiteSpace: 'nowrap',
                }}
              >
                {formatAmount(entry.amount, entry.currency)}
              </Text>
            </Flex>
            <Text style={{ fontSize: 12, color: token.colorTextTertiary }}>
              {entry.dateGroupLabel}
            </Text>
          </div>
        ))}
      </div>
    </StaffLayout>
  );
};

export default AdminAudit;
