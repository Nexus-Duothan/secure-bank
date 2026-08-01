package com.securebank.transfer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.transfer.dto.CreateScheduledTransferRequest;
import com.securebank.transfer.dto.UpdateScheduleStatusRequest;
import com.securebank.transfer.enums.ScheduleFrequency;
import com.securebank.transfer.enums.ScheduleStatus;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.time.Instant;
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
class ScheduledTransferControllerTest {

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
  void create_thenList_returnsTheNewSchedule() throws Exception {
    CreateScheduledTransferRequest request = new CreateScheduledTransferRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      "rent",
      ScheduleFrequency.MONTHLY,
      Instant.now().plusSeconds(3600),
      null
    );

    String body = mockMvc
      .perform(
        post("/api/v1/transfers/scheduled")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.frequency").value("MONTHLY"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String scheduleId = objectMapper.readTree(body).get("id").asText();

    mockMvc
      .perform(
        get("/api/v1/transfers/scheduled").header(
          HttpHeaders.AUTHORIZATION,
          "Bearer " + accessToken()
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].id").value(scheduleId));
  }

  @Test
  void create_rejectsStartAtInThePast() throws Exception {
    CreateScheduledTransferRequest request = new CreateScheduledTransferRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      null,
      ScheduleFrequency.ONE_TIME,
      Instant.now().minusSeconds(3600),
      null
    );

    mockMvc
      .perform(
        post("/api/v1/transfers/scheduled")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void updateStatus_pausesThenRejectsReactivatingAfterCancel() throws Exception {
    CreateScheduledTransferRequest request = new CreateScheduledTransferRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      null,
      ScheduleFrequency.WEEKLY,
      Instant.now().plusSeconds(3600),
      null
    );
    String body = mockMvc
      .perform(
        post("/api/v1/transfers/scheduled")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();
    String scheduleId = objectMapper.readTree(body).get("id").asText();

    mockMvc
      .perform(
        patch("/api/v1/transfers/scheduled/{id}", scheduleId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              new UpdateScheduleStatusRequest(ScheduleStatus.CANCELLED)
            )
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("CANCELLED"));

    mockMvc
      .perform(
        patch("/api/v1/transfers/scheduled/{id}", scheduleId)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(new UpdateScheduleStatusRequest(ScheduleStatus.ACTIVE))
          )
      )
      .andExpect(status().isConflict());
  }

  @Test
  void create_isRejected_withoutAnAccessToken() throws Exception {
    CreateScheduledTransferRequest request = new CreateScheduledTransferRequest(
      "acc-demo-primary",
      "acc-other",
      new BigDecimal("100"),
      null,
      ScheduleFrequency.ONE_TIME,
      Instant.now().plusSeconds(3600),
      null
    );

    mockMvc
      .perform(
        post("/api/v1/transfers/scheduled")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isForbidden());
  }
}
