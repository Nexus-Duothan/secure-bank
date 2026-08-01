package com.securebank.payments.repository;

import com.securebank.payments.entity.Merchant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, UUID> {
  Optional<Merchant> findByMerchantCode(String merchantCode);

  boolean existsByMerchantCode(String merchantCode);

  Optional<Merchant> findByMerchantUserId(UUID merchantUserId);
}
