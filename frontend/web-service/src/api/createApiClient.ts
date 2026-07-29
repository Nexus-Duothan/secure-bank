import axios, { type AxiosInstance } from 'axios';

export const createApiClient = (baseURL: string): AxiosInstance =>
  axios.create({
    baseURL,
    headers: {
      'Content-Type': 'application/json',
    },
  });
