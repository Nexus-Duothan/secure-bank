import React from 'react';
import { Avatar, Flex, Typography, theme } from 'antd';
import { CheckCircleFilled, WarningFilled } from '@ant-design/icons';

const { Text } = Typography;

export interface TransactionRowProps {
  avatarLabel: string;
  merchant: string;
  category: string;
  date?: string;
  amount: number;
  currency?: string;
  verified?: boolean;
  flagged?: boolean;
  journalId?: string;
  showDivider?: boolean;
}

const formatAmount = (amount: number, currency: string) => {
  const formatted = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(Math.abs(amount));
  return `${amount < 0 ? '-' : '+'}${formatted}`;
};

const TransactionRow: React.FC<TransactionRowProps> = ({
  avatarLabel,
  merchant,
  category,
  date,
  amount,
  currency = 'USD',
  verified = false,
  flagged = false,
  journalId,
  showDivider = true,
}) => {
  const { token } = theme.useToken();
  const isPositive = amount >= 0;
  const showBadge = verified || flagged;
  const badgeIcon = flagged ? (
    <WarningFilled style={{ fontSize: 10, color: token.colorWarning }} />
  ) : (
    <CheckCircleFilled style={{ fontSize: 10, color: token.colorPrimary }} />
  );
  const badgeColor = flagged ? token.colorWarning : token.colorPrimary;
  const badgeLabel = flagged ? 'Held for review' : 'Verified';

  return (
    <div
      style={{
        padding: '16px 20px',
        borderBottom: showDivider ? `1px solid ${token.colorBorder}` : 'none',
      }}
    >
      <Flex align="center" justify="space-between">
        <Flex align="center" gap={14}>
          <Avatar
            size={44}
            style={{
              background: token.colorBgLayout,
              color: token.colorTextSecondary,
              fontWeight: 600,
            }}
          >
            {avatarLabel}
          </Avatar>
          <div>
            <Text
              className="font-display"
              style={{ display: 'block', fontSize: 16, fontWeight: 600, color: token.colorText }}
            >
              {merchant}
            </Text>
            <Text style={{ fontSize: 13, color: token.colorTextTertiary }}>
              {date ? `${category} · ${date}` : category}
            </Text>
          </div>
        </Flex>

        <Flex vertical align="end" gap={2}>
          <Text
            className="font-mono"
            style={{
              fontSize: 17,
              fontWeight: 600,
              color: isPositive ? token.colorPrimary : token.colorText,
            }}
          >
            {formatAmount(amount, currency)}
          </Text>
          {showBadge && !journalId && (
            <Flex align="center" gap={4}>
              {badgeIcon}
              <Text style={{ fontSize: 11, color: badgeColor }}>{badgeLabel}</Text>
            </Flex>
          )}
        </Flex>
      </Flex>

      {showBadge && journalId && (
        <Flex align="center" gap={4} style={{ marginTop: 8, marginLeft: 58 }}>
          {badgeIcon}
          <Text style={{ fontSize: 11, color: badgeColor }}>{badgeLabel}</Text>
          <Text style={{ fontSize: 11, color: token.colorTextTertiary }}>
            · Journal #{journalId}
          </Text>
        </Flex>
      )}
    </div>
  );
};

export default TransactionRow;
