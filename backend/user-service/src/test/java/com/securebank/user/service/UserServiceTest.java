package com.securebank.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.dto.ConfirmChangeRequest;
import com.securebank.user.dto.FreezeAccountRequest;
import com.securebank.user.dto.OtpChallengeResponse;
import com.securebank.user.dto.ProfileUpdateRequest;
import com.securebank.user.dto.RoleUpdateRequest;
import com.securebank.user.dto.UserProfileResponse;
import com.securebank.user.entity.PendingUserChange;
import com.securebank.user.entity.UserProfile;
import com.securebank.user.enums.ChangeRequestType;
import com.securebank.user.enums.Role;
import com.securebank.user.enums.UserStatus;
import com.securebank.user.repository.PendingUserChangeRepository;
import com.securebank.user.repository.UserDeviceRepository;
import com.securebank.user.repository.UserProfileRepository;
import com.securebank.user.security.AccessDeniedException;
import com.securebank.user.security.CallerIdentity;
import com.securebank.user.service.notification.UserSecurityAlertService;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final UUID CUSTOMER_ID = UUID.randomUUID();
  private static final UUID ADMIN_ID = UUID.randomUUID();
  private static final String KNOWN_CODE = "123456";
  private static final int MAX_ATTEMPTS = 3;

  @Mock
  private UserProfileRepository userProfileRepository;

  @Mock
  private UserDeviceRepository userDeviceRepository;

  @Mock
  private PendingUserChangeRepository pendingUserChangeRepository;

  @Mock
  private UserSecurityAlertService userSecurityAlertService;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PasswordEncoder otpEncoder = new BCryptPasswordEncoder();

  private UserService userService;

  @BeforeEach
  void setUp() {
    UserServiceProperties properties = new UserServiceProperties(
      false,
      null,
      new UserServiceProperties.Otp(Duration.ofMinutes(5), MAX_ATTEMPTS, true),
      new UserServiceProperties.Security(false)
    );
    userService = new UserService(
      userProfileRepository,
      userDeviceRepository,
      pendingUserChangeRepository,
      objectMapper,
      otpEncoder,
      properties,
      userSecurityAlertService
    );
  }

  @Nested
  @DisplayName("OTP confirmed changes (FR-07)")
  class OtpConfirmedChanges {

    @Test
    void issuesARandomCodeAndPersistsOnlyItsHash() {
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
      when(userProfileRepository.existsByEmailIgnoreCaseAndIdNot(any(), any())).thenReturn(false);
      when(pendingUserChangeRepository.save(any())).thenAnswer(call -> call.getArgument(0));

      OtpChallengeResponse challenge = userService.requestProfileUpdate(
        caller(CUSTOMER_ID, Role.CUSTOMER),
        profileUpdate("Jane Doe", "jane.doe@securebank.lk")
      );

      ArgumentCaptor<PendingUserChange> captor = ArgumentCaptor.forClass(PendingUserChange.class);
      verify(pendingUserChangeRepository).save(captor.capture());
      PendingUserChange stored = captor.getValue();

      assertThat(challenge.demoCode()).hasSize(6).containsOnlyDigits();
      assertThat(stored.getOtpHash()).isNotEqualTo(challenge.demoCode());
      assertThat(otpEncoder.matches(challenge.demoCode(), stored.getOtpHash())).isTrue();
      assertThat(challenge.deliveryTarget()).isEqualTo("j***e@securebank.lk");
    }

    @Test
    void appliesTheStagedUpdateWhenTheCodeMatches() {
      UserProfile profile = customer();
      PendingUserChange change = profileChange("{\"fullName\":\"Jane Doe\"}");
      when(
        pendingUserChangeRepository.findByIdAndUserProfileId(change.getId(), CUSTOMER_ID)
      ).thenReturn(Optional.of(change));
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(profile));
      when(userProfileRepository.save(any())).thenAnswer(call -> call.getArgument(0));
      when(
        userDeviceRepository.findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(
          CUSTOMER_ID
        )
      ).thenReturn(List.of());

      UserProfileResponse response = userService.confirmChange(
        caller(CUSTOMER_ID, Role.CUSTOMER),
        change.getId(),
        new ConfirmChangeRequest(KNOWN_CODE)
      );

      assertThat(response.fullName()).isEqualTo("Jane Doe");
      assertThat(change.isConfirmed()).isTrue();
    }

    @Test
    void countsAWrongCodeAgainstTheAttemptBudgetAndLeavesTheProfileUntouched() {
      PendingUserChange change = profileChange("{\"fullName\":\"Jane Doe\"}");
      when(
        pendingUserChangeRepository.findByIdAndUserProfileId(change.getId(), CUSTOMER_ID)
      ).thenReturn(Optional.of(change));

      assertThatThrownBy(() ->
        userService.confirmChange(
          caller(CUSTOMER_ID, Role.CUSTOMER),
          change.getId(),
          new ConfirmChangeRequest("000000")
        )
      )
        .isInstanceOf(OtpVerificationException.class)
        .hasMessageContaining("2 attempt(s) remaining");

      assertThat(change.getFailedAttempts()).isOne();
      assertThat(change.isConfirmed()).isFalse();
      verify(userProfileRepository, never()).save(any());
    }

    @Test
    void refusesAnExhaustedChallengeEvenWhenTheCodeIsCorrect() {
      PendingUserChange change = profileChange("{\"fullName\":\"Jane Doe\"}");
      change.setFailedAttempts(MAX_ATTEMPTS);
      when(
        pendingUserChangeRepository.findByIdAndUserProfileId(change.getId(), CUSTOMER_ID)
      ).thenReturn(Optional.of(change));

      assertThatThrownBy(() ->
        userService.confirmChange(
          caller(CUSTOMER_ID, Role.CUSTOMER),
          change.getId(),
          new ConfirmChangeRequest(KNOWN_CODE)
        )
      )
        .isInstanceOf(OtpVerificationException.class)
        .hasMessageContaining("Too many incorrect codes");

      verify(userProfileRepository, never()).save(any());
    }

    @Test
    void rejectsAnExpiredChallenge() {
      PendingUserChange change = profileChange("{\"fullName\":\"Jane Doe\"}");
      change.setExpiresAt(Instant.now().minusSeconds(1));
      when(
        pendingUserChangeRepository.findByIdAndUserProfileId(change.getId(), CUSTOMER_ID)
      ).thenReturn(Optional.of(change));

      assertThatThrownBy(() ->
        userService.confirmChange(
          caller(CUSTOMER_ID, Role.CUSTOMER),
          change.getId(),
          new ConfirmChangeRequest(KNOWN_CODE)
        )
      )
        .isInstanceOf(OtpVerificationException.class)
        .hasMessageContaining("expired");
    }

    @Test
    void doesNotLetOneCustomerConfirmAnothersChange() {
      UUID intruderId = UUID.randomUUID();
      UUID changeId = UUID.randomUUID();
      when(pendingUserChangeRepository.findByIdAndUserProfileId(changeId, intruderId)).thenReturn(
        Optional.empty()
      );

      assertThatThrownBy(() ->
        userService.confirmChange(
          caller(intruderId, Role.CUSTOMER),
          changeId,
          new ConfirmChangeRequest(KNOWN_CODE)
        )
      ).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void rejectsAnEmailAlreadyHeldByAnotherProfileBeforeIssuingACode() {
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
      when(
        userProfileRepository.existsByEmailIgnoreCaseAndIdNot("taken@securebank.lk", CUSTOMER_ID)
      ).thenReturn(true);

      assertThatThrownBy(() ->
        userService.requestProfileUpdate(
          caller(CUSTOMER_ID, Role.CUSTOMER),
          profileUpdate(null, "taken@securebank.lk")
        )
      ).isInstanceOf(ConflictException.class);

      verify(pendingUserChangeRepository, never()).save(any());
    }
  }

  @Nested
  @DisplayName("Account freeze and unfreeze (FR-13)")
  class FreezeControls {

    @Test
    void freezeRecordsTheReasonAndUnfreezeClearsIt() {
      UserProfile profile = customer();
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(profile));
      when(userProfileRepository.save(any())).thenAnswer(call -> call.getArgument(0));
      when(
        userDeviceRepository.findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(
          CUSTOMER_ID
        )
      ).thenReturn(List.of());

      PendingUserChange freeze = change(
        ChangeRequestType.FREEZE_ACCOUNT,
        "{\"reason\":\"Card lost\"}"
      );
      when(
        pendingUserChangeRepository.findByIdAndUserProfileId(freeze.getId(), CUSTOMER_ID)
      ).thenReturn(Optional.of(freeze));

      UserProfileResponse frozen = userService.confirmChange(
        caller(CUSTOMER_ID, Role.CUSTOMER),
        freeze.getId(),
        new ConfirmChangeRequest(KNOWN_CODE)
      );
      assertThat(frozen.frozen()).isTrue();
      assertThat(frozen.status()).isEqualTo(UserStatus.FROZEN);
      assertThat(frozen.freezeReason()).isEqualTo("Card lost");
      assertThat(profile.getFrozenAt()).isNotNull();

      PendingUserChange unfreeze = change(ChangeRequestType.UNFREEZE_ACCOUNT, "{}");
      when(
        pendingUserChangeRepository.findByIdAndUserProfileId(unfreeze.getId(), CUSTOMER_ID)
      ).thenReturn(Optional.of(unfreeze));

      UserProfileResponse active = userService.confirmChange(
        caller(CUSTOMER_ID, Role.CUSTOMER),
        unfreeze.getId(),
        new ConfirmChangeRequest(KNOWN_CODE)
      );
      assertThat(active.frozen()).isFalse();
      assertThat(active.status()).isEqualTo(UserStatus.ACTIVE);
      assertThat(active.freezeReason()).isNull();
      assertThat(profile.getFrozenAt()).isNull();
    }

    @Test
    void refusesToFreezeAnAlreadyFrozenAccount() {
      UserProfile profile = customer();
      profile.setFrozen(true);
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(profile));

      assertThatThrownBy(() ->
        userService.requestAccountFreeze(
          caller(CUSTOMER_ID, Role.CUSTOMER),
          new FreezeAccountRequest("duplicate")
        )
      ).isInstanceOf(ConflictException.class);
    }

    @Test
    void refusesToUnfreezeAnAccountThatIsNotFrozen() {
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));

      assertThatThrownBy(() ->
        userService.requestAccountUnfreeze(caller(CUSTOMER_ID, Role.CUSTOMER))
      ).isInstanceOf(ConflictException.class);
    }
  }

  @Nested
  @DisplayName("Role based access control (FR-08)")
  class RoleBasedAccessControl {

    @Test
    void customersCannotListOtherUsers() {
      assertThatThrownBy(() ->
        userService.getUsers(caller(CUSTOMER_ID, Role.CUSTOMER))
      ).isInstanceOf(AccessDeniedException.class);
      verify(userProfileRepository, never()).findAll();
    }

    @Test
    void bankOfficersCannotGrantRoles() {
      assertThatThrownBy(() ->
        userService.updateRole(
          caller(ADMIN_ID, Role.BANK_OFFICER),
          CUSTOMER_ID,
          new RoleUpdateRequest(Role.ADMIN)
        )
      ).isInstanceOf(AccessDeniedException.class);
      verify(userProfileRepository, never()).save(any());
    }

    @Test
    void administratorsCannotRegradeThemselves() {
      assertThatThrownBy(() ->
        userService.updateRole(
          caller(ADMIN_ID, Role.ADMIN),
          ADMIN_ID,
          new RoleUpdateRequest(Role.ADMIN)
        )
      ).isInstanceOf(AccessDeniedException.class);
      verify(userProfileRepository, never()).save(any());
    }

    @Test
    void administratorsCanGradeOtherUsers() {
      UserProfile profile = customer();
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(profile));
      when(userProfileRepository.save(any())).thenAnswer(call -> call.getArgument(0));
      when(
        userDeviceRepository.findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(
          CUSTOMER_ID
        )
      ).thenReturn(List.of());

      UserProfileResponse response = userService.updateRole(
        caller(ADMIN_ID, Role.ADMIN),
        CUSTOMER_ID,
        new RoleUpdateRequest(Role.MERCHANT)
      );

      assertThat(response.role()).isEqualTo(Role.MERCHANT);
    }
  }

  // ---- fixtures ------------------------------------------------------

  private static CallerIdentity caller(UUID userId, Role role) {
    return new CallerIdentity(userId, role);
  }

  private static UserProfile customer() {
    return UserProfile.builder()
      .id(CUSTOMER_ID)
      .fullName("John Doe")
      .email("john.doe@securebank.lk")
      .phoneNumber("+94 77 123 4567")
      .language("English")
      .role(Role.CUSTOMER)
      .status(UserStatus.ACTIVE)
      .idVerified(true)
      .build();
  }

  private static ProfileUpdateRequest profileUpdate(String fullName, String email) {
    return new ProfileUpdateRequest(fullName, email, null, null, null, null, null);
  }

  private PendingUserChange profileChange(String payloadJson) {
    return change(ChangeRequestType.UPDATE_PROFILE, payloadJson);
  }

  private PendingUserChange change(ChangeRequestType type, String payloadJson) {
    return PendingUserChange.builder()
      .id(UUID.randomUUID())
      .userProfileId(CUSTOMER_ID)
      .type(type)
      .payloadJson(payloadJson)
      .otpHash(otpEncoder.encode(KNOWN_CODE))
      .expiresAt(Instant.now().plusSeconds(300))
      .build();
  }
}
