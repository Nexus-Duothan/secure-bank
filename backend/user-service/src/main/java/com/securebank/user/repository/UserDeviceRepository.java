package com.securebank.user.repository;

import com.securebank.user.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
  List<UserDevice> findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(UUID userId);

  Optional<UserDevice> findByIdAndUserProfileId(UUID id, UUID userId);
}
