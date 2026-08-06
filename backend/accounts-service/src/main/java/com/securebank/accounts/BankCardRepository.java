package com.securebank.accounts;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCardRepository extends JpaRepository<BankCardEntity, String> {
  List<BankCardEntity> findByAccountIdOrderByCreatedAtAsc(String accountId);

  /** Only a card the bank has issued and nobody has claimed yet can be linked. */
  Optional<BankCardEntity> findByCardNumberAndUserIdIsNull(String cardNumber);
}
