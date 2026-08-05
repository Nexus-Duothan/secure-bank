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
const { Text } = Typography;

const OfficerDashboard: React.FC = () => {
  const { token } = theme.useToken();
  const navigate = useNavigate();
  const [kycCount, setKycCount] = useState(0);
  const [loanCount, setLoanCount] = useState(0);
  const [flaggedCount, setFlaggedCount] = useState(0);
  const [customerCount, setCustomerCount] = useState(0);

  useEffect(() => {
    let cancelled = false;

    kycService
      .getPendingApplications()
      .then((data) => {
        if (!cancelled) setKycCount((data || []).length);
      })
      .catch(() => {
        if (!cancelled) setKycCount(0);
      });

    lendingService
      .getPendingApplications()
      .then((data) => {
        if (!cancelled) setLoanCount((data || []).length);
      })
      .catch(() => {
        if (!cancelled) setLoanCount(0);
      });

    auditService
      .getTransactions()
      .then((data) => {
        if (!cancelled) setFlaggedCount((data || []).filter((entry) => entry.flagged).length);
      })
      .catch(() => {
        if (!cancelled) setFlaggedCount(0);
      });

    adminService
      .getUsers()
      .then((data) => {
        if (!cancelled)
          setCustomerCount((data || []).filter((user) => user.role === 'CUSTOMER').length);
      })
      .catch(() => {
        if (!cancelled) setCustomerCount(0);
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
