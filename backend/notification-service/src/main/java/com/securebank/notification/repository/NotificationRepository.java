package com.securebank.notification.repository;

import com.securebank.notification.entity.Notification;
import com.securebank.notification.enums.NotificationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
  Page<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

  Page<Notification> findByUserIdAndReadOrderByCreatedAtDesc(
    UUID userId,
    boolean read,
    Pageable pageable
  );

  Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(
    UUID userId,
    NotificationType type,
    Pageable pageable
  );

  long countByUserIdAndReadFalse(UUID userId);

  @Modifying
  @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
  int markAllAsReadForUser(UUID userId);
}
