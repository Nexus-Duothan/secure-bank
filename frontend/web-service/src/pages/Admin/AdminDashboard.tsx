import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Button,
  Card,
  Col,
  Flex,
  Row,
  Spin,
  Statistic,
  Tag,
  Tooltip,
  Typography,
  theme,
} from 'antd';
import {
  AuditOutlined,
  CheckCircleFilled,
  CloseCircleFilled,
  FlagOutlined,
  QuestionCircleFilled,
  ReloadOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
} from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { ADMIN_NAV } from '../../components/staffNavs';
import adminService, { type ServiceHealth, type ServiceHealthStatus } from '../../api/adminService';
import { getApiErrorMessage } from '../../api/apiError';
import auditService from '../../api/auditService';
import type { UserProfile } from '../../types';

const { Text, Title } = Typography;

/** How often the panel re-measures while an administrator is watching it. */
const HEALTH_REFRESH_MS = 15000;

const STATUS_PRESENTATION: Record<
  ServiceHealthStatus,
  { label: string; color: string; icon: React.ReactNode }
> = {
  UP: {
    label: 'Online',
    color: '#1F7A6C',
    icon: <CheckCircleFilled style={{ color: '#1F7A6C', fontSize: 13 }} />,
  },
  DOWN: {
    label: 'Offline',
    color: '#CF1322',
    icon: <CloseCircleFilled style={{ color: '#CF1322', fontSize: 13 }} />,
  },
  UNKNOWN: {
    label: 'Unknown',
    color: '#AD6800',
    icon: <QuestionCircleFilled style={{ color: '#D48806', fontSize: 13 }} />,
  },
};

const AdminDashboard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [flaggedCount, setFlaggedCount] = useState<number>(0);
  const [health, setHealth] = useState<ServiceHealth[]>([]);
  const [healthError, setHealthError] = useState<string | null>(null);
  const [checkingHealth, setCheckingHealth] = useState(true);

  const loadHealth = useCallback(async () => {
    setCheckingHealth(true);
    try {
      const data = await adminService.getSystemHealth();
      setHealth(data ?? []);
      setHealthError(null);
    } catch (error) {
      // If the panel itself cannot be reached, say so rather than showing a stale all-green list.
      setHealth([]);
      setHealthError(
        getApiErrorMessage(error, 'Could not reach user-service to measure platform health.')
      );
    } finally {
      setCheckingHealth(false);
    }
  }, []);

  useEffect(() => {
    void loadHealth();
    const timer = setInterval(() => void loadHealth(), HEALTH_REFRESH_MS);
    return () => clearInterval(timer);
  }, [loadHealth]);

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

  const offlineCount = health.filter((service) => service.status === 'DOWN').length;
  const lastCheckedLabel =
    health.length > 0
      ? new Date(health[0].checkedAt).toLocaleTimeString('en-GB', {
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit',
        })
      : null;

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
        <Flex justify="space-between" align="flex-start" gap={8}>
          <div>
            <Flex align="center" gap={8} style={{ marginBottom: 4 }}>
              <AuditOutlined style={{ color: token.colorPrimary, fontSize: 18 }} />
              <Title level={5} style={{ margin: 0 }}>
                System health
              </Title>
              {offlineCount > 0 && (
                <Tag color="red" style={{ marginInlineEnd: 0 }}>
                  {offlineCount} offline
                </Tag>
              )}
            </Flex>
            <Text style={{ color: token.colorTextSecondary, fontSize: 12 }}>
              {lastCheckedLabel
                ? `Every service probed directly. Last checked ${lastCheckedLabel}.`
                : 'Every service probed directly.'}
            </Text>
          </div>
          <Tooltip title="Check again now">
            <Button
              size="small"
              icon={<ReloadOutlined />}
              loading={checkingHealth}
              onClick={() => void loadHealth()}
            />
          </Tooltip>
        </Flex>

        {healthError && (
          <Alert
            type="warning"
            showIcon
            message={healthError}
            style={{ marginTop: 12, marginBottom: 4 }}
          />
        )}

        <div style={{ marginTop: 12 }}>
          {health.length === 0 && checkingHealth ? (
            <Flex justify="center" style={{ padding: '24px 0' }}>
              <Spin />
            </Flex>
          ) : (
            health.map((service, index) => {
              const presentation = STATUS_PRESENTATION[service.status];
              return (
                <Flex
                  key={service.key}
                  justify="space-between"
                  align="center"
                  gap={12}
                  style={{
                    padding: '10px 0',
                    borderTop: index === 0 ? 'none' : `1px solid ${token.colorBorderSecondary}`,
                  }}
                >
                  <div style={{ minWidth: 0 }}>
                    <Text style={{ fontSize: 13 }}>{service.name}</Text>
                    {service.detail && (
                      <Text
                        style={{
                          display: 'block',
                          fontSize: 11,
                          color: token.colorTextTertiary,
                        }}
                      >
                        {service.detail}
                      </Text>
                    )}
                  </div>
                  <Flex align="center" gap={6} style={{ flexShrink: 0 }}>
                    {presentation.icon}
                    <Text style={{ fontSize: 12, color: presentation.color, fontWeight: 500 }}>
                      {presentation.label}
                    </Text>
                    {service.status === 'UP' && service.responseTimeMs !== null && (
                      <Text style={{ fontSize: 11, color: token.colorTextTertiary }}>
                        {service.responseTimeMs}ms
                      </Text>
                    )}
                  </Flex>
                </Flex>
              );
            })
          )}
        </div>
      </Card>
    </StaffLayout>
  );
};

export default AdminDashboard;
