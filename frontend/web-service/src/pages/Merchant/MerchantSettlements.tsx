import React, { useEffect, useState } from 'react';
import { Card, Flex, Tag, Typography, theme } from 'antd';
import { WalletOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { MERCHANT_NAV } from '../../components/staffNavs';
import paymentsService, { type MerchantSettlement } from '../../api/paymentsService';
import { DEMO_MERCHANT_SETTLEMENTS } from '../../mocks/demoStaff';

const { Text, Title } = Typography;

const formatAmount = (value: number, currency: string) =>
  new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
  }).format(value);

/** Weekly settlement payouts: what was collected, the fee, and what was paid out. */
const MerchantSettlements: React.FC = () => {
  const { token } = theme.useToken();
  const [settlements, setSettlements] = useState<MerchantSettlement[]>(DEMO_MERCHANT_SETTLEMENTS);

  useEffect(() => {
    let cancelled = false;
    paymentsService
      .getMerchantSettlements()
      .then((data) => {
        if (!cancelled) setSettlements(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const next = settlements.find((settlement) => settlement.status === 'SCHEDULED');

  return (
    <StaffLayout portalName="Settlements" roleLabel="MERCHANT" navItems={MERCHANT_NAV}>
      {next && (
        <Card size="small" style={{ marginBottom: 16, borderColor: token.colorPrimary }}>
          <Flex align="center" gap={8} style={{ marginBottom: 8 }}>
            <WalletOutlined style={{ color: token.colorPrimary, fontSize: 18 }} />
            <Title level={5} style={{ margin: 0 }}>
              Next payout
            </Title>
          </Flex>
          <Text className="font-mono" style={{ display: 'block', fontSize: 26, fontWeight: 600 }}>
            {formatAmount(next.netAmount, next.currency)}
          </Text>
          <Text style={{ fontSize: 13, color: token.colorTextSecondary }}>
            Arrives in your bank account on {next.payoutDate}.
          </Text>
        </Card>
      )}

      <Flex vertical gap={12}>
        {settlements.map((settlement) => (
          <Card key={settlement.id} size="small">
            <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
              <Text style={{ fontWeight: 600, fontSize: 14 }}>{settlement.periodLabel}</Text>
              <Tag
                color={settlement.status === 'PAID' ? 'green' : 'gold'}
                style={{ marginInlineEnd: 0 }}
              >
                {settlement.status === 'PAID' ? 'Paid' : 'Scheduled'}
              </Tag>
            </Flex>
            <Flex justify="space-between" style={{ marginBottom: 4 }}>
              <Text style={{ fontSize: 13, color: token.colorTextSecondary }}>Collected</Text>
              <Text className="font-mono" style={{ fontSize: 13 }}>
                {formatAmount(settlement.grossAmount, settlement.currency)}
              </Text>
            </Flex>
            <Flex justify="space-between" style={{ marginBottom: 4 }}>
              <Text style={{ fontSize: 13, color: token.colorTextSecondary }}>Service fee</Text>
              <Text className="font-mono" style={{ fontSize: 13 }}>
                −{formatAmount(settlement.fees, settlement.currency)}
              </Text>
            </Flex>
            <Flex
              justify="space-between"
              style={{
                paddingTop: 8,
                borderTop: `1px solid ${token.colorBorderSecondary}`,
              }}
            >
              <Text style={{ fontSize: 13, fontWeight: 600 }}>
                Paid out ({settlement.payoutDate})
              </Text>
              <Text className="font-mono" style={{ fontSize: 13, fontWeight: 600 }}>
                {formatAmount(settlement.netAmount, settlement.currency)}
              </Text>
            </Flex>
          </Card>
        ))}
      </Flex>
    </StaffLayout>
  );
};

export default MerchantSettlements;
