package com.securebank.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.auth.dto.KycApplicationResponse;
import com.securebank.auth.dto.KycSubmissionRequest;
import com.securebank.auth.dto.LoginRequest;
import com.securebank.auth.dto.MfaVerifyRequest;
import com.securebank.auth.dto.OfficerKycReviewRequest;
import com.securebank.auth.dto.PreAuthResponse;
import com.securebank.auth.dto.RegisterRequest;
import com.securebank.auth.entity.UserCredential;
import com.securebank.auth.enums.DocumentType;
import com.securebank.auth.enums.KycStatus;
import com.securebank.auth.enums.Role;
import com.securebank.auth.enums.UserStatus;
import com.securebank.auth.repository.UserCredentialRepository;
import com.securebank.auth.security.JwtTokenProvider;
import com.securebank.auth.service.notification.LoginSmsAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserCredentialRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtTokenProvider tokenProvider;

  @SpyBean
  private LoginSmsAlertService loginSmsAlertService;

  private UserCredential testUser;
  private UserCredential officerUser;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    testUser = userRepository.save(
      UserCredential.builder()
        .username("testuser")
        .email("testuser@securebank.com")
        .passwordHash(passwordEncoder.encode("Secret123!"))
        .nationalIdOrPassport("NIC-987654321V")
        .fullName("Test Customer")
        .phoneNumber("+94 77 123 4567")
        .role(Role.CUSTOMER)
        .status(UserStatus.PENDING_KYC)
        .mfaEnabled(true)
        .build()
    );

    officerUser = userRepository.save(
      UserCredential.builder()
        .username("officer1")
        .email("officer1@securebank.com")
        .passwordHash(passwordEncoder.encode("Officer123!"))
        .nationalIdOrPassport("OFFICER-001")
        .fullName("Bank Officer One")
        .phoneNumber("+94 77 900 1122")
        .role(Role.BANK_OFFICER)
        .status(UserStatus.ACTIVE)
        .mfaEnabled(true)
        .build()
    );
  }

  @Test
  void testRegisterUser_Success() throws Exception {
    RegisterRequest request = RegisterRequest.builder()
      .username("newuser")
      .email("newuser@securebank.com")
      .password("Password123!")
      .nationalIdOrPassport("NIC-11223344")
      .fullName("New Customer")
      .phoneNumber("+94 77 555 0101")
      .build();

    mockMvc
      .perform(
        post("/api/v1/auth/register")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.username").value("newuser"))
      .andExpect(jsonPath("$.status").value("PENDING_KYC"));
  }

  @Test
  void testTwoStepLogin_Success() throws Exception {
    LoginRequest loginRequest = LoginRequest.builder()
      .usernameOrEmail("testuser")
      .password("Secret123!")
      .build();

    MvcResult loginResult = mockMvc
      .perform(
        post("/api/v1/auth/login")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(loginRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.mfaRequired").value(true))
      .andExpect(jsonPath("$.preAuthToken").exists())
      .andReturn();

    PreAuthResponse preAuth = objectMapper.readValue(
      loginResult.getResponse().getContentAsString(),
      PreAuthResponse.class
    );

    MfaVerifyRequest mfaRequest = MfaVerifyRequest.builder()
      .preAuthToken(preAuth.getPreAuthToken())
      .totpCode("123456")
      .build();

    mockMvc
      .perform(
        post("/api/v1/auth/login/verify-mfa")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(mfaRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.accessToken").exists())
      .andExpect(jsonPath("$.refreshToken").exists())
      .andExpect(jsonPath("$.tokenType").value("Bearer"));

    verify(loginSmsAlertService).sendSuccessfulLoginAlert(
      eq("+94 77 123 4567"),
      eq("Test Customer"),
      contains("127."),
      eq("Unknown Device"),
      any()
    );
  }

  @Test
  void testKycSubmissionAndOfficerReviewFlow() throws Exception {
    String token = tokenProvider.generateAccessToken(
      testUser.getId(),
      testUser.getUsername(),
      testUser.getRole(),
      testUser.getStatus()
    );

    KycSubmissionRequest kycRequest = KycSubmissionRequest.builder()
      .documentType(DocumentType.NATIONAL_ID)
      .documentNumber("NIC-987654321V")
      .documentPayload("base64EncodedDocumentDataSample")
      .build();

    MvcResult kycResult = mockMvc
      .perform(
        post("/api/v1/auth/verify-kyc")
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(kycRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
      .andReturn();

    KycApplicationResponse kycApp = objectMapper.readValue(
      kycResult.getResponse().getContentAsString(),
      KycApplicationResponse.class
    );

    String officerToken = tokenProvider.generateAccessToken(
      officerUser.getId(),
      officerUser.getUsername(),
      officerUser.getRole(),
      officerUser.getStatus()
    );

    mockMvc
      .perform(
        get("/api/v1/auth/officer/kyc/pending").header("Authorization", "Bearer " + officerToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].applicationId").value(kycApp.getApplicationId().toString()));

    OfficerKycReviewRequest reviewRequest = OfficerKycReviewRequest.builder()
      .action(KycStatus.APPROVED)
      .build();

    mockMvc
      .perform(
        post("/api/v1/auth/officer/kyc/" + kycApp.getApplicationId() + "/review")
          .header("Authorization", "Bearer " + officerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(reviewRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("APPROVED"));

    UserCredential updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
    assertEquals(UserStatus.ACTIVE, updatedUser.getStatus());
  }

  @Test
  void testTokenValidationEndpoint() throws Exception {
    String token = tokenProvider.generateAccessToken(
      testUser.getId(),
      testUser.getUsername(),
      testUser.getRole(),
      testUser.getStatus()
    );

    mockMvc
      .perform(get("/api/v1/auth/validate").param("token", token))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid").value(true))
      .andExpect(jsonPath("$.username").value("testuser"));
  }
}
