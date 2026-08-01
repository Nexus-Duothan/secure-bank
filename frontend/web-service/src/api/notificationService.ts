import { createApiClient } from './createApiClient';

const client = createApiClient('/api/v1/notifications');

export type NotificationType = 'security' | 'warning' | 'info';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  description: string;
  categoryLabel?: string;
  timestamp: string;
  groupLabel: string;
  read: boolean;
}

export const notificationService = {
  client,
  getNotifications: () => client.get<Notification[]>('/').then((response) => response.data),
  markAllAsRead: () => client.post('/mark-all-read').then((response) => response.data),
};

export default notificationService;
