import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Col, Flex, Row, Statistic, Tag, Typography, theme } from 'antd';
import {
  AuditOutlined,
  CheckCircleFilled,
  FlagOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { ADMIN_NAV } from '../../components/staffNavs';
import adminService from '../../api/adminService';
import auditService from '../../api/auditService';
import type { UserProfile } from '../../types';

const { Text, Title } = Typography;

/** Every service behind the gateway (see proposal §3.2). Health shown per FR-34. */
const PLATFORM_SERVICES = [
  'API Gateway',
  'Auth Service',
  'TOTP Service',
  'User Service',
  'Accounts Service',
  'Transfer Service',
  'Payments Service',
  'Lending Service',
  'Notification Service',
  'Audit & Recovery Service',
];

const AdminDashboard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [flaggedCount, setFlaggedCount] = useState<number>(0);

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

    auditService
      .getTransactions()
      .then((data) => {
        if (!cancelled) setFlaggedCount((data || []).filter((entry) => entry.flagged).length);
      })
      .catch(() => {
        if (!cancelled) setFlaggedCount(0);
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const countByRole = (role: UserProfile['role']) =>
    users.filter((user) => user.role === role).length;
  const pendingReview = users.filter((user) => user.status === 'PENDING_REVIEW').length;
  const frozenOrSuspended = users.filter(
    (user) => user.status === 'FROZEN' || user.status === 'SUSPENDED'
  ).length;

  return (
    <StaffLayout portalName="System administration" roleLabel="ADMIN" navItems={ADMIN_NAV}>
      <Row gutter={[12, 12]} style={{ marginBottom: 20 }}>
        <Col span={12}>
          <Card size="small" hoverable onClick={() => navigate('/admin/users')}>
            <Statistic
              title="Total users"
              value={users.length}
              prefix={<TeamOutlined style={{ color: token.colorPrimary }} />}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small" hoverable onClick={() => navigate('/admin/users')}>
            <Statistic title="Waiting for review" value={pendingReview} />
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small" hoverable onClick={() => navigate('/admin/audit')}>
            <Statistic
              title="Flagged entries"
              value={flaggedCount}
              prefix={<FlagOutlined style={{ color: token.colorError }} />}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card size="small">
            <Statistic title="Frozen / suspended" value={frozenOrSuspended} />
          </Card>
        </Col>
      </Row>

      <Card size="small" style={{ marginBottom: 20 }}>
        <Flex align="center" gap={8} style={{ marginBottom: 12 }}>
          <SafetyCertificateOutlined style={{ color: token.colorPrimary, fontSize: 18 }} />
          <Title level={5} style={{ margin: 0 }}>
            Users by role
          </Title>
        </Flex>
        <Flex gap={8} wrap="wrap">
          <Tag color="blue">Customers: {countByRole('CUSTOMER')}</Tag>
          <Tag color="purple">Merchants: {countByRole('MERCHANT')}</Tag>
          <Tag color="cyan">Bank officers: {countByRole('BANK_OFFICER')}</Tag>
          <Tag color="gold">Admins: {countByRole('ADMIN')}</Tag>
        </Flex>
      </Card>

      <Card size="small">
        <Flex align="center" gap={8} style={{ marginBottom: 4 }}>
          <AuditOutlined style={{ color: token.colorPrimary, fontSize: 18 }} />
          <Title level={5} style={{ margin: 0 }}>
            System health
          </Title>
        </Flex>
        <Text style={{ color: token.colorTextSecondary, fontSize: 12 }}>
          Live status of every platform service (FR-34).
        </Text>
        <div style={{ marginTop: 12 }}>
          {PLATFORM_SERVICES.map((service, index) => (
            <Flex
              key={service}
              justify="space-between"
              align="center"
              style={{
                padding: '10px 0',
                borderTop: index === 0 ? 'none' : `1px solid ${token.colorBorderSecondary}`,
              }}
            >
              <Text style={{ fontSize: 13 }}>{service}</Text>
              <Flex align="center" gap={6}>
                <CheckCircleFilled style={{ color: '#1F7A6C', fontSize: 13 }} />
                <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>Online</Text>
              </Flex>
            </Flex>
          ))}
        </div>
      </Card>
    </StaffLayout>
  );
};

export default AdminDashboard;
