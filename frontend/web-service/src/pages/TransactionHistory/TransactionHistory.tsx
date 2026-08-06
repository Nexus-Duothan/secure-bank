import { type Dayjs } from 'dayjs';
import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button, DatePicker, Flex, InputNumber, Select, Typography, theme } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import accountsService, {
  type AccountActivity,
  type TransactionDirection,
} from '../../api/accountsService';
import accountSelection from '../../api/accountSelection';
import TransactionRow from '../../components/TransactionRow';
const { Text, Title } = Typography;
const { RangePicker } = DatePicker;

type FilterKey = 'all' | 'in' | 'out';

const FILTERS: { key: FilterKey; label: string }[] = [
  { key: 'all', label: 'All' },
  { key: 'in', label: 'In' },
  { key: 'out', label: 'Out' },
];

const groupByDate = (transactions: AccountActivity[]) => {
  const groups: { label: string; items: AccountActivity[] }[] = [];
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

const mapDirection = (value: FilterKey): TransactionDirection =>
  value === 'all' ? 'ALL' : value === 'in' ? 'IN' : 'OUT';

const formatTypeLabel = (value: string) =>
  value
    .toLowerCase()
    .split('_')
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ');

const TransactionHistory: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [accountId, setAccountId] = useState(accountSelection.getSelectedAccountId());
  const [transactions, setTransactions] = useState<AccountActivity[]>([]);
  const [filter, setFilter] = useState<FilterKey>('all');
  const [flaggedOnly, setFlaggedOnly] = useState(false);
  const [selectedType, setSelectedType] = useState<string | undefined>();
  const [amountMin, setAmountMin] = useState<number | null>(null);
  const [amountMax, setAmountMax] = useState<number | null>(null);
  const [dateRange, setDateRange] = useState<[Dayjs | null, Dayjs | null] | null>(null);

  useEffect(() => {
    let cancelled = false;
    accountsService
      .getAccounts()
      .then((data) => {
        if (!cancelled && data.length > 0) {
          const selected =
            data.find((account) => account.id === accountSelection.getSelectedAccountId()) ??
            data[0];
          accountSelection.setSelectedAccountId(selected.id);
          setAccountId(selected.id);
        }
      })
      .catch(() => {
        // Leaves accountId unset, so the empty state below is shown.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    // Nothing to ask for until the customer has an account.
    if (!accountId) {
      setTransactions([]);
      return;
    }

    let cancelled = false;
    accountsService
      .getTransactionHistory(accountId, {
        direction: mapDirection(filter),
        dateFrom: dateRange?.[0]?.format('YYYY-MM-DD'),
        dateTo: dateRange?.[1]?.format('YYYY-MM-DD'),
        minAmount: amountMin ?? undefined,
        maxAmount: amountMax ?? undefined,
        type: selectedType,
        flaggedOnly,
      })
      .then((data) => {
        if (!cancelled) {
          setTransactions(data);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setTransactions([]);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [accountId, amountMax, amountMin, dateRange, filter, flaggedOnly, selectedType]);

  const typeOptions = useMemo(() => {
    const allItems = transactions;
    return Array.from(new Set(allItems.map((txn) => txn.transactionType)))
      .sort()
      .map((value) => ({ label: formatTypeLabel(value), value }));
  }, [transactions]);

  const groups = useMemo(() => groupByDate(transactions), [transactions]);

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

        <div
          style={{
            background: token.colorBgContainer,
            borderRadius: 16,
            border: `1px solid ${token.colorBorder}`,
            padding: 16,
            marginBottom: 24,
          }}
        >
          <Flex vertical gap={14}>
            <div>
              <Text
                style={{
                  display: 'block',
                  marginBottom: 8,
                  fontSize: 12,
                  fontWeight: 600,
                  letterSpacing: 0.4,
                  textTransform: 'uppercase',
                  color: token.colorTextTertiary,
                }}
              >
                Date range
              </Text>
              <RangePicker
                style={{ width: '100%' }}
                value={dateRange}
                onChange={(value) => setDateRange(value)}
                allowEmpty={[true, true]}
              />
            </div>

            <div>
              <Text
                style={{
                  display: 'block',
                  marginBottom: 8,
                  fontSize: 12,
                  fontWeight: 600,
                  letterSpacing: 0.4,
                  textTransform: 'uppercase',
                  color: token.colorTextTertiary,
                }}
              >
                Amount range
              </Text>
              <Flex gap={12}>
                <InputNumber<number>
                  style={{ flex: 1 }}
                  size="large"
                  min={0}
                  value={amountMin ?? undefined}
                  onChange={(value) => setAmountMin(value ?? null)}
                  placeholder="Min"
                  controls={false}
                />
                <InputNumber<number>
                  style={{ flex: 1 }}
                  size="large"
                  min={0}
                  value={amountMax ?? undefined}
                  onChange={(value) => setAmountMax(value ?? null)}
                  placeholder="Max"
                  controls={false}
                />
              </Flex>
            </div>

            <div>
              <Text
                style={{
                  display: 'block',
                  marginBottom: 8,
                  fontSize: 12,
                  fontWeight: 600,
                  letterSpacing: 0.4,
                  textTransform: 'uppercase',
                  color: token.colorTextTertiary,
                }}
              >
                Transaction type
              </Text>
              <Select
                allowClear
                size="large"
                placeholder="All transaction types"
                value={selectedType}
                options={typeOptions}
                onChange={(value) => setSelectedType(value)}
              />
            </div>

            <Flex justify="space-between" align="center" gap={12}>
              <Button
                onClick={() => setFlaggedOnly((value) => !value)}
                style={{
                  borderColor: flaggedOnly ? token.colorPrimary : token.colorBorder,
                  color: flaggedOnly ? token.colorPrimary : token.colorText,
                  fontWeight: 600,
                }}
              >
                {flaggedOnly ? 'Showing held items' : 'Held for review only'}
              </Button>
              <Button
                onClick={() => {
                  setFilter('all');
                  setFlaggedOnly(false);
                  setSelectedType(undefined);
                  setAmountMin(null);
                  setAmountMax(null);
                  setDateRange(null);
                }}
              >
                Clear filters
              </Button>
            </Flex>
          </Flex>
        </div>

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
