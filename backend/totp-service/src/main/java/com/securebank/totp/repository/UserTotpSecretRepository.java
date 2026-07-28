package com.securebank.totp.repository;

import com.securebank.totp.entity.UserTotpSecret;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTotpSecretRepository extends JpaRepository<UserTotpSecret, UUID> {
  Optional<UserTotpSecret> findByUserId(UUID userId);
  boolean existsByUserIdAndEnabledTrue(UUID userId);
}
