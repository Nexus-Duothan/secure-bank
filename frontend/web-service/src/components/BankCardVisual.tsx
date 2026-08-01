import React from 'react';
import { Flex, Typography } from 'antd';
import type { BankCard } from '../api/accountsService';

const { Text } = Typography;

interface BankCardVisualProps {
  card: BankCard;
  accountName: string;
}

const BankCardVisual: React.FC<BankCardVisualProps> = ({ card, accountName }) => {
  const credit = card.cardType === 'CREDIT';
  return (
    <div
      style={{
        background: credit ? '#1F554E' : '#0B1B2B',
        borderRadius: 18,
        padding: 22,
        aspectRatio: '1.586 / 1',
        minHeight: 220,
        color: '#FFFFFF',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        boxShadow: '0 16px 34px rgba(11, 27, 43, 0.14)',
      }}
    >
      <Flex justify="space-between" align="flex-start">
        <div>
          <Text
            className="font-display"
            style={{ color: '#FFFFFF', fontSize: 21, fontWeight: 600 }}
          >
            SecureBank
          </Text>
          <Text style={{ display: 'block', color: 'rgba(255,255,255,0.7)', marginTop: 3 }}>
            {card.productName}
          </Text>
        </div>
        <Text style={{ color: '#8FE3D2', fontSize: 12, fontWeight: 700 }}>{card.cardType}</Text>
      </Flex>

      <Text className="font-mono" style={{ color: '#FFFFFF', fontSize: 24, fontWeight: 500 }}>
        {card.maskedNumber}
      </Text>

      <Flex justify="space-between" align="end" gap={16}>
        <div style={{ minWidth: 0 }}>
          <Text style={{ color: 'rgba(255,255,255,0.55)', fontSize: 10 }}>CARDHOLDER</Text>
          <Text ellipsis style={{ display: 'block', color: '#FFFFFF', fontSize: 12 }}>
            {card.cardholderName}
          </Text>
          <Text style={{ display: 'block', color: '#8FE3D2', fontSize: 11, marginTop: 6 }}>
            {card.jointAccountCard ? 'Shared card' : accountName}
          </Text>
        </div>
        <div style={{ textAlign: 'right' }}>
          <Text style={{ color: 'rgba(255,255,255,0.55)', fontSize: 10 }}>EXPIRES</Text>
          <Text className="font-mono" style={{ display: 'block', color: '#FFFFFF' }}>
            {card.expiryDate}
          </Text>
          <Text style={{ display: 'block', color: '#FFFFFF', fontWeight: 700, marginTop: 6 }}>
            {card.scheme}
          </Text>
        </div>
      </Flex>
    </div>
  );
};

export default BankCardVisual;
