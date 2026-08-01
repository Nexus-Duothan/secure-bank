package com.securebank.notification.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.notification.dto.SendNotificationRequest;
import com.securebank.notification.entity.Notification;
import com.securebank.notification.enums.NotificationChannel;
import com.securebank.notification.enums.NotificationType;
import com.securebank.notification.repository.NotificationRepository;
import com.securebank.notification.security.JwtTokenProvider;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private JwtTokenProvider tokenProvider;

  @Autowired
  private ObjectMapper objectMapper;

  private UUID userId;
  private String userToken;
  private String adminToken;

  @BeforeEach
  void setUp() {
    notificationRepository.deleteAll();
    userId = UUID.randomUUID();

    userToken = generateToken(userId.toString(), "USER");
    adminToken = generateToken(UUID.randomUUID().toString(), "ADMIN");
  }

  private String generateToken(String subject, String role) {
    return io.jsonwebtoken.Jwts.builder()
      .subject(subject)
      .claim("role", role)
      .expiration(new java.util.Date(System.currentTimeMillis() + 3600000))
      .signWith(
        io.jsonwebtoken.security.Keys.hmacShaKeyFor(
          "dGhpc0lzQVZlcnlTZWN1cmVTZWNyZXRLZXlGb3JTZWN1cmVCYW5rSkdUVG9rZW5zMjAyNiE=".getBytes()
        )
      )
      .compact();
  }

  @Test
  void getNotifications_ReturnsUserNotifications() throws Exception {
    Notification n1 = notificationRepository.save(
      Notification.builder()
        .userId(userId)
        .type(NotificationType.TRANSACTION_ALERT)
        .channel(NotificationChannel.IN_APP)
        .title("Transfer Received")
        .message("You received LKR 5000.")
        .read(false)
        .build()
    );

    mockMvc
      .perform(get("/api/v1/notifications").header("Authorization", "Bearer " + userToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content", hasSize(1)))
      .andExpect(jsonPath("$.content[0].id", is(n1.getId().toString())))
      .andExpect(jsonPath("$.content[0].title", is("Transfer Received")));
  }

  @Test
  void getUnreadCount_ReturnsCorrectCount() throws Exception {
    notificationRepository.save(
      Notification.builder()
        .userId(userId)
        .type(NotificationType.SECURITY_ALERT)
        .channel(NotificationChannel.SMS)
        .title("Failed Login")
        .message("A failed login attempt was detected.")
        .read(false)
        .build()
    );

    mockMvc
      .perform(
        get("/api/v1/notifications/unread-count").header("Authorization", "Bearer " + userToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.unreadCount", is(1)));
  }

  @Test
  void markAsRead_UpdatesReadStatus() throws Exception {
    Notification n1 = notificationRepository.save(
      Notification.builder()
        .userId(userId)
        .type(NotificationType.ACCOUNT_ALERT)
        .channel(NotificationChannel.IN_APP)
        .title("Profile Updated")
        .message("Your profile details were updated.")
        .read(false)
        .build()
    );

    mockMvc
      .perform(
        patch("/api/v1/notifications/" + n1.getId() + "/read").header(
          "Authorization",
          "Bearer " + userToken
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.read", is(true)));
  }

  @Test
  void markAllAsRead_ClearsUnreadNotifications() throws Exception {
    notificationRepository.save(
      Notification.builder()
        .userId(userId)
        .type(NotificationType.TRANSACTION_ALERT)
        .channel(NotificationChannel.EMAIL)
        .title("Alert 1")
        .message("Msg 1")
        .read(false)
        .build()
    );

    mockMvc
      .perform(
        post("/api/v1/notifications/read-all").header("Authorization", "Bearer " + userToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.unreadCount", is(0)));
  }

  @Test
  void sendNotification_AdminDirectSend_Success() throws Exception {
    SendNotificationRequest request = SendNotificationRequest.builder()
      .userId(userId)
      .type(NotificationType.SECURITY_ALERT)
      .channel(NotificationChannel.SMS)
      .title("Direct Alert")
      .message("Direct security alert message.")
      .build();

    mockMvc
      .perform(
        post("/api/v1/notifications/send")
          .header("Authorization", "Bearer " + adminToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.title", is("Direct Alert")));
  }
}
