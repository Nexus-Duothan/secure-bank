import { createApiClient } from './createApiClient';

export const authService = createApiClient('/api/v1/auth');

export default authService;
