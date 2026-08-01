import React, { useEffect, useMemo, useState } from 'react';
import { Flex, Typography, message, theme } from 'antd';
import notificationService, {
  type Notification,
  type NotificationType,
} from '../../api/notificationService';

const { Text, Title, Link } = Typography;

const SLATE = '#5B6B82';

const MOCK_NOTIFICATIONS: Notification[] = [
  {
    id: 'ntf-001',
    type: 'security',
    title: 'New device sign-in verified',
    description:
      "Chrome on Windows · Colombo, LK. If this wasn't you, freeze your account immediately.",
    categoryLabel: 'Security alert',
    timestamp: '14:22',
    groupLabel: 'Today',
    read: false,
  },
  {
    id: 'ntf-002',
    type: 'security',
    title: 'Card attempt blocked',
    description: 'LKR 92,000.00 attempt from an unrecognised location was held for your review.',
    categoryLabel: 'Security alert',
    timestamp: '09:47',
    groupLabel: 'Today',
    read: false,
  },
  {
    id: 'ntf-003',
    type: 'info',
    title: 'Loan installment received',
    description: 'Your July installment of LKR 24,350.00 was collected automatically.',
    timestamp: '05 Jul · 08:00',
    groupLabel: 'Earlier',
    read: true,
  },
  {
    id: 'ntf-004',
    type: 'info',
    title: 'Statement ready',
    description: 'Your June statement for Everyday Current is ready to download.',
    timestamp: '01 Jul · 06:12',
    groupLabel: 'Earlier',
    read: true,
  },
];

const accentColor = (type: NotificationType, token: ReturnType<typeof theme.useToken>['token']) => {
  switch (type) {
    case 'security':
      return token.colorPrimary;
    case 'warning':
      return token.colorWarning;
    default:
      return SLATE;
  }
};

const groupByLabel = (notifications: Notification[]) => {
  const groups: { label: string; items: Notification[] }[] = [];
  notifications.forEach((item) => {
    const group = groups.find((g) => g.label === item.groupLabel);
    if (group) {
      group.items.push(item);
    } else {
      groups.push({ label: item.groupLabel, items: [item] });
    }
  });
  return groups;
};

const Notifications: React.FC = () => {
  const { token } = theme.useToken();
  const [notifications, setNotifications] = useState<Notification[]>(MOCK_NOTIFICATIONS);

  useEffect(() => {
    let cancelled = false;
    notificationService
      .getNotifications()
      .then((data) => {
        if (!cancelled) setNotifications(data);
      })
      .catch(() => {
        // Endpoint not available yet — fall back to the placeholder shown above.
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const hasUnread = useMemo(() => notifications.some((item) => !item.read), [notifications]);
  const groups = useMemo(() => groupByLabel(notifications), [notifications]);

  const handleMarkAllAsRead = () => {
    const previousNotifications = notifications;
    setNotifications((prev) => prev.map((item) => ({ ...item, read: true })));
    notificationService.markAllAsRead().catch(() => {
      setNotifications(previousNotifications);
      message.error('Could not mark notifications as read. Please try again.');
    });
  };

  return (
    <div style={{ minHeight: '100vh', background: token.colorBgLayout }}>
      <div style={{ maxWidth: 480, margin: '0 auto', padding: '32px 20px 48px' }}>
        <Flex justify="space-between" align="center" style={{ marginBottom: 24 }}>
          <Title
            level={3}
            className="font-display"
            style={{ margin: 0, color: token.colorText, fontWeight: 600 }}
          >
            Notifications
          </Title>
          {hasUnread && (
            <Link
              onClick={handleMarkAllAsRead}
              style={{ color: token.colorPrimary, fontWeight: 500, fontSize: 14 }}
            >
              Mark all as read
            </Link>
          )}
        </Flex>

        {groups.map((group) => (
          <div key={group.label} style={{ marginBottom: 20 }}>
            <Text
              style={{
                display: 'block',
                marginBottom: 10,
                fontSize: 12,
                fontWeight: 600,
                letterSpacing: 0.4,
                textTransform: 'uppercase',
                color: token.colorTextTertiary,
              }}
            >
              {group.label}
            </Text>

            <Flex vertical gap={12}>
              {group.items.map((item) => {
                const accent = accentColor(item.type, token);
                return (
                  <div
                    key={item.id}
                    style={{
                      background: token.colorBgContainer,
                      borderRadius: 16,
                      border: `1px solid ${item.read ? token.colorBorder : accent}`,
                      padding: '18px 20px',
                    }}
                  >
                    <Flex align="center" gap={10}>
                      {!item.read && (
                        <span
                          style={{
                            width: 8,
                            height: 8,
                            borderRadius: '50%',
                            background: accent,
                            flexShrink: 0,
                          }}
                        />
                      )}
                      <Text
                        className="font-display"
                        style={{ fontSize: 16, fontWeight: 600, color: token.colorText }}
                      >
                        {item.title}
                      </Text>
                    </Flex>

                    <Text
                      style={{
                        display: 'block',
                        marginTop: 8,
                        fontSize: 14,
                        lineHeight: 1.5,
                        color: token.colorTextSecondary,
                      }}
                    >
                      {item.description}
                    </Text>

                    <Flex align="center" gap={6} style={{ marginTop: 12 }}>
                      {!item.read && item.categoryLabel && (
                        <>
                          <Text style={{ fontSize: 13, fontWeight: 600, color: accent }}>
                            {item.categoryLabel}
                          </Text>
                          <Text style={{ fontSize: 13, color: token.colorTextTertiary }}>·</Text>
                        </>
                      )}
                      <Text
                        className="font-mono"
                        style={{ fontSize: 13, color: token.colorTextTertiary }}
                      >
                        {item.timestamp}
                      </Text>
                    </Flex>
                  </div>
                );
              })}
            </Flex>
          </div>
        ))}

        {groups.length === 0 && (
          <Flex justify="center" style={{ padding: '40px 0' }}>
            <Text style={{ color: token.colorTextTertiary }}>You're all caught up.</Text>
          </Flex>
        )}
      </div>
    </div>
  );
};

export default Notifications;
