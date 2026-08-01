package com.securebank.user.config;

import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import com.securebank.user.repository.UserProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the walkthrough accounts used by the web prototype.
 *
 * <p>Runs as an {@link ApplicationRunner} rather than from {@code @PostConstruct}: Spring applies
 * {@code @Transactional} through a proxy that is not yet in place during bean initialisation, so a
 * transactional post-construct hook runs with no transaction at all.
 *
 * <p>Two profiles are created because role administration (FR-08) is only demonstrable when an
 * administrator and a subject customer both exist. The customer is written first so it remains the
 * profile that the unauthenticated development fallback resolves to.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "securebank.user", name = "seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

  private static final java.util.UUID DEMO_CUSTOMER_ID = java.util.UUID.fromString(
    "8f7113d3-9f5b-4a7b-88fd-0a0b7854b1d1"
  );
  private static final java.util.UUID DEMO_ADMIN_ID = java.util.UUID.fromString(
    "36c2864d-f993-4781-a750-f5ec4bc5db02"
  );
  private static final String DEMO_CUSTOMER_EMAIL = "kaveesha.kapitiarachchi@securebank.lk";
  private static final String DEMO_ADMIN_EMAIL = "nimali.perera@securebank.lk";
  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private final UserProfileRepository userProfileRepository;
  private final JdbcTemplate jdbcTemplate;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    java.util.UUID customerId = upsertCustomer();
    java.util.UUID administratorId = upsertAdministrator();

    log.info("Seeded demo profiles: customer={} administrator={}", customerId, administratorId);
  }

  private java.util.UUID upsertCustomer() {
    java.util.UUID customerId = userProfileRepository
      .findByEmailIgnoreCase(DEMO_CUSTOMER_EMAIL)
      .map(UserProfile::getId)
      .orElse(DEMO_CUSTOMER_ID);

    upsertProfile(
      customerId,
      "Kaveesha Kapitiarachchi",
      DEMO_CUSTOMER_EMAIL,
      "+94 77 510 6101",
      "42 Lake Drive",
      "Kandy",
      "Sri Lanka",
      "English",
      Role.CUSTOMER
    );

    jdbcTemplate.update("delete from user_devices where user_profile_id = ?", customerId);
    insertDevice(
      java.util.UUID.fromString("1eb1f0d8-9f2b-4911-8f04-21387ad42c01"),
      customerId,
      "Galaxy S24",
      "Primary Mobile",
      "SecureBank App",
      "Kandy, LK",
      true,
      Instant.now().minus(5, ChronoUnit.MINUTES)
    );
    insertDevice(
      java.util.UUID.fromString("6d89f88f-c757-45ee-b6a4-1bc2d76f44f4"),
      customerId,
      "Chrome on Windows",
      "Desktop",
      "Chrome",
      "Colombo, LK",
      true,
      Instant.now().minus(2, ChronoUnit.HOURS)
    );
    insertDevice(
      java.util.UUID.fromString("9f8a3f2f-9e3d-4872-9f72-1682d83f2b2e"),
      customerId,
      "SecureBank iPhone",
      "Mobile",
      "Mobile App",
      "Kandy, LK",
      true,
      Instant.now().minus(3, ChronoUnit.DAYS)
    );
    return customerId;
  }

  private java.util.UUID upsertAdministrator() {
    java.util.UUID administratorId = userProfileRepository
      .findByEmailIgnoreCase(DEMO_ADMIN_EMAIL)
      .map(UserProfile::getId)
      .orElse(DEMO_ADMIN_ID);

    upsertProfile(
      administratorId,
      "Nimali Perera",
      DEMO_ADMIN_EMAIL,
      "+94 77 900 1122",
      "SecureBank Operations Centre",
      "Colombo",
      "Sri Lanka",
      "English",
      Role.ADMIN
    );
    return administratorId;
  }

  private void upsertProfile(
    java.util.UUID id,
    String fullName,
    String email,
    String phoneNumber,
    String addressLine,
    String city,
    String country,
    String language,
    Role role
  ) {
    int updated = jdbcTemplate.update(
      """
      update user_profiles
      set full_name = ?,
          email = ?,
          phone_number = ?,
          address_line = ?,
          city = ?,
          country = ?,
          language = ?,
          role = ?,
          status = ?,
          id_verified = ?,
          email_notifications = ?,
          sms_notifications = ?,
          push_notifications = ?,
          frozen = ?,
          freeze_reason = ?,
          frozen_at = ?,
          updated_at = current_timestamp
      where id = ?
      """,
      fullName,
      email,
      phoneNumber,
      addressLine,
      city,
      country,
      language,
      role.name(),
      UserStatus.ACTIVE.name(),
      true,
      true,
      true,
      true,
      false,
      null,
      null,
      id
    );

    if (updated == 0) {
      jdbcTemplate.update(
        """
        insert into user_profiles (
          id,
          full_name,
          email,
          phone_number,
          address_line,
          city,
          country,
          language,
          role,
          status,
          id_verified,
          email_notifications,
          sms_notifications,
          push_notifications,
          frozen,
          freeze_reason,
          frozen_at,
          created_at,
          updated_at
        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
        """,
        id,
        fullName,
        email,
        phoneNumber,
        addressLine,
        city,
        country,
        language,
        role.name(),
        UserStatus.ACTIVE.name(),
        true,
        true,
        true,
        true,
        false,
        null,
        null
      );
    }
  }

  private void insertDevice(
    java.util.UUID id,
    java.util.UUID userProfileId,
    String deviceName,
    String deviceType,
    String browser,
    String location,
    boolean trusted,
    Instant lastVerifiedAt
  ) {
    jdbcTemplate.update(
      """
      insert into user_devices (
        id,
        user_profile_id,
        device_name,
        device_type,
        browser,
        location,
        trusted,
        last_verified_at,
        created_at,
        revoked_at
      ) values (?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, ?)
      """,
      id,
      userProfileId,
      deviceName,
      deviceType,
      browser,
      location,
      trusted,
      lastVerifiedAt,
      null
    );
  }
}
