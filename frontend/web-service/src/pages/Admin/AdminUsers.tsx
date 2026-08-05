import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  Alert,
  Avatar,
  Button,
  Flex,
  Input,
  Modal,
  Segmented,
  Select,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import { SafetyOutlined, SearchOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { ADMIN_NAV } from '../../components/staffNavs';
import adminService from '../../api/adminService';
import sessionUser from '../../api/sessionUser';
import { getApiErrorMessage } from '../../api/apiError';
import type { Role, UserProfile, UserStatus } from '../../types';

const { Text, Title } = Typography;

const ROLE_OPTIONS: { value: Role; label: string }[] = [
  { value: 'CUSTOMER', label: 'Customer' },
  { value: 'MERCHANT', label: 'Merchant' },
  { value: 'BANK_OFFICER', label: 'Bank officer' },
  { value: 'ADMIN', label: 'Admin' },
];

const STATUS_OPTIONS: { value: UserStatus; label: string }[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'FROZEN', label: 'Frozen (hold)' },
  { value: 'SUSPENDED', label: 'Suspended' },
  { value: 'PENDING_REVIEW', label: 'Pending review' },
];

const roleColor: Record<Role, string> = {
  CUSTOMER: 'blue',
  MERCHANT: 'purple',
  BANK_OFFICER: 'cyan',
  ADMIN: 'gold',
};

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

const AdminUsers: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const location = useLocation();
  const [messageApi, messageContext] = message.useMessage();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<'ALL' | Role>('ALL');
  const [selected, setSelected] = useState<UserProfile | null>(null);
  const [nextRole, setNextRole] = useState<Role>('CUSTOMER');
  const [nextStatus, setNextStatus] = useState<UserStatus>('ACTIVE');
  const [savingRole, setSavingRole] = useState(false);
  const [savingStatus, setSavingStatus] = useState(false);
  const currentUserId = sessionUser.get()?.userId;

  useEffect(() => {
    const state = location.state as { otpSuccessMessage?: string } | null;
    if (state?.otpSuccessMessage) {
      messageApi.success(state.otpSuccessMessage);
      navigate(location.pathname, { replace: true, state: null });
    }
  }, [location, messageApi, navigate]);

  useEffect(() => {
    let cancelled = false;
    adminService
      .getUsers()
      .then((data) => {
        if (!cancelled) {
          setUsers(data || []);
        }
      })
      .catch(() => {
        if (!cancelled) setUsers([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return users.filter((user) => {
      if (roleFilter !== 'ALL' && user.role !== roleFilter) return false;
      if (!query) return true;
      return (
        user.fullName.toLowerCase().includes(query) || user.email.toLowerCase().includes(query)
      );
    });
  }, [users, search, roleFilter]);

  const openUser = (user: UserProfile) => {
    setSelected(user);
    setNextRole(user.role);
    setNextStatus(user.status);
  };

  const handleRoleChange = async () => {
    if (!selected || nextRole === selected.role) return;
    setSavingRole(true);
    try {
      const challenge = await adminService.requestRoleChange(selected.id, nextRole);
      navigate('/verify-otp', {
        state: {
          flow: 'admin-change',
          challenge,
          successMessage: `Role updated for ${selected.fullName}.`,
          returnTo: '/admin/users',
        },
      });
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'Could not start this role change.'));
    } finally {
      setSavingRole(false);
    }
  };

  const handleStatusChange = async () => {
    if (!selected || nextStatus === selected.status) return;
    setSavingStatus(true);
    try {
      const challenge = await adminService.requestStatusChange(selected.id, nextStatus);
      navigate('/verify-otp', {
        state: {
          flow: 'admin-change',
          challenge,
          successMessage: `Account status updated for ${selected.fullName}.`,
          returnTo: '/admin/users',
        },
      });
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'Could not start this status change.'));
    } finally {
      setSavingStatus(false);
    }
  };

  const isSelf = selected?.id === currentUserId;

  return (
    <StaffLayout portalName="User management" roleLabel="ADMIN" navItems={ADMIN_NAV}>
      {messageContext}
      <Input
        prefix={<SearchOutlined style={{ color: token.colorTextTertiary }} />}
        placeholder="Search by name or email"
        allowClear
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        size="large"
        style={{ marginBottom: 12 }}
      />
      <Segmented
        block
        value={roleFilter}
        onChange={(value) => setRoleFilter(value as 'ALL' | Role)}
        options={[
          { label: 'All', value: 'ALL' },
          { label: 'Customers', value: 'CUSTOMER' },
          { label: 'Merchants', value: 'MERCHANT' },
          { label: 'Staff', value: 'BANK_OFFICER' },
          { label: 'Admins', value: 'ADMIN' },
        ]}
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
        {filtered.length === 0 && (
          <Text
            style={{
              display: 'block',
              padding: 24,
              textAlign: 'center',
              color: token.colorTextSecondary,
            }}
          >
            No users match this search.
          </Text>
        )}
        {filtered.map((user, index) => (
          <Flex
            key={user.id}
            align="center"
            gap={12}
            onClick={() => openUser(user)}
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
            <Flex vertical align="flex-end" gap={4}>
              <Tag color={roleColor[user.role]} style={{ marginInlineEnd: 0 }}>
                {user.role.replace('_', ' ')}
              </Tag>
              <Tag color={statusColor[user.status]} style={{ marginInlineEnd: 0 }}>
                {user.status.replace('_', ' ')}
              </Tag>
            </Flex>
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

            {isSelf ? (
              <Alert type="info" showIcon message="You cannot change your own role or status." />
            ) : (
              <>
                <Alert
                  type="warning"
                  showIcon
                  icon={<SafetyOutlined />}
                  message="High-security change"
                  description="Role and status changes must be confirmed with a one-time code sent to you. Everything is written to the audit journal."
                />
                <div>
                  <Title level={5} style={{ margin: '0 0 6px' }}>
                    Role
                  </Title>
                  <Flex gap={8}>
                    <Select
                      value={nextRole}
                      onChange={setNextRole}
                      options={ROLE_OPTIONS}
                      style={{ flex: 1 }}
                      size="large"
                    />
                    <Button
                      type="primary"
                      size="large"
                      loading={savingRole}
                      disabled={nextRole === selected.role}
                      onClick={handleRoleChange}
                    >
                      Change role
                    </Button>
                  </Flex>
                </div>
                <div>
                  <Title level={5} style={{ margin: '0 0 6px' }}>
                    Account status
                  </Title>
                  <Flex gap={8}>
                    <Select
                      value={nextStatus}
                      onChange={setNextStatus}
                      options={STATUS_OPTIONS}
                      style={{ flex: 1 }}
                      size="large"
                    />
                    <Button
                      type="primary"
                      size="large"
                      loading={savingStatus}
                      disabled={nextStatus === selected.status}
                      onClick={handleStatusChange}
                    >
                      Change status
                    </Button>
                  </Flex>
                </div>
              </>
            )}
          </Flex>
        )}
      </Modal>
    </StaffLayout>
  );
};

export default AdminUsers;
