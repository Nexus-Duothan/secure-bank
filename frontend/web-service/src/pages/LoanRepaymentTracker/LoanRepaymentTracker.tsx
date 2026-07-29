import React from 'react';
import { Typography } from 'antd';
import { useParams } from 'react-router-dom';

const LoanRepaymentTracker: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  return <Typography.Title level={2}>Loan Repayments: {id}</Typography.Title>;
};

export default LoanRepaymentTracker;
