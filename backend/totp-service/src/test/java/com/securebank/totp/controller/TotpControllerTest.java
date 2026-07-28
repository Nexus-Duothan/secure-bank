package com.securebank.totp.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.totp.dto.*;
import com.securebank.totp.entity.UserTotpSecret;
import com.securebank.totp.repository.UserTotpSecretRepository;
import com.securebank.totp.util.TotpEngine;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TotpControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private UserTotpSecretRepository totpRepository;

  @Autowired
  private TotpEngine totpEngine;

  private UUID userId;

  @BeforeEach
  void setUp() {
    totpRepository.deleteAll();
    userId = UUID.randomUUID();
  }

  @Test
  void testSetupTotp_Success() throws Exception {
    MvcResult setupResult = mockMvc
      .perform(post("/api/v1/totp/setup/" + userId).param("username", "testuser"))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.secretKey").exists())
      .andExpect(jsonPath("$.otpauthUrl").exists())
      .andExpect(jsonPath("$.qrCodeBase64").exists())
      .andExpect(jsonPath("$.scratchCodes.length()").value(8))
      .andReturn();

    TotpSetupResponse setupResponse = objectMapper.readValue(
      setupResult.getResponse().getContentAsString(),
      TotpSetupResponse.class
    );

    assertNotNull(setupResponse.getSecretKey());
    assertTrue(setupResponse.getQrCodeBase64().startsWith("data:image/png;base64,"));

    UserTotpSecret entity = totpRepository.findByUserId(userId).orElseThrow();
    assertFalse(entity.isEnabled());
  }

  @Test
  void testEnableAndVerifyTotp_Success() throws Exception {
    // 1. Setup 2FA
    MvcResult setupResult = mockMvc
      .perform(post("/api/v1/totp/setup/" + userId))
      .andExpect(status().isCreated())
      .andReturn();

    TotpSetupResponse setupResponse = objectMapper.readValue(
      setupResult.getResponse().getContentAsString(),
      TotpSetupResponse.class
    );

    // 2. Generate valid current TOTP code
    String currentCode = totpEngine.generateTotpForCurrentTime(setupResponse.getSecretKey());

    // 3. Enable 2FA
    TotpEnableRequest enableRequest = TotpEnableRequest.builder()
      .userId(userId)
      .totpCode(currentCode)
      .build();

    mockMvc
      .perform(
        post("/api/v1/totp/enable")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(enableRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid").value(true));

    // 4. Verify 2FA status
    mockMvc
      .perform(get("/api/v1/totp/status/" + userId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.enabled").value(true));

    // 5. Verify TOTP code verification
    TotpVerifyRequest verifyRequest = TotpVerifyRequest.builder()
      .userId(userId)
      .code(currentCode)
      .build();

    mockMvc
      .perform(
        post("/api/v1/totp/verify")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(verifyRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  void testScratchCodeVerificationAndConsumption() throws Exception {
    // 1. Setup 2FA
    MvcResult setupResult = mockMvc
      .perform(post("/api/v1/totp/setup/" + userId))
      .andExpect(status().isCreated())
      .andReturn();

    TotpSetupResponse setupResponse = objectMapper.readValue(
      setupResult.getResponse().getContentAsString(),
      TotpSetupResponse.class
    );

    String validCode = totpEngine.generateTotpForCurrentTime(setupResponse.getSecretKey());

    // 2. Enable 2FA
    mockMvc
      .perform(
        post("/api/v1/totp/enable")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              TotpEnableRequest.builder().userId(userId).totpCode(validCode).build()
            )
          )
      )
      .andExpect(status().isOk());

    String scratchCode = setupResponse.getScratchCodes().get(0);

    // 3. Verify using scratch code (1st attempt)
    mockMvc
      .perform(
        post("/api/v1/totp/verify")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              TotpVerifyRequest.builder().userId(userId).code(scratchCode).build()
            )
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.valid").value(true))
      .andExpect(jsonPath("$.usedScratchCode").value(true));

    // 4. Verify using same scratch code (2nd attempt - must fail as consumed)
    mockMvc
      .perform(
        post("/api/v1/totp/verify")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              TotpVerifyRequest.builder().userId(userId).code(scratchCode).build()
            )
          )
      )
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.valid").value(false));
  }

  @Test
  void testInvalidCode_ReturnsUnauthorized() throws Exception {
    mockMvc.perform(post("/api/v1/totp/setup/" + userId)).andExpect(status().isCreated());

    TotpVerifyRequest invalidRequest = TotpVerifyRequest.builder()
      .userId(userId)
      .code("000000")
      .build();

    mockMvc
      .perform(
        post("/api/v1/totp/verify")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(invalidRequest))
      )
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.valid").value(false));
  }
}
