package com.securebank.payments.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.payments.client.FakeAuditRecoveryClient;
import com.securebank.payments.dto.*;
import com.securebank.payments.entity.Merchant;
import com.securebank.payments.enums.Role;
import com.securebank.payments.enums.UserStatus;
import com.securebank.payments.repository.MerchantRepository;
import com.securebank.payments.repository.VendorPaymentRepository;
import com.securebank.payments.security.JwtTokenProvider;
import java.math.BigDecimal;
import java.util.Base64;
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
class PaymentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private VendorPaymentRepository vendorPaymentRepository;

  @Autowired
  private MerchantRepository merchantRepository;

  @Autowired
  private JwtTokenProvider tokenProvider;

  private UUID customerId;
  private UUID merchantUserId;
  private UUID officerId;
  private String customerToken;
  private String merchantToken;
  private String officerToken;

  @BeforeEach
  void setUp() {
    vendorPaymentRepository.deleteAll();
    merchantRepository.deleteAll();
    FakeAuditRecoveryClient.reset();

    customerId = UUID.randomUUID();
    merchantUserId = UUID.randomUUID();
    officerId = UUID.randomUUID();

    customerToken = tokenProvider.generateAccessToken(
      customerId,
      "customer1",
      Role.CUSTOMER,
      UserStatus.ACTIVE
    );
    merchantToken = tokenProvider.generateAccessToken(
      merchantUserId,
      "merchant1",
      Role.MERCHANT,
      UserStatus.ACTIVE
    );
    officerToken = tokenProvider.generateAccessToken(
      officerId,
      "officer1",
      Role.BANK_OFFICER,
      UserStatus.ACTIVE
    );
  }

  private String registerMerchant(String token) throws Exception {
    MerchantRegisterRequest request = MerchantRegisterRequest.builder()
      .businessName("Keells Super")
      .category("Grocery")
      .settlementAccountId("acc-merchant-001")
      .build();

    MvcResult result = mockMvc
      .perform(
        post("/api/v1/payments/merchants/register")
          .header("Authorization", "Bearer " + token)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andReturn();

    MerchantResponse response = objectMapper.readValue(
      result.getResponse().getContentAsString(),
      MerchantResponse.class
    );
    return response.getMerchantCode();
  }

  @Test
  void testRegisterMerchant_Success() throws Exception {
    mockMvc
      .perform(
        post("/api/v1/payments/merchants/register")
          .header("Authorization", "Bearer " + merchantToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              MerchantRegisterRequest.builder()
                .businessName("Dialog Mobile")
                .settlementAccountId("acc-merchant-002")
                .build()
            )
          )
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.businessName").value("Dialog Mobile"))
      .andExpect(jsonPath("$.merchantCode").exists())
      .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void testRegisterMerchant_DuplicateForSameUser_Rejected() throws Exception {
    registerMerchant(merchantToken);

    mockMvc
      .perform(
        post("/api/v1/payments/merchants/register")
          .header("Authorization", "Bearer " + merchantToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            objectMapper.writeValueAsString(
              MerchantRegisterRequest.builder()
                .businessName("Second Shop")
                .settlementAccountId("acc-merchant-003")
                .build()
            )
          )
      )
      .andExpect(status().isBadRequest());
  }

  @Test
  void testPay_Success() throws Exception {
    String merchantCode = registerMerchant(merchantToken);

    PayRequest request = PayRequest.builder()
      .merchantCode(merchantCode)
      .amount(BigDecimal.valueOf(1500))
      .currency("LKR")
      .note("Groceries")
      .build();

    mockMvc
      .perform(
        post("/api/v1/payments/pay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.merchantCode").value(merchantCode))
      .andExpect(jsonPath("$.referenceNumber", org.hamcrest.Matchers.startsWith("RCPT-")));
  }

  @Test
  void testPay_UnknownMerchantCode_ReturnsNotFound() throws Exception {
    PayRequest request = PayRequest.builder()
      .merchantCode("MCH-DOESNOTEXIST")
      .amount(BigDecimal.valueOf(500))
      .currency("LKR")
      .build();

    mockMvc
      .perform(
        post("/api/v1/payments/pay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void testPay_InactiveMerchant_ReturnsUnprocessable() throws Exception {
    String merchantCode = registerMerchant(merchantToken);
    Merchant merchant = merchantRepository.findByMerchantCode(merchantCode).orElseThrow();
    merchant.setActive(false);
    merchantRepository.save(merchant);

    PayRequest request = PayRequest.builder()
      .merchantCode(merchantCode)
      .amount(BigDecimal.valueOf(500))
      .currency("LKR")
      .build();

    mockMvc
      .perform(
        post("/api/v1/payments/pay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void testPay_HighVelocityFlagged_HeldThenOfficerApproves() throws Exception {
    String merchantCode = registerMerchant(merchantToken);
    FakeAuditRecoveryClient.flagUser(customerId.toString());

    PayRequest request = PayRequest.builder()
      .merchantCode(merchantCode)
      .amount(BigDecimal.valueOf(90000))
      .currency("LKR")
      .build();

    MvcResult payResult = mockMvc
      .perform(
        post("/api/v1/payments/pay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("HELD_FOR_REVIEW"))
      .andReturn();

    PaymentResponse payment = objectMapper.readValue(
      payResult.getResponse().getContentAsString(),
      PaymentResponse.class
    );

    mockMvc
      .perform(
        get("/api/v1/payments/officer/held").header("Authorization", "Bearer " + officerToken)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value(payment.getId().toString()));

    PaymentReviewRequest reviewRequest = PaymentReviewRequest.builder().approve(true).build();

    mockMvc
      .perform(
        post("/api/v1/payments/officer/" + payment.getId() + "/review")
          .header("Authorization", "Bearer " + officerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(reviewRequest))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void testQrPay_Success() throws Exception {
    String merchantCode = registerMerchant(merchantToken);

    QrPaymentDetails details = QrPaymentDetails.builder()
      .merchantCode(merchantCode)
      .suggestedAmount(BigDecimal.valueOf(2500))
      .currency("LKR")
      .build();
    String qrPayload = Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(details));

    QrPayRequest request = QrPayRequest.builder().qrPayload(qrPayload).build();

    mockMvc
      .perform(
        post("/api/v1/payments/qr/pay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.status").value("COMPLETED"))
      .andExpect(jsonPath("$.channel").value("QR"))
      .andExpect(jsonPath("$.amount").value(2500));
  }

  @Test
  void testGetReceipt_Success() throws Exception {
    String merchantCode = registerMerchant(merchantToken);
    PaymentResponse payment = createCompletedPayment(merchantCode, BigDecimal.valueOf(750));

    mockMvc
      .perform(
        get("/api/v1/payments/" + payment.getId() + "/receipt").header(
          "Authorization",
          "Bearer " + customerToken
        )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.referenceNumber").value(payment.getReferenceNumber()))
      .andExpect(jsonPath("$.merchantName").value("Keells Super"));
  }

  @Test
  void testGetPayment_NotOwnerAndNotOfficer_ReturnsNotFound() throws Exception {
    String merchantCode = registerMerchant(merchantToken);
    PaymentResponse payment = createCompletedPayment(merchantCode, BigDecimal.valueOf(300));

    UUID otherCustomerId = UUID.randomUUID();
    String otherCustomerToken = tokenProvider.generateAccessToken(
      otherCustomerId,
      "customer2",
      Role.CUSTOMER,
      UserStatus.ACTIVE
    );

    mockMvc
      .perform(
        get("/api/v1/payments/" + payment.getId()).header(
          "Authorization",
          "Bearer " + otherCustomerToken
        )
      )
      .andExpect(status().isNotFound());
  }

  @Test
  void testGetHistory_ReturnsOwnPaymentsOnly() throws Exception {
    String merchantCode = registerMerchant(merchantToken);
    createCompletedPayment(merchantCode, BigDecimal.valueOf(100));
    createCompletedPayment(merchantCode, BigDecimal.valueOf(200));

    mockMvc
      .perform(get("/api/v1/payments").header("Authorization", "Bearer " + customerToken))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content.length()").value(2));
  }

  @Test
  void testPay_WithoutToken_IsRejected() throws Exception {
    PayRequest request = PayRequest.builder()
      .merchantCode("MCH-ANY")
      .amount(BigDecimal.valueOf(100))
      .currency("LKR")
      .build();

    mockMvc
      .perform(
        post("/api/v1/payments/pay")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().is4xxClientError());
  }

  private PaymentResponse createCompletedPayment(String merchantCode, BigDecimal amount)
    throws Exception {
    PayRequest request = PayRequest.builder()
      .merchantCode(merchantCode)
      .amount(amount)
      .currency("LKR")
      .build();

    MvcResult result = mockMvc
      .perform(
        post("/api/v1/payments/pay")
          .header("Authorization", "Bearer " + customerToken)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isCreated())
      .andReturn();

    return objectMapper.readValue(result.getResponse().getContentAsString(), PaymentResponse.class);
  }
}
