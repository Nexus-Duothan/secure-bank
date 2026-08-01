package com.securebank.transfer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.transfer.dto.AddPayeeRequest;
import com.securebank.transfer.dto.ConfirmPayeeRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PayeeControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${jwt.secret}")
  private String jwtSecret;

  private final UUID userId = UUID.randomUUID();

  private String accessToken() {
    SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    Date now = new Date();
    return Jwts.builder()
      .subject(userId.toString())
      .claim("type", "ACCESS")
      .issuedAt(now)
      .expiration(new Date(now.getTime() + 300_000))
      .signWith(key)
      .compact();
  }

  @Test
  void listPayees_returnsEmptyList_initially() throws Exception {
    mockMvc
      .perform(
        get("/api/v1/transfers/payees").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void requestAddPayee_thenConfirm_createsPayeeWithCoolingOff() throws Exception {
    AddPayeeRequest addRequest = new AddPayeeRequest("A. Silva", "acc-other");

    String challengeBody = mockMvc
      .perform(
        post("/api/v1/transfers/payees")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(addRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.demoCode").exists())
      .andReturn()
      .getResponse()
      .getContentAsString();

    var challenge = objectMapper.readTree(challengeBody);
    String changeRequestId = challenge.get("changeRequestId").asText();
    String demoCode = challenge.get("demoCode").asText();

    mockMvc
      .perform(
        post("/api/v1/transfers/payees/{id}/confirm", changeRequestId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(new ConfirmPayeeRequest(demoCode)))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.nickname").value("A. Silva"))
      .andExpect(jsonPath("$.accountReference").value("acc-other"))
      .andExpect(jsonPath("$.coolingOff").value(true));

    mockMvc
      .perform(
        get("/api/v1/transfers/payees").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void confirmAddPayee_rejectsWrongCode() throws Exception {
    AddPayeeRequest addRequest = new AddPayeeRequest("A. Silva", "acc-other");

    String challengeBody = mockMvc
      .perform(
        post("/api/v1/transfers/payees")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(addRequest))
      )
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = objectMapper.readTree(challengeBody).get("changeRequestId").asText();

    mockMvc
      .perform(
        post("/api/v1/transfers/payees/{id}/confirm", changeRequestId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(new ConfirmPayeeRequest("000000")))
      )
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void requestAddPayee_isRejected_withoutAnAccessToken() throws Exception {
    AddPayeeRequest addRequest = new AddPayeeRequest("A. Silva", "acc-other");

    mockMvc
      .perform(
        post("/api/v1/transfers/payees")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(addRequest))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void removePayee_returnsNotFound_whenPayeeDoesNotExist() throws Exception {
    mockMvc
      .perform(
        delete("/api/v1/transfers/payees/{id}", UUID.randomUUID()).header(
          HttpHeaders.AUTHORIZATION,
          "Bearer " + accessToken()
        )
      )
      .andExpect(status().isNotFound());
  }
}
