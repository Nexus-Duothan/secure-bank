package com.securebank.auth.config;

import com.securebank.auth.entity.UserCredential;
import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import com.securebank.auth.repository.UserCredentialRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "securebank.auth", name = "seed-demo-data", havingValue = "true")
public class DemoAuthDataSeeder implements ApplicationRunner {

  public static final UUID DEMO_CUSTOMER_ID = UUID.fromString(
    "8f7113d3-9f5b-4a7b-88fd-0a0b7854b1d1"
  );
  public static final String DEMO_CUSTOMER_USERNAME = "kaveesha.demo";
  public static final String DEMO_CUSTOMER_EMAIL = "kaveesha.kapitiarachchi@securebank.lk";
  public static final String DEMO_CUSTOMER_PASSWORD = "SecureBank@123";

  private static final UUID DEMO_ADMIN_ID = UUID.fromString("36c2864d-f993-4781-a750-f5ec4bc5db02");
  private static final String DEMO_ADMIN_USERNAME = "nimali.admin";
  private static final String DEMO_ADMIN_EMAIL = "nimali.perera@securebank.lk";
  private static final String DEMO_ADMIN_PASSWORD = "SecureBankAdmin@123";
  private static final Logger log = LoggerFactory.getLogger(DemoAuthDataSeeder.class);

  private final UserCredentialRepository userCredentialRepository;
  private final JdbcTemplate jdbcTemplate;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    SeededUser customer = upsertUser(
      DEMO_CUSTOMER_ID,
      DEMO_CUSTOMER_USERNAME,
      DEMO_CUSTOMER_EMAIL,
      DEMO_CUSTOMER_PASSWORD,
      "200308801234",
      "Kaveesha Kapitiarachchi",
      "+94 77 510 6101",
      Role.CUSTOMER
    );

    SeededUser admin = upsertUser(
      DEMO_ADMIN_ID,
      DEMO_ADMIN_USERNAME,
      DEMO_ADMIN_EMAIL,
      DEMO_ADMIN_PASSWORD,
      "198412345678",
      "Nimali Perera",
      "+94 77 900 1122",
      Role.ADMIN
    );

    log.info(
      "Seeded auth demo users: customer={} username={} admin={} username={}",
      customer.id(),
      customer.username(),
      admin.id(),
      admin.username()
    );
  }

  private SeededUser upsertUser(
    UUID id,
    String username,
    String email,
    String rawPassword,
    String nationalIdOrPassport,
    String fullName,
    String phoneNumber,
    Role role
  ) {
    UserCredential existing = userCredentialRepository.findByUsername(username).orElse(null);

    if (existing == null) {
      jdbcTemplate.update(
        """
        insert into users (
          id,
          username,
          email,
          password_hash,
          national_id_or_passport,
          full_name,
          phone_number,
          role,
          status,
          mfa_enabled,
          created_at,
          updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        id,
        username,
        email,
        passwordEncoder.encode(rawPassword),
        nationalIdOrPassport,
        fullName,
        phoneNumber,
        role.name(),
        UserStatus.ACTIVE.name(),
        true
      );
      return new SeededUser(id, username);
    }

    jdbcTemplate.update(
      """
      update users
      set email = ?,
          password_hash = ?,
          national_id_or_passport = ?,
          full_name = ?,
          phone_number = ?,
          role = ?,
          status = ?,
          mfa_enabled = ?,
          updated_at = current_timestamp
      where id = ?
      """,
      email,
      passwordEncoder.encode(rawPassword),
      nationalIdOrPassport,
      fullName,
      phoneNumber,
      role.name(),
      UserStatus.ACTIVE.name(),
      true,
      existing.getId()
    );
    return new SeededUser(existing.getId(), username);
  }

  private record SeededUser(UUID id, String username) {}
}
