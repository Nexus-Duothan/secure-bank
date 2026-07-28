package com.securebank.auth.repository;

import com.securebank.auth.entity.UserSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
  List<UserSession> findByUserIdAndRevokedFalse(UUID userId);
  Optional<UserSession> findByRefreshTokenAndRevokedFalse(String refreshToken);
  Optional<UserSession> findByIdAndUserId(UUID id, UUID userId);
}
