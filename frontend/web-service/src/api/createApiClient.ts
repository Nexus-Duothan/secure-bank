import axios, { type AxiosInstance } from 'axios';
import tokenStorage from './tokenStorage';

export const createApiClient = (baseURL: string): AxiosInstance => {
  const client = axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  client.interceptors.request.use((config) => {
    const accessToken = tokenStorage.getAccessToken();
    if (accessToken) {
      config.headers.set('Authorization', `Bearer ${accessToken}`);
    }
    return config;
  });

  return client;
};
