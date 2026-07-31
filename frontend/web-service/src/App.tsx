import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login/Login';
import CreateAccount from './pages/CreateAccount/CreateAccount';
import ForgotPassword from './pages/ForgotPassword/ForgotPassword';
import ResetPassword from './pages/ResetPassword/ResetPassword';
import OtpVerification from './pages/OtpVerification/OtpVerification';
import Dashboard from './pages/Dashboard/Dashboard';
import AccountDetails from './pages/AccountDetails/AccountDetails';
import TransferMoney from './pages/TransferMoney/TransferMoney';
import PayVendor from './pages/PayVendor/PayVendor';
import LoanApplication from './pages/LoanApplication/LoanApplication';
import LoanRepaymentTracker from './pages/LoanRepaymentTracker/LoanRepaymentTracker';
import TransactionHistory from './pages/TransactionHistory/TransactionHistory';
import Notifications from './pages/Notifications/Notifications';
import Profile from './pages/Profile/Profile';

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
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/accounts/:id" element={<AccountDetails />} />
        <Route path="/transfer" element={<TransferMoney />} />
        <Route path="/pay" element={<PayVendor />} />
        <Route path="/loans/apply" element={<LoanApplication />} />
        <Route path="/loans/:id/repayments" element={<LoanRepaymentTracker />} />
        <Route path="/activity" element={<TransactionHistory />} />
        <Route path="/notifications" element={<Notifications />} />
        <Route path="/profile" element={<Profile />} />
      </Routes>
    </BrowserRouter>
  );
};

export default App;
