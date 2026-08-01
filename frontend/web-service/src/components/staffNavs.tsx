import {
  AuditOutlined,
  DashboardOutlined,
  DollarOutlined,
  FlagOutlined,
  IdcardOutlined,
  PercentageOutlined,
  TeamOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import type { StaffNavItem } from './StaffLayout';

export const ADMIN_NAV: StaffNavItem[] = [
  { key: 'home', label: 'Overview', icon: <DashboardOutlined />, path: '/admin' },
  { key: 'users', label: 'Users', icon: <TeamOutlined />, path: '/admin/users' },
  { key: 'audit', label: 'Audit log', icon: <AuditOutlined />, path: '/admin/audit' },
];

export const OFFICER_NAV: StaffNavItem[] = [
  { key: 'home', label: 'Work queue', icon: <DashboardOutlined />, path: '/officer' },
  { key: 'kyc', label: 'KYC', icon: <IdcardOutlined />, path: '/officer/kyc' },
  { key: 'loans', label: 'Loans', icon: <PercentageOutlined />, path: '/officer/loans' },
  { key: 'customers', label: 'Customers', icon: <TeamOutlined />, path: '/officer/customers' },
  { key: 'flagged', label: 'Flags', icon: <FlagOutlined />, path: '/officer/flagged' },
];

export const MERCHANT_NAV: StaffNavItem[] = [
  { key: 'home', label: 'Overview', icon: <DashboardOutlined />, path: '/merchant' },
  { key: 'payments', label: 'Payments', icon: <DollarOutlined />, path: '/merchant/payments' },
  {
    key: 'settlements',
    label: 'Settlements',
    icon: <WalletOutlined />,
    path: '/merchant/settlements',
  },
];
