package com.securebank.lending.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.lending.client.FakeAccountsClient;
import com.securebank.lending.dto.*;
import com.securebank.lending.enums.Role;
import com.securebank.lending.enums.UserStatus;
import com.securebank.lending.repository.LoanApplicationRepository;
import com.securebank.lending.repository.LoanInstallmentRepository;
import com.securebank.lending.repository.LoanRepository;
import com.securebank.lending.security.JwtTokenProvider;
import java.math.BigDecimal;
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
class LoanControllerTest {

  private static final String ACCOUNT_ID = "acc-demo-primary";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private JwtTokenProvider tokenProvider;

  @Autowired
  private LoanApplicationRepository loanApplicationRepository;

  @Autowired
  private LoanRepository loanRepository;

  @Autowired
  private LoanInstallmentRepository loanInstallmentRepository;

  private UUID customerId;
  private UUID officerId;
  private String customerToken;
  private String officerToken;

  @BeforeEach
  void setUp() {
    loanInstallmentRepository.deleteAll();
    loanRepository.deleteAll();
    loanApplicationRepository.deleteAll();
    FakeAccountsClient.reset();

    customerId = UUID.randomUUID();
    officerId = UUID.randomUUID();

    customerToken = tokenProvider.generateAccessToken(
      customerId,
      "customer1",
      Role.CUSTOMER,
      UserStatus.ACTIVE
    );
    officerToken = tokenProvider.generateAccessToken(
      officerId,
      "officer1",
      Role.BANK_OFFICER,
      UserStatus.ACTIVE
    );
  }

  private LoanApplicationRequest applicationRequest() {
    return new LoanApplicationRequest("home-improvement", new BigDecimal("120000"), 12, ACCOUNT_ID);
  }

  private String submitApplication() throws Exception {
    MvcResult result = mockMvc
      .perform(
        post("/api/v1/loans/apply")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(applicationRequest()))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("UNDER_REVIEW"))
      .andReturn();

    return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
  }

  @Test
  void apply_withoutToken_isRejected() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/loans/apply")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(applicationRequest()))
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void apply_amountAboveLimit_isRejected() throws Exception {
    LoanApplicationRequest request = new LoanApplicationRequest(
      "personal",
      new BigDecimal("10000000"),
      12,
      ACCOUNT_ID
    );

    mockMvc
      .perform(
        post("/api/v1/loans/apply")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void officerPendingList_requiresOfficerRole() throws Exception {
    mockMvc
      .perform(
        get("/api/v1/loans/officer/pending").header("Authorization", "Bearer " + customerToken)
      )
      .andExpect(status().isForbidden());
  }

  @Test
  void fullLifecycle_applyReviewDisburseAndPay() throws Exception {
    String applicationId = submitApplication();

    mockMvc
      .perform(
        get("/api/v1/loans/officer/pending").header("Authorization", "Bearer " + officerToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(applicationId));

    LoanApplicationReviewRequest approve = new LoanApplicationReviewRequest(true, null);
    MvcResult reviewResult = mockMvc
      .perform(
        post("/api/v1/loans/officer/" + applicationId + "/review")
          .header("Authorization", "Bearer " + officerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(approve))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("DISBURSED"))
      .andExpect(jsonPath("$.loanId").exists())
      .andReturn();

    String loanId = objectMapper
      .readTree(reviewResult.getResponse().getContentAsString())
      .get("loanId")
      .asText();

    mockMvc
      .perform(get("/api/v1/loans/" + loanId).header("Authorization", "Bearer " + customerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.installmentsTotal").value(12))
      .andExpect(jsonPath("$.installmentsPaid").value(0))
      .andExpect(jsonPath("$.remainingBalance").value(120000));

    mockMvc
      .perform(
        get("/api/v1/loans/" + loanId + "/installments").header(
          "Authorization",
          "Bearer " + customerToken
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(12))
      .andExpect(jsonPath("$[0].installmentNumber").value(1));

    mockMvc
      .perform(
        post("/api/v1/loans/" + loanId + "/pay").header("Authorization", "Bearer " + customerToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.installmentsPaid").value(1));

    mockMvc
      .perform(
        patch("/api/v1/loans/" + loanId + "/autopay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(new AutoPayUpdateRequest(false)))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.autopayEnabled").value(false));
  }

  @Test
  void payNow_failsInstallment_whenBalanceIsInsufficient() throws Exception {
    String applicationId = submitApplication();
    FakeAccountsClient.setBalance(ACCOUNT_ID, new BigDecimal("10"));

    MvcResult reviewResult = mockMvc
      .perform(
        post("/api/v1/loans/officer/" + applicationId + "/review")
          .header("Authorization", "Bearer " + officerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(new LoanApplicationReviewRequest(true, null)))
      )
      .andExpect(status().isOk())
      .andReturn();
    String loanId = objectMapper
      .readTree(reviewResult.getResponse().getContentAsString())
      .get("loanId")
      .asText();

    mockMvc
      .perform(
        post("/api/v1/loans/" + loanId + "/pay").header("Authorization", "Bearer " + customerToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.installmentsPaid").value(0));

    mockMvc
      .perform(
        get("/api/v1/loans/" + loanId + "/installments").header(
          "Authorization",
          "Bearer " + customerToken
        )
      )
      .andExpect(jsonPath("$[0].status").value("FAILED"));
  }

  @Test
  void reviewApplication_reject_setsReasonAndStatus() throws Exception {
    String applicationId = submitApplication();

    mockMvc
      .perform(
        post("/api/v1/loans/officer/" + applicationId + "/review")
          .header("Authorization", "Bearer " + officerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              new LoanApplicationReviewRequest(false, "Insufficient income")
            )
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("REJECTED"))
      .andExpect(jsonPath("$.rejectionReason").value("Insufficient income"));
  }

  @Test
  void getApplication_notOwner_returnsNotFound() throws Exception {
    String applicationId = submitApplication();
    String otherToken = tokenProvider.generateAccessToken(
      UUID.randomUUID(),
      "customer2",
      Role.CUSTOMER,
      UserStatus.ACTIVE
    );

    mockMvc
      .perform(
        get("/api/v1/loans/applications/" + applicationId).header(
          "Authorization",
          "Bearer " + otherToken
        )
      )
      .andExpect(status().isNotFound());
  }
}
