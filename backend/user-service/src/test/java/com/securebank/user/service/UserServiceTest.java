package com.securebank.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.user.client.CredentialAccessClient;
import com.securebank.user.client.TotpClient;
import com.securebank.user.config.UserServiceProperties;
import com.securebank.user.dto.ConfirmChangeRequest;
import com.securebank.user.dto.OtpChallengeResponse;
import com.securebank.user.dto.ProfileUpdateRequest;
import com.securebank.user.dto.RoleUpdateRequest;
import com.securebank.user.dto.StatusUpdateRequest;
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

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final UUID CUSTOMER_ID = UUID.randomUUID();
  private static final UUID ADMIN_ID = UUID.randomUUID();
  /** The code the customer's authenticator app is showing in these tests. */
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

  @Mock
  private TotpClient totpClient;

  @Mock
  private CredentialAccessClient credentialAccessClient;

  @Mock
  private ServiceHealthProbe serviceHealthProbe;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private UserService userService;

  @BeforeEach
  void setUp() {
    UserServiceProperties properties = new UserServiceProperties(
      null,
      new UserServiceProperties.Otp(Duration.ofMinutes(5), MAX_ATTEMPTS),
      new UserServiceProperties.Security(false)
    );
    userService = new UserService(
      userProfileRepository,
      userDeviceRepository,
      pendingUserChangeRepository,
      objectMapper,
      properties,
      userSecurityAlertService,
      totpClient,
      credentialAccessClient,
      serviceHealthProbe
    );
  }

  @Nested
  @DisplayName("TOTP confirmed changes")
  class TotpConfirmedChanges {

    @Test
    void stagesTheChangeWithoutGeneratingOrSendingAnyCode() {
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(customer()));
      when(userProfileRepository.existsByEmailIgnoreCaseAndIdNot(any(), any())).thenReturn(false);
      when(pendingUserChangeRepository.save(any())).thenAnswer(call -> call.getArgument(0));

      OtpChallengeResponse challenge = userService.requestProfileUpdate(
        caller(CUSTOMER_ID, Role.CUSTOMER),
        profileUpdate("Jane Doe", "jane.doe@securebank.lk")
      );

      verify(pendingUserChangeRepository).save(any(PendingUserChange.class));

      assertThat(challenge.demoCode()).isNull();
      assertThat(challenge.deliveryTarget()).isEqualTo("Authenticator app");
      assertThat(challenge.message()).contains("authenticator app");
      verifyNoInteractions(userSecurityAlertService);
    }

    @Test
    void appliesTheStagedUpdateWhenTheAuthenticatorCodeMatches() {
      UserProfile profile = customer();
      when(totpClient.verify(CUSTOMER_ID, KNOWN_CODE)).thenReturn(true);
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
  @DisplayName("Role based access control")
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
      // The profile row alone changes nothing the user can feel: sign-in reads auth-service.
      verify(credentialAccessClient).updateAccess(CUSTOMER_ID, null, Role.MERCHANT);
    }

    @Test
    void suspendingAUserIsPushedToTheServiceThatOwnsSignIn() {
      UserProfile profile = customer();
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(profile));
      when(userProfileRepository.save(any())).thenAnswer(call -> call.getArgument(0));
      when(
        userDeviceRepository.findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(
          CUSTOMER_ID
        )
      ).thenReturn(List.of());

      UserProfileResponse response = userService.updateStatus(
        caller(ADMIN_ID, Role.ADMIN),
        CUSTOMER_ID,
        new StatusUpdateRequest(UserStatus.SUSPENDED)
      );

      assertThat(response.status()).isEqualTo(UserStatus.SUSPENDED);
      verify(credentialAccessClient).updateAccess(CUSTOMER_ID, UserStatus.SUSPENDED, null);
    }

    @Test
    void mirroringAStatusFromAuthServiceDoesNotPushItStraightBack() {
      UserProfile profile = customer();
      when(userProfileRepository.findById(CUSTOMER_ID)).thenReturn(Optional.of(profile));
      when(userProfileRepository.save(any())).thenAnswer(call -> call.getArgument(0));
      when(
        userDeviceRepository.findByUserProfileIdAndRevokedAtIsNullOrderByLastVerifiedAtDesc(
          CUSTOMER_ID
        )
      ).thenReturn(List.of());

      UserProfileResponse response = userService.syncProfileStatus(CUSTOMER_ID, UserStatus.ACTIVE);

      assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
      verify(credentialAccessClient, never()).updateAccess(any(), any(), any());
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
      .expiresAt(Instant.now().plusSeconds(300))
      .build();
  }
}
