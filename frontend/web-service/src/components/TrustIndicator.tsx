import React from 'react';
import { Flex, Typography, theme } from 'antd';
import { LockFilled } from '@ant-design/icons';

interface TrustIndicatorProps {
  text: string;
}

const TrustIndicator: React.FC<TrustIndicatorProps> = ({ text }) => {
  const { token } = theme.useToken();

  return (
    <Flex align="center" gap={8} style={{ marginTop: 24 }}>
      <LockFilled style={{ color: token.colorPrimary }} />
      <Typography.Text style={{ color: token.colorPrimary, fontWeight: 500 }}>
        {text}
      </Typography.Text>
    </Flex>
  );
};

export default TrustIndicator;
