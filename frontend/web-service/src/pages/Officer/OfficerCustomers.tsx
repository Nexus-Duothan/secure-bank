import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Avatar,
  Button,
  Flex,
  Input,
  Modal,
  Select,
  Tag,
  Typography,
  message,
  theme,
} from 'antd';
import { SafetyOutlined, SearchOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import adminService from '../../api/adminService';
import { getApiErrorMessage } from '../../api/apiError';
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

const OfficerCustomers: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [messageApi, messageContext] = message.useMessage();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<UserProfile | null>(null);
  const [nextStatus, setNextStatus] = useState<UserStatus>('ACTIVE');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    adminService
      .getUsers()
      .then((data) => {
        if (!cancelled) setUsers(data || []);
      })
      .catch(() => {
        if (!cancelled) setUsers([]);
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

  const openCustomer = (user: UserProfile) => {
    setSelected(user);
    setNextStatus(user.status);
  };

  const requestStatusChange = async () => {
    if (!selected || nextStatus === selected.status) return;
    setSaving(true);
    try {
      const challenge = await adminService.requestStatusChange(selected.id, nextStatus);
      navigate('/verify-otp', {
        state: {
          flow: 'admin-change',
          challenge,
          successMessage: `Account status updated for ${selected.fullName}.`,
          returnTo: '/officer/customers',
        },
      });
    } catch (error) {
      messageApi.error(getApiErrorMessage(error, 'Could not start the account status change.'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <StaffLayout portalName="Customer lookup" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      {messageContext}
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
            onClick={() => openCustomer(user)}
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
            <Alert
              type="warning"
              showIcon
              icon={<SafetyOutlined />}
              message="Protected account action"
              description="Freezing, suspending, or reactivating an account requires your authenticator code."
            />
            <Flex gap={8}>
              <Select
                value={nextStatus}
                onChange={setNextStatus}
                style={{ flex: 1 }}
                size="large"
                options={[
                  { value: 'ACTIVE', label: 'Active' },
                  { value: 'FROZEN', label: 'Frozen' },
                  { value: 'SUSPENDED', label: 'Suspended' },
                  { value: 'PENDING_REVIEW', label: 'Pending review' },
                ]}
              />
              <Button
                type="primary"
                size="large"
                loading={saving}
                disabled={nextStatus === selected.status}
                onClick={() => void requestStatusChange()}
              >
                Change status
              </Button>
            </Flex>
          </Flex>
        )}
      </Modal>
    </StaffLayout>
  );
};

export default OfficerCustomers;
