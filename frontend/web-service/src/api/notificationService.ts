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

/** Notification as notification-service returns it. */
interface BackendNotification {
  id: string;
  userId: string;
  type: 'TRANSACTION_ALERT' | 'SECURITY_ALERT' | 'ACCOUNT_ALERT' | 'SYSTEM_NOTICE';
  channel: string;
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
  metadataJson: string | null;
}

/** The list endpoint is paged (Spring `Page`). */
interface Paged<T> {
  content: T[];
}

const TYPE_MAP: Record<BackendNotification['type'], NotificationType> = {
  SECURITY_ALERT: 'security',
  ACCOUNT_ALERT: 'warning',
  TRANSACTION_ALERT: 'info',
  SYSTEM_NOTICE: 'info',
};

const CATEGORY_MAP: Record<BackendNotification['type'], string> = {
  SECURITY_ALERT: 'Security alert',
  ACCOUNT_ALERT: 'Account alert',
  TRANSACTION_ALERT: 'Transaction',
  SYSTEM_NOTICE: 'Notice',
};

const startOfDay = (value: Date) =>
  new Date(value.getFullYear(), value.getMonth(), value.getDate()).getTime();

const DAY_MS = 24 * 60 * 60 * 1000;

const timeOf = (date: Date) =>
  date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });

/** Recent items show just the time; older ones carry their date too. */
const formatTimestamp = (date: Date, daysAgo: number) =>
  daysAgo <= 1
    ? timeOf(date)
    : `${date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' })} - ${timeOf(date)}`;

const formatGroupLabel = (daysAgo: number) => {
  if (daysAgo <= 0) return 'Today';
  if (daysAgo === 1) return 'Yesterday';
  return 'Earlier';
};

const mapNotification = (notification: BackendNotification): Notification => {
  const created = new Date(notification.createdAt);
  const daysAgo = Math.round((startOfDay(new Date()) - startOfDay(created)) / DAY_MS);

  return {
    id: notification.id,
    type: TYPE_MAP[notification.type] ?? 'info',
    title: notification.title,
    description: notification.message,
    categoryLabel: CATEGORY_MAP[notification.type],
    timestamp: formatTimestamp(created, daysAgo),
    groupLabel: formatGroupLabel(daysAgo),
    read: notification.read,
  };
};

export const notificationService = {
  client,
  getNotifications: () =>
    client
      .get<Paged<BackendNotification>>('')
      .then((response) => (response.data.content ?? []).map(mapNotification)),
  markAllAsRead: () => client.post('/read-all').then((response) => response.data),
};

export default notificationService;
