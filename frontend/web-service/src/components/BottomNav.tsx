import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Flex, Typography, theme } from 'antd';
import {
  HomeOutlined,
  BankOutlined,
  SwapOutlined,
  PercentageOutlined,
  UserOutlined,
} from '@ant-design/icons';

const { Text } = Typography;

interface NavItem {
  key: string;
  label: string;
  icon: React.ReactNode;
  path: string;
}

const NAV_ITEMS: NavItem[] = [
  { key: 'home', label: 'Home', icon: <HomeOutlined />, path: '/dashboard' },
  { key: 'accounts', label: 'Accounts', icon: <BankOutlined />, path: '/accounts' },
  { key: 'transfers', label: 'Transfers', icon: <SwapOutlined />, path: '/transfer' },
  { key: 'loans', label: 'Loans', icon: <PercentageOutlined />, path: '/loans/apply' },
  { key: 'profile', label: 'Profile', icon: <UserOutlined />, path: '/profile' },
];

interface BottomNavProps {
  accountsPath?: string;
}

const BottomNav: React.FC<BottomNavProps> = ({ accountsPath }) => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <Flex
      justify="space-around"
      align="center"
      style={{
        position: 'fixed',
        left: 0,
        right: 0,
        bottom: 0,
        background: token.colorBgContainer,
        borderTop: `1px solid ${token.colorBorder}`,
        padding: '10px 8px calc(env(safe-area-inset-bottom, 0px) + 10px)',
        zIndex: 10,
      }}
    >
      {NAV_ITEMS.map((item) => {
        const path = item.key === 'accounts' && accountsPath ? accountsPath : item.path;
        const isActive =
          item.key === 'home'
            ? location.pathname === '/dashboard'
            : item.key === 'profile'
              ? location.pathname.startsWith('/profile')
              : location.pathname.startsWith(item.path);
        const color = isActive ? token.colorPrimary : token.colorTextTertiary;

        return (
          <Flex
            key={item.key}
            vertical
            align="center"
            gap={4}
            onClick={() => navigate(path)}
            style={{ cursor: 'pointer', minWidth: 56 }}
          >
            <span style={{ fontSize: 20, color, display: 'flex' }}>{item.icon}</span>
            <Text style={{ fontSize: 11, fontWeight: 500, color }}>{item.label}</Text>
          </Flex>
        );
      })}
    </Flex>
  );
};

export default BottomNav;
