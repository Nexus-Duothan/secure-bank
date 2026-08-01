import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login/Login';
import CreateAccount from './pages/CreateAccount/CreateAccount';
import ForgotPassword from './pages/ForgotPassword/ForgotPassword';
import ResetPassword from './pages/ResetPassword/ResetPassword';
import OtpVerification from './pages/OtpVerification/OtpVerification';
import Dashboard from './pages/Dashboard/Dashboard';
import AccountDetails from './pages/AccountDetails/AccountDetails';
import Accounts from './pages/Accounts/Accounts';
import LinkAccount from './pages/Accounts/LinkAccount';
import OpenAccount from './pages/Accounts/OpenAccount';
import LinkCreditCard from './pages/Accounts/LinkCreditCard';
import TransferMoney from './pages/TransferMoney/TransferMoney';
import PayVendor from './pages/PayVendor/PayVendor';
import LoanApplication from './pages/LoanApplication/LoanApplication';
import LoanRepaymentTracker from './pages/LoanRepaymentTracker/LoanRepaymentTracker';
import TransactionHistory from './pages/TransactionHistory/TransactionHistory';
import Notifications from './pages/Notifications/Notifications';
import Profile from './pages/Profile/Profile';
import RequireRole from './components/RequireRole';
import AdminDashboard from './pages/Admin/AdminDashboard';
import AdminUsers from './pages/Admin/AdminUsers';
import AdminAudit from './pages/Admin/AdminAudit';
import OfficerDashboard from './pages/Officer/OfficerDashboard';
import KycReviewQueue from './pages/Officer/KycReviewQueue';
import LoanReviewQueue from './pages/Officer/LoanReviewQueue';
import OfficerCustomers from './pages/Officer/OfficerCustomers';
import FlaggedTransactions from './pages/Officer/FlaggedTransactions';
import MerchantDashboard from './pages/Merchant/MerchantDashboard';
import MerchantPayments from './pages/Merchant/MerchantPayments';
import MerchantSettlements from './pages/Merchant/MerchantSettlements';
import StaffProfile from './pages/Staff/StaffProfile';
import type { Role } from './types';

const customer: Role[] = ['CUSTOMER'];
const admin: Role[] = ['ADMIN'];
const officer: Role[] = ['BANK_OFFICER'];
const merchant: Role[] = ['MERCHANT'];

const guard = (allowed: Role[], page: React.ReactElement) => (
  <RequireRole allowed={allowed}>{page}</RequireRole>
);

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<CreateAccount />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password/:token" element={<ResetPassword />} />
        <Route path="/verify-otp" element={<OtpVerification />} />

        {/* Customer banking — staff roles are redirected to their own portal */}
        <Route path="/dashboard" element={guard(customer, <Dashboard />)} />
        <Route path="/accounts" element={guard(customer, <Accounts />)} />
        <Route path="/accounts/link" element={guard(customer, <LinkAccount />)} />
        <Route path="/accounts/open" element={guard(customer, <OpenAccount />)} />
        <Route path="/accounts/cards/link" element={guard(customer, <LinkCreditCard />)} />
        <Route path="/accounts/:id" element={guard(customer, <AccountDetails />)} />
        <Route path="/transfer" element={guard(customer, <TransferMoney />)} />
        <Route path="/pay" element={guard(customer, <PayVendor />)} />
        <Route path="/loans/apply" element={guard(customer, <LoanApplication />)} />
        <Route path="/loans/:id/repayments" element={guard(customer, <LoanRepaymentTracker />)} />
        <Route path="/activity" element={guard(customer, <TransactionHistory />)} />
        <Route path="/notifications" element={guard(customer, <Notifications />)} />
        <Route path="/profile" element={guard(customer, <Profile />)} />

        {/* System administration portal */}
        <Route path="/admin" element={guard(admin, <AdminDashboard />)} />
        <Route path="/admin/users" element={guard(admin, <AdminUsers />)} />
        <Route path="/admin/audit" element={guard(admin, <AdminAudit />)} />
        <Route path="/admin/profile" element={guard(admin, <StaffProfile />)} />

        {/* Bank officer (branch operations) portal */}
        <Route path="/officer" element={guard(officer, <OfficerDashboard />)} />
        <Route path="/officer/kyc" element={guard(officer, <KycReviewQueue />)} />
        <Route path="/officer/loans" element={guard(officer, <LoanReviewQueue />)} />
        <Route path="/officer/customers" element={guard(officer, <OfficerCustomers />)} />
        <Route path="/officer/flagged" element={guard(officer, <FlaggedTransactions />)} />
        <Route path="/officer/profile" element={guard(officer, <StaffProfile />)} />

        {/* Merchant business portal */}
        <Route path="/merchant" element={guard(merchant, <MerchantDashboard />)} />
        <Route path="/merchant/payments" element={guard(merchant, <MerchantPayments />)} />
        <Route path="/merchant/settlements" element={guard(merchant, <MerchantSettlements />)} />
        <Route path="/merchant/profile" element={guard(merchant, <StaffProfile />)} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
