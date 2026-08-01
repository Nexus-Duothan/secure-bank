import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import tokenStorage from './tokenStorage';
import sessionUser from './sessionUser';

type RetriableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

interface ApiClientOptions {
  attachAccessToken?: boolean;
}

let refreshRequest: Promise<string | null> | null = null;

const forceLogout = () => {
  tokenStorage.clear();
  sessionUser.clear();
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.replace('/login');
  }
};

const refreshAccessToken = async (): Promise<string | null> => {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    forceLogout();
    return null;
  }

  if (!refreshRequest) {
    refreshRequest = axios
      .post(
        '/api/v1/auth/refresh',
        { refreshToken },
        { headers: { 'Content-Type': 'application/json' } }
      )
      .then((response) => {
        const nextAccessToken = response.data?.accessToken as string | undefined;
        const nextRefreshToken = response.data?.refreshToken as string | undefined;

        if (!nextAccessToken || !nextRefreshToken) {
          forceLogout();
          return null;
        }

        tokenStorage.setTokens(nextAccessToken, nextRefreshToken);
        return nextAccessToken;
      })
      .catch(() => {
        forceLogout();
        return null;
      })
      .finally(() => {
        refreshRequest = null;
      });
  }

  return refreshRequest;
};

export const createApiClient = (baseURL: string, options: ApiClientOptions = {}): AxiosInstance => {
  const { attachAccessToken = true } = options;
  const client = axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  if (attachAccessToken) {
    client.interceptors.request.use((config) => {
      const accessToken = tokenStorage.getAccessToken();
      if (accessToken) {
        config.headers.set('Authorization', `Bearer ${accessToken}`);
      }
      return config;
    });
  }

  if (attachAccessToken) {
    client.interceptors.response.use(
      (response) => response,
      async (error) => {
        const status = error.response?.status as number | undefined;
        const originalRequest = error.config as RetriableRequestConfig | undefined;

        if (!originalRequest || status !== 401 || originalRequest._retry) {
          return Promise.reject(error);
        }

        if (originalRequest.url?.includes('/login') || originalRequest.url?.includes('/refresh')) {
          return Promise.reject(error);
        }

        const nextAccessToken = await refreshAccessToken();
        if (!nextAccessToken) {
          return Promise.reject(error);
        }

        originalRequest._retry = true;
        originalRequest.headers.set('Authorization', `Bearer ${nextAccessToken}`);
        return client(originalRequest);
      }
    );
  }

  return client;
};
