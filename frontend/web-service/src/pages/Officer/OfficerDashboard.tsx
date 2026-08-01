import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, Col, Row, Statistic, Typography, theme } from 'antd';
import { FlagOutlined, IdcardOutlined, PercentageOutlined, TeamOutlined } from '@ant-design/icons';
import StaffLayout from '../../components/StaffLayout';
import { OFFICER_NAV } from '../../components/staffNavs';
import kycService from '../../api/kycService';
import lendingService from '../../api/lendingService';
import auditService from '../../api/auditService';
import adminService from '../../api/adminService';
import {
  DEMO_ADMIN_USERS,
  DEMO_AUDIT_JOURNAL,
  DEMO_KYC_QUEUE,
  DEMO_LOAN_QUEUE,
} from '../../mocks/demoStaff';

const { Text } = Typography;

const OfficerDashboard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [kycCount, setKycCount] = useState(DEMO_KYC_QUEUE.length);
  const [loanCount, setLoanCount] = useState(DEMO_LOAN_QUEUE.length);
  const [flaggedCount, setFlaggedCount] = useState(
    DEMO_AUDIT_JOURNAL.filter((entry) => entry.flagged).length
  );
  const [customerCount, setCustomerCount] = useState(
    DEMO_ADMIN_USERS.filter((user) => user.role === 'CUSTOMER').length
  );

  useEffect(() => {
    let cancelled = false;

    kycService
      .getPendingApplications()
      .then((data) => {
        if (!cancelled) setKycCount(data.length);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    lendingService
      .getPendingApplications()
      .then((data) => {
        if (!cancelled) setLoanCount(data.length);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    auditService
      .getTransactions()
      .then((data) => {
        if (!cancelled) setFlaggedCount(data.filter((entry) => entry.flagged).length);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    adminService
      .getUsers()
      .then((data) => {
        if (!cancelled) setCustomerCount(data.filter((user) => user.role === 'CUSTOMER').length);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const queues = [
    {
      key: 'kyc',
      title: 'KYC checks waiting',
      value: kycCount,
      icon: <IdcardOutlined style={{ color: token.colorPrimary }} />,
      path: '/officer/kyc',
      hint: 'Waiting for identity approval in the bank system.',
    },
    {
      key: 'loans',
      title: 'Loan applications',
      value: loanCount,
      icon: <PercentageOutlined style={{ color: token.colorPrimary }} />,
      path: '/officer/loans',
      hint: 'Waiting for a decision in the lending system.',
    },
    {
      key: 'flagged',
      title: 'Flagged transactions',
      value: flaggedCount,
      icon: <FlagOutlined style={{ color: token.colorError }} />,
      path: '/officer/flagged',
      hint: 'Held by fraud rules; reviewed in the bank system (FR-31).',
    },
    {
      key: 'customers',
      title: 'Customers on book',
      value: customerCount,
      icon: <TeamOutlined style={{ color: token.colorPrimary }} />,
      path: '/officer/customers',
      hint: 'Look up customer accounts and their status.',
    },
  ];

  return (
    <StaffLayout portalName="Branch operations" roleLabel="BANK OFFICER" navItems={OFFICER_NAV}>
      <Row gutter={[12, 12]}>
        {queues.map((queue) => (
          <Col span={24} key={queue.key}>
            <Card size="small" hoverable onClick={() => navigate(queue.path)}>
              <Statistic title={queue.title} value={queue.value} prefix={queue.icon} />
              <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>{queue.hint}</Text>
            </Card>
          </Col>
        ))}
      </Row>
    </StaffLayout>
  );
};

export default OfficerDashboard;
