import React from 'react';
import { Typography } from 'antd';
import { useParams } from 'react-router-dom';

const AccountDetails: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  return <Typography.Title level={2}>Account Details: {id}</Typography.Title>;
};

export default AccountDetails;
