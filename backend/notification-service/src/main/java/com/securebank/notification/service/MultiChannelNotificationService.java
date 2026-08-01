package com.securebank.notification.service;

import com.securebank.notification.dispatcher.NotificationDispatcher;
import com.securebank.notification.entity.Notification;
import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import com.securebank.notification.exception.ResourceNotFoundException;
import com.securebank.notification.repository.NotificationRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class MultiChannelNotificationService {

  private final NotificationRepository notificationRepository;
  private final Map<NotificationChannel, NotificationDispatcher> dispatchers = new EnumMap<>(
    NotificationChannel.class
  );

  public MultiChannelNotificationService(
    NotificationRepository notificationRepository,
    List<NotificationDispatcher> dispatcherList
  ) {
    this.notificationRepository = notificationRepository;
    for (NotificationDispatcher dispatcher : dispatcherList) {
      this.dispatchers.put(dispatcher.getChannel(), dispatcher);
    }
  }

  @Transactional
  public Notification sendNotification(
    UUID userId,
    NotificationType type,
    NotificationChannel channel,
    String title,
    String message,
    String recipientContact,
    String metadataJson
  ) {
    Notification notification = Notification.builder()
      .userId(userId)
      .type(type)
      .channel(channel != null ? channel : NotificationChannel.IN_APP)
      .title(title)
      .message(message)
      .read(false)
      .metadataJson(metadataJson)
      .build();

    notification = notificationRepository.save(notification);

    // Dispatch via external channel if requested (or SMS fallback for security alerts)
    NotificationChannel targetChannel = channel != null ? channel : NotificationChannel.IN_APP;
    if (targetChannel != NotificationChannel.IN_APP && dispatchers.containsKey(targetChannel)) {
      dispatchers.get(targetChannel).dispatch(userId, type, title, message, recipientContact);
    }

    // FR-29: Guaranteed SMS fallback for critical security alerts
    if (
      type == NotificationType.SECURITY_ALERT &&
      targetChannel != NotificationChannel.SMS &&
      dispatchers.containsKey(NotificationChannel.SMS)
    ) {
      dispatchers
        .get(NotificationChannel.SMS)
        .dispatch(userId, type, title, message, recipientContact);
    }

    return notification;
  }

  @Transactional(readOnly = true)
  public Page<Notification> getUserNotifications(
    UUID userId,
    Boolean unreadOnly,
    NotificationType type,
    Pageable pageable
  ) {
    if (Boolean.TRUE.equals(unreadOnly)) {
      return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(
        userId,
        false,
        pageable
      );
    } else if (type != null) {
      return notificationRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);
    } else {
      return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(UUID userId) {
    return notificationRepository.countByUserIdAndReadFalse(userId);
  }

  @Transactional
  public Notification markAsRead(UUID id, UUID userId) {
    Notification notification = notificationRepository
      .findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));

    if (!notification.getUserId().equals(userId)) {
      throw new ResourceNotFoundException("Notification not found: " + id);
    }

    notification.setRead(true);
    return notificationRepository.save(notification);
  }

  @Transactional
  public int markAllAsRead(UUID userId) {
    return notificationRepository.markAllAsReadForUser(userId);
  }
}
