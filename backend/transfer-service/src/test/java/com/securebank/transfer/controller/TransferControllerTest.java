package com.securebank.transfer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.transfer.client.AccountSnapshot;
import com.securebank.transfer.client.AccountsClient;
import com.securebank.transfer.dto.TransferQuoteRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransferControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @MockBean
  private AccountsClient accountsClient;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    when(accountsClient.getAccount(any())).thenReturn(
      new AccountSnapshot("acc-demo-primary", new BigDecimal("10000"), "LKR")
    );
  }

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
  void quote_returnsPendingConfirmation_whenRequestIsValid() throws Exception {
    TransferQuoteRequest request = new TransferQuoteRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      "rent"
    );

    mockMvc
      .perform(
        post("/api/v1/transfers/quote")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
      .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void quote_isRejected_withoutAnAccessToken() throws Exception {
    TransferQuoteRequest request = new TransferQuoteRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      null
    );

    mockMvc
      .perform(
        post("/api/v1/transfers/quote")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void quote_thenConfirm_completesTheTransfer() throws Exception {
    TransferQuoteRequest request = new TransferQuoteRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("50"),
      null
    );

    String quoteBody = mockMvc
      .perform(
        post("/api/v1/transfers/quote")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("PENDING_CONFIRMATION"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String transferId = objectMapper.readTree(quoteBody).get("id").asText();

    mockMvc
      .perform(
        post("/api/v1/transfers/{id}/confirm", transferId).header(
          HttpHeaders.AUTHORIZATION,
          "Bearer " + accessToken()
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.fee").value(0))
      .andExpect(jsonPath("$.currency").value("LKR"));
  }

  @Test
  void quote_rejectsAmountAboveThePerTransactionLimit() throws Exception {
    TransferQuoteRequest request = new TransferQuoteRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("5000"),
      null
    );

    mockMvc
      .perform(
        post("/api/v1/transfers/quote")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isUnprocessableEntity());
  }
}
