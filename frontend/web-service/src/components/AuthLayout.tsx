import React from 'react';
import { Flex, Typography, theme } from 'antd';

const { Title, Text } = Typography;

interface AuthLayoutProps {
  heading: string;
  maxWidth?: number;
  children: React.ReactNode;
}

const AuthLayout: React.FC<AuthLayoutProps> = ({ heading, maxWidth = 440, children }) => {
  const { token } = theme.useToken();

  return (
    <div
      style={{
        minHeight: '100vh',
        background: token.colorBgLayout,
        display: 'flex',
        justifyContent: 'center',
        padding: '64px 24px',
      }}
    >
      <Flex vertical style={{ width: '100%', maxWidth }}>
        <Text
          className="font-display"
          style={{ fontSize: 22, fontWeight: 600, color: token.colorText }}
        >
          SecureBank
        </Text>

        <Title
          level={2}
          className="font-display"
          style={{ margin: '40px 0 32px', color: token.colorText, fontWeight: 500 }}
        >
          {heading}
        </Title>

        {children}
      </Flex>
    </div>
  );
};

export default AuthLayout;
