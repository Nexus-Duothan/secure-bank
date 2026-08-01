import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Flex, Typography, theme } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import auditService, { type AuditTransaction } from '../../api/auditService';
import TransactionRow from '../../components/TransactionRow';
import { DEMO_AUDIT_TRANSACTIONS } from '../../mocks/demoCustomer';

const { Text, Title } = Typography;

type FilterKey = 'all' | 'in' | 'out' | 'flagged';

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'all', label: 'All' },
  { key: 'in', label: 'In' },
  { key: 'out', label: 'Out' },
  { key: 'flagged', label: 'Flagged' },
];

const MOCK_TRANSACTIONS: AuditTransaction[] = DEMO_AUDIT_TRANSACTIONS;

const groupByDate = (transactions: AuditTransaction[]) => {
  const groups: { label: string; items: AuditTransaction[] }[] = [];
  transactions.forEach((txn) => {
    const group = groups.find((item) => item.label === txn.dateGroupLabel);
    if (group) {
      group.items.push(txn);
    } else {
      groups.push({ label: txn.dateGroupLabel, items: [txn] });
    }
  });
  return groups;
};

const TransactionHistory: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [transactions, setTransactions] = useState<AuditTransaction[]>(MOCK_TRANSACTIONS);
  const [filter, setFilter] = useState<FilterKey>('all');

  useEffect(() => {
    let cancelled = false;
    auditService
      .getTransactions()
      .then((data) => {
        if (!cancelled) setTransactions(data);
      })
      .catch(() => {
        // Endpoint not available yet - fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filteredTransactions = useMemo(() => {
    switch (filter) {
      case 'in':
        return transactions.filter((txn) => txn.amount > 0);
      case 'out':
        return transactions.filter((txn) => txn.amount < 0);
      case 'flagged':
        return transactions.filter((txn) => txn.flagged);
      default:
        return transactions;
    }
  }, [transactions, filter]);

  const groups = useMemo(() => groupByDate(filteredTransactions), [filteredTransactions]);

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Flex align="center" gap={16} style={{ marginBottom: 24 }}>
          <LeftOutlined
            onClick={() => navigate('/dashboard')}
            style={{ fontSize: 20, color: token.colorText, cursor: 'pointer' }}
          />
          <Title
            level={3}
            className="font-display"
            style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
          >
            Previous activities
          </Title>
        </Flex>

        <Flex gap={10} style={{ marginBottom: 24, flexWrap: 'wrap' }}>
          {FILTERS.map((item) => {
            const isSelected = item.key === filter;
            return (
              <div
                key={item.key}
                onClick={() => setFilter(item.key)}
                style={{
                  cursor: 'pointer',
                  padding: '9px 18px',
                  borderRadius: 999,
                  fontSize: 14,
                  fontWeight: 500,
                  background: isSelected ? token.colorPrimary : token.colorBgContainer,
                  color: isSelected ? '#FFFFFF' : token.colorText,
                  border: isSelected ? 'none' : `1px solid ${token.colorBorder}`,
                }}
              >
                {item.label}
              </div>
            );
          })}
        </Flex>

        {groups.map((group) => (
          <div key={group.label} style={{ marginBottom: 20 }}>
            <Text
              style={{
                display: 'block',
                marginBottom: 10,
                fontSize: 12,
                fontWeight: 600,
                letterSpacing: 0.4,
                textTransform: 'uppercase',
                color: token.colorTextTertiary,
              }}
            >
              {group.label}
            </Text>

            <Flex vertical gap={12}>
              {group.items.map((txn) => (
                <div
                  key={txn.id}
                  style={{
                    background: token.colorBgContainer,
                    borderRadius: 16,
                    border: txn.flagged
                      ? `1px solid ${token.colorPrimary}`
                      : `1px solid ${token.colorBorder}`,
                  }}
                >
                  <TransactionRow
                    avatarLabel={txn.merchant.charAt(0).toUpperCase()}
                    merchant={txn.merchant}
                    category={txn.category}
                    date={txn.location || undefined}
                    amount={txn.amount}
                    currency={txn.currency}
                    verified={!txn.flagged}
                    flagged={txn.flagged}
                    journalId={txn.journalId}
                    showDivider={false}
                  />
                </div>
              ))}
            </Flex>
          </div>
        ))}

        {groups.length === 0 && (
          <Flex justify="center" style={{ padding: '40px 0' }}>
            <Text style={{ color: token.colorTextTertiary }}>No transactions to show.</Text>
          </Flex>
        )}
      </div>
    </div>
  );
};

export default TransactionHistory;
