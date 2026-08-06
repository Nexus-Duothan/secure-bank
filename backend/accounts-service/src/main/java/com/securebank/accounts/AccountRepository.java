package com.securebank.accounts;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<AccountEntity, String> {
  List<AccountEntity> findByUserIdOrderByCreatedAtAsc(UUID userId);

  Optional<AccountEntity> findByIdAndUserId(String id, UUID userId);

  Optional<AccountEntity> findFirstByUserIdOrderByCreatedAtAsc(UUID userId);

  /** Only an account the bank has issued and nobody has claimed yet can be linked. */
  Optional<AccountEntity> findByAccountNumberAndUserIdIsNull(String accountNumber);

  Optional<AccountEntity> findByAccountNumber(String accountNumber);

  boolean existsByAccountNumber(String accountNumber);

  /** Locks the row so two concurrent movements cannot post against the same balance. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AccountEntity a where a.id = :id")
  Optional<AccountEntity> findByIdForUpdate(@Param("id") String id);
}
