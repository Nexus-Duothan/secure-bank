import { createApiClient } from './createApiClient';

export const notificationService = createApiClient('/api/v1/notifications');

export default notificationService;
