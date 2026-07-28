package com.securebank.auth.repository;

import com.securebank.auth.entity.UserCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
  Optional<UserCredential> findByUsername(String username);
  Optional<UserCredential> findByEmail(String email);
  boolean existsByUsername(String username);
  boolean existsByEmail(String email);

  @org.springframework.data.jpa.repository.Query(
    "SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserCredential u WHERE u.nationalIdOrPassport = :nationalIdOrPassport"
  )
  boolean existsByNationalIdOrPassport(
    @org.springframework.data.repository.query.Param(
      "nationalIdOrPassport"
    ) String nationalIdOrPassport
  );
}
