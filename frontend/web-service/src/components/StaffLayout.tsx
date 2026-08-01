import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { Button, Flex, Tag, Typography, theme } from 'antd';
import { LogoutOutlined, UserOutlined } from '@ant-design/icons';
import authService from '../api/authService';
import tokenStorage from '../api/tokenStorage';
import sessionUser, { homePathForRole } from '../api/sessionUser';

const { Text, Title } = Typography;

const NAVY = '#0B1B2B';

export interface StaffNavItem {
  key: string;
  label: string;
  icon: React.ReactNode;
  path: string;
}

interface StaffLayoutProps {
  portalName: string;
  roleLabel: string;
  navItems: StaffNavItem[];
  children: React.ReactNode;
}

/**
 * Shared shell for the bank-side portals (admin, bank officer, merchant).
 * Staff accounts are created by the bank, so there is no customer chrome here —
 * just the portal header, the work area, and a nav scoped to the role.
 */
const StaffLayout: React.FC<StaffLayoutProps> = ({ portalName, roleLabel, navItems, children }) => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const sessionRole = sessionUser.get()?.role;
  const profilePath =
    sessionRole && sessionRole !== 'CUSTOMER' ? `${homePathForRole(sessionRole)}/profile` : null;

  const handleSignOut = async () => {
    try {
      await authService.logout();
    } catch {
      // If the backend session revoke fails, still clear local state.
    } finally {
      tokenStorage.clear();
      sessionUser.clear();
      navigate('/login', { replace: true });
    }
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '28px 20px 112px' }}>
        <Flex justify="space-between" align="center" style={{ marginBottom: 24 }}>
          <div>
            <Text style={{ color: token.colorPrimary, fontSize: 13, fontWeight: 600 }}>
              SecureBank staff
            </Text>
            <Title
              level={3}
              className="font-display"
              style={{ margin: '2px 0 6px', color: token.colorText, fontWeight: 600 }}
            >
              {portalName}
            </Title>
            <Tag color="geekblue" style={{ fontWeight: 600 }}>
              {roleLabel}
            </Tag>
          </div>
          <Flex gap={10}>
            {profilePath && (
              <Button
                shape="circle"
                size="large"
                icon={<UserOutlined />}
                onClick={() => navigate(profilePath)}
                title="My profile"
                style={{ width: 44, height: 44 }}
              />
            )}
            <Button
              shape="circle"
              size="large"
              icon={<LogoutOutlined />}
              onClick={handleSignOut}
              title="Sign out"
              style={{ width: 44, height: 44 }}
            />
          </Flex>
        </Flex>

        {children}
      </div>

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
        {navItems.map((item) => {
          const isActive =
            item.path === navItems[0].path
              ? location.pathname === item.path
              : location.pathname.startsWith(item.path);
          const color = isActive ? token.colorPrimary : token.colorTextTertiary;

          return (
            <Flex
              key={item.key}
              vertical
              align="center"
              gap={4}
              onClick={() => navigate(item.path)}
              style={{ cursor: 'pointer', minWidth: 56 }}
            >
              <span style={{ fontSize: 20, color, display: 'flex' }}>{item.icon}</span>
              <Text style={{ fontSize: 11, fontWeight: 500, color }}>{item.label}</Text>
            </Flex>
          );
        })}
      </Flex>
    </div>
  );
};

export { NAVY as STAFF_NAVY };
export default StaffLayout;
