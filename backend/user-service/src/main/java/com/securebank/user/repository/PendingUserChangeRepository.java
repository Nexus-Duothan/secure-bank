package com.securebank.user.repository;

import com.securebank.user.entity.PendingUserChange;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PendingUserChangeRepository extends JpaRepository<PendingUserChange, UUID> {
  /**
   * Scoped lookup: a challenge can only be confirmed by the profile that raised it, so a leaked
   * change id is useless to another caller.
   */
  Optional<PendingUserChange> findByIdAndUserProfileId(UUID id, UUID userProfileId);

  @Modifying
  @Query("delete from PendingUserChange c where c.confirmed = false and c.expiresAt < :cutoff")
  int deleteUnconfirmedExpiredBefore(Instant cutoff);
}
