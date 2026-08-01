package com.securebank.notification.controller;

import com.securebank.notification.dto.*;
import com.securebank.notification.entity.Notification;
import com.securebank.notification.enums.NotificationType;
import com.securebank.notification.service.MultiChannelNotificationService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final MultiChannelNotificationService notificationService;

  @GetMapping
  public ResponseEntity<Page<NotificationResponse>> getNotifications(
    Authentication authentication,
    @RequestParam(required = false) Boolean unreadOnly,
    @RequestParam(required = false) NotificationType type,
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "20") int size
  ) {
    UUID userId = UUID.fromString(authentication.getName());
    Pageable pageable = PageRequest.of(page, size);
    Page<Notification> notifications = notificationService.getUserNotifications(
      userId,
      unreadOnly,
      type,
      pageable
    );
    Page<NotificationResponse> response = notifications.map(this::toResponse);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/unread-count")
  public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    long count = notificationService.getUnreadCount(userId);
    return ResponseEntity.ok(
      UnreadCountResponse.builder().userId(userId).unreadCount(count).build()
    );
  }

  @PatchMapping("/{id}/read")
  public ResponseEntity<NotificationResponse> markAsRead(
    Authentication authentication,
    @PathVariable UUID id
  ) {
    UUID userId = UUID.fromString(authentication.getName());
    Notification notification = notificationService.markAsRead(id, userId);
    return ResponseEntity.ok(toResponse(notification));
  }

  @PostMapping("/read-all")
  public ResponseEntity<UnreadCountResponse> markAllAsRead(Authentication authentication) {
    UUID userId = UUID.fromString(authentication.getName());
    notificationService.markAllAsRead(userId);
    return ResponseEntity.ok(UnreadCountResponse.builder().userId(userId).unreadCount(0).build());
  }

  @PostMapping("/send")
  @PreAuthorize("hasAnyRole('BANK_OFFICER', 'ADMIN')")
  public ResponseEntity<NotificationResponse> sendNotification(
    @Valid @RequestBody SendNotificationRequest request
  ) {
    Notification notification = notificationService.sendNotification(
      request.getUserId(),
      request.getType(),
      request.getChannel(),
      request.getTitle(),
      request.getMessage(),
      request.getRecipientContact(),
      request.getMetadataJson()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(notification));
  }

  private NotificationResponse toResponse(Notification notification) {
    return NotificationResponse.builder()
      .id(notification.getId())
      .userId(notification.getUserId())
      .type(notification.getType())
      .channel(notification.getChannel())
      .title(notification.getTitle())
      .message(notification.getMessage())
      .read(notification.isRead())
      .createdAt(notification.getCreatedAt())
      .metadataJson(notification.getMetadataJson())
      .build();
  }
}
