import type { Role } from '../types';

const SESSION_USER_KEY = 'sb_session_user';

export interface SessionUser {
  userId: string;
  username: string;
  role: Role;
  status: string;
}

const isRole = (value: string): value is Role =>
  value === 'CUSTOMER' || value === 'MERCHANT' || value === 'BANK_OFFICER' || value === 'ADMIN';

/** Landing page for each role. Staff roles never land on the customer dashboard. */
export const homePathForRole = (role: Role): string => {
  switch (role) {
    case 'ADMIN':
      return '/admin';
    case 'BANK_OFFICER':
      return '/officer';
    case 'MERCHANT':
      return '/merchant';
    default:
      return '/dashboard';
  }
};

export const sessionUser = {
  get: (): SessionUser | null => {
    const raw = localStorage.getItem(SESSION_USER_KEY);
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as SessionUser;
      if (!parsed.role || !isRole(parsed.role)) return null;
      return parsed;
    } catch {
      return null;
    }
  },
  set: (user: SessionUser) => {
    localStorage.setItem(SESSION_USER_KEY, JSON.stringify(user));
  },
  clear: () => {
    localStorage.removeItem(SESSION_USER_KEY);
  },
};

export default sessionUser;
