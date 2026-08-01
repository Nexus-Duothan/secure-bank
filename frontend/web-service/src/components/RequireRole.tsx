import React from 'react';
import { Navigate } from 'react-router-dom';
import tokenStorage from '../api/tokenStorage';
import sessionUser, { homePathForRole } from '../api/sessionUser';
import type { Role } from '../types';

interface RequireRoleProps {
  allowed: Role[];
  children: React.ReactElement;
}

/**
 * Route guard. Customer pages are only for customers; staff portals are only
 * for their own role. Anyone in the wrong place is sent to their own home.
 */
const RequireRole: React.FC<RequireRoleProps> = ({ allowed, children }) => {
  if (!tokenStorage.getAccessToken()) {
    return <Navigate to="/login" replace />;
  }

  const user = sessionUser.get();
  const role: Role = user?.role ?? 'CUSTOMER';

  if (!allowed.includes(role)) {
    return <Navigate to={homePathForRole(role)} replace />;
  }

  return children;
};

export default RequireRole;
