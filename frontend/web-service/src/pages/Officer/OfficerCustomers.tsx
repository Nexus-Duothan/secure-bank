import React, { useEffect, useMemo, useState } from 'react';
import { Avatar, Flex, Input, Modal, Tag, Typography, theme } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import adminService from '../../api/adminService';
import { DEMO_ADMIN_USERS } from '../../mocks/demoStaff';
import type { UserProfile, UserStatus } from '../../types';

const { Text } = Typography;

const statusColor: Record<UserStatus, string> = {
  ACTIVE: 'green',
  FROZEN: 'orange',
  SUSPENDED: 'red',
  PENDING_REVIEW: 'default',
};

const getInitials = (name: string) =>
  name
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

/**
 * View-only customer lookup for officers. Account actions (holds, suspensions)
 * are handled in the bank's core system; this app only shows the state.
 */
const OfficerCustomers: React.FC = () => {
  const { token } = theme.useToken();
  const [users, setUsers] = useState<UserProfile[]>(DEMO_ADMIN_USERS);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<UserProfile | null>(null);

  useEffect(() => {
    let cancelled = false;
    adminService
      .getUsers()
      .then((data) => {
        if (!cancelled) setUsers(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const customers = useMemo(() => {
    const query = search.trim().toLowerCase();
    return users
      .filter((user) => user.role === 'CUSTOMER' || user.role === 'MERCHANT')
      .filter(
        (user) =>
          !query ||
          user.fullName.toLowerCase().includes(query) ||
          user.email.toLowerCase().includes(query)
      );
  }, [users, search]);

  return (
    <StaffLayout portalName="Customer lookup" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      <Input
        prefix={<SearchOutlined style={{ color: token.colorTextTertiary }} />}
        placeholder="Search customers by name or email"
        allowClear
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        size="large"
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
        {customers.length === 0 && (
          <Text
            style={{
              display: 'block',
              padding: 24,
              textAlign: 'center',
              color: token.colorTextSecondary,
            }}
          >
            No customers match this search.
          </Text>
        )}
        {customers.map((user, index) => (
          <Flex
            key={user.id}
            align="center"
            gap={12}
            onClick={() => setSelected(user)}
            style={{
              padding: '14px 16px',
              cursor: 'pointer',
              borderTop: index === 0 ? 'none' : `1px solid ${token.colorBorderSecondary}`,
            }}
          >
            <Avatar style={{ background: '#0B1B2B', fontWeight: 600 }}>
              {getInitials(user.fullName)}
            </Avatar>
            <div style={{ flex: 1, minWidth: 0 }}>
              <Text style={{ display: 'block', fontWeight: 600, fontSize: 14 }} ellipsis>
                {user.fullName}
              </Text>
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }} ellipsis>
                {user.email}
              </Text>
            </div>
            <Tag color={statusColor[user.status]} style={{ marginInlineEnd: 0 }}>
              {user.status.replace('_', ' ')}
            </Tag>
          </Flex>
        ))}
      </div>

      <Modal
        open={selected !== null}
        onCancel={() => setSelected(null)}
        title={selected?.fullName}
        footer={null}
      >
        {selected && (
          <Flex vertical gap={16} style={{ paddingTop: 8 }}>
            <div>
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Email</Text>
              <Text style={{ display: 'block', fontWeight: 500 }}>{selected.email}</Text>
            </div>
            <div>
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Phone</Text>
              <Text style={{ display: 'block', fontWeight: 500 }}>{selected.phoneNumber}</Text>
            </div>
            <div>
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>ID verified</Text>
              <Text style={{ display: 'block', fontWeight: 500 }}>
                {selected.idVerified ? 'Yes' : 'No'}
              </Text>
            </div>
            <div>
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Account status</Text>
              <div style={{ marginTop: 4 }}>
                <Tag color={statusColor[selected.status]}>{selected.status.replace('_', ' ')}</Tag>
              </div>
            </div>
          </Flex>
        )}
      </Modal>
    </StaffLayout>
  );
};

export default OfficerCustomers;
