import type { OtpChallenge, Role, UserProfile, UserStatus } from '../types';

/**
 * All traffic goes through the API Gateway, which is the platform's single hardened entry point
 * (blueprint §2.2). In development Vite proxies `/api` there; set `VITE_USER_API_BASE` to talk to
 * a user-service instance directly instead.
 */
const API_BASE = import.meta.env.VITE_USER_API_BASE ?? '/api/v1/users';

/** The service was reached and rejected the request. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** The service could not be reached at all, so nothing was submitted. */
export class NetworkError extends Error {
  constructor(message = 'SecureBank services are unreachable') {
    super(message);
    this.name = 'NetworkError';
  }
}

/**
 * Identity headers the API Gateway is responsible for stamping on real traffic. The web client
 * only sets them for the operator console, which stands in for a signed-in staff session until
 * auth-service issues tokens.
 */
export type Identity = {
  userId?: string;
  role?: Role;
};

async function request<T>(
  path: string,
  init: RequestInit = {},
  identity: Identity = {}
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...(identity.userId ? { 'X-User-Id': identity.userId } : {}),
        ...(identity.role ? { 'X-User-Role': identity.role } : {}),
        ...init.headers,
      },
    });
  } catch {
    throw new NetworkError();
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorMessage(response));
  }
  return (await response.json()) as T;
}

async function readErrorMessage(response: Response): Promise<string> {
  try {
    const body = (await response.json()) as { message?: string };
    return body.message ?? `Request failed (${response.status})`;
  } catch {
    return `Request failed (${response.status})`;
  }
}

export function fetchProfile(identity?: Identity): Promise<UserProfile> {
  return request<UserProfile>('/me', {}, identity);
}

/** Stages a self-service change and returns the confirmation challenge. */
export function requestChange(endpoint: string, payload: unknown): Promise<OtpChallenge> {
  return request<OtpChallenge>(endpoint, {
    method: 'POST',
    body: JSON.stringify(payload ?? {}),
  });
}

export function confirmChange(changeRequestId: string, otpCode: string): Promise<UserProfile> {
  return request<UserProfile>(`/me/changes/${changeRequestId}/confirm`, {
    method: 'POST',
    body: JSON.stringify({ otpCode }),
  });
}

export function fetchAllUsers(identity: Identity): Promise<UserProfile[]> {
  return request<UserProfile[]>('/admin', {}, identity);
}

export function updateUserRole(
  identity: Identity,
  userId: string,
  role: Role
): Promise<UserProfile> {
  return request<UserProfile>(
    `/admin/${userId}/role`,
    { method: 'PATCH', body: JSON.stringify({ role }) },
    identity
  );
}

export function updateUserStatus(
  identity: Identity,
  userId: string,
  status: UserStatus
): Promise<UserProfile> {
  return request<UserProfile>(
    `/admin/${userId}/status`,
    { method: 'PATCH', body: JSON.stringify({ status }) },
    identity
  );
}
