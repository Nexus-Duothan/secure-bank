package com.securebank.accounts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The service seeds no portfolio: everything read back here is something the test itself put in
 * the database, and a customer who has just registered sees nothing at all.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountsControllerTest {

  /** The code a customer would read off their authenticator app in these tests. */
  private static final String VALID_TOTP_CODE = "123456";

  private static final String USER_HEADER = "X-User-Id";
  private final UUID customerId = UUID.randomUUID();
  private final UUID otherCustomerId = UUID.randomUUID();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private AccountRepository accountRepository;

  @Autowired
  private AccountTransactionRepository transactionRepository;

  @Autowired
  private BankCardRepository cardRepository;

  @MockBean
  private TotpClient totpClient;

  @MockBean
  private UserProfileClient userProfileClient;

  @BeforeEach
  void resetLedger() {
    transactionRepository.deleteAll();
    cardRepository.deleteAll();
    accountRepository.deleteAll();
    given(totpClient.verify(any(), eq(VALID_TOTP_CODE))).willReturn(true);
    given(userProfileClient.getProfile(any(), any())).willReturn(
      Optional.of(
        new UserProfileClient.UserProfileSnapshot(
          "Kaveesha Kapitiarachchi",
          "42 Lake Drive",
          "Kandy, Sri Lanka"
        )
      )
    );
  }

  @Test
  void aCustomerWhoJustRegisteredOwnsNothing() throws Exception {
    mockMvc
      .perform(get("/api/v1/accounts").header(USER_HEADER, customerId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));

    mockMvc
      .perform(get("/api/v1/accounts/primary").header(USER_HEADER, customerId))
      .andExpect(status().isNotFound());

    mockMvc
      .perform(get("/api/v1/accounts/primary/transactions").header(USER_HEADER, customerId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void requestsWithoutACallerIdentityAreRejected() throws Exception {
    mockMvc.perform(get("/api/v1/accounts")).andExpect(status().isUnauthorized());
  }

  @Test
  void oneCustomerCannotSeeAnotherCustomersAccount() throws Exception {
    AccountEntity theirs = accountRepository.save(account(otherCustomerId, "8800000001"));

    mockMvc
      .perform(get("/api/v1/accounts/" + theirs.getId()).header(USER_HEADER, customerId))
      .andExpect(status().isNotFound());

    mockMvc
      .perform(get("/api/v1/accounts").header(USER_HEADER, customerId))
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void openingAnAccountStartsEmptyWithOneDebitCard() throws Exception {
    String response = mockMvc
      .perform(
        post("/api/v1/accounts/open")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"accountType\":\"SAVINGS\",\"productCode\":\"SAV-GOAL\",\"ownershipType\":\"INDIVIDUAL\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("OPEN_ACCOUNT"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    String confirmation = mockMvc
      .perform(
        post("/api/v1/accounts/open/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + VALID_TOTP_CODE + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.nickname").value("Goal Savings"))
      .andExpect(jsonPath("$.account.balance").value(0))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String accountId = com.jayway.jsonpath.JsonPath.read(confirmation, "$.account.id");
    mockMvc
      .perform(get("/api/v1/accounts/" + accountId).header(USER_HEADER, customerId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ownershipLabel").value("Individual account"))
      .andExpect(jsonPath("$.cards.length()").value(1))
      .andExpect(jsonPath("$.cards[0].cardType").value("DEBIT"))
      .andExpect(jsonPath("$.cards[0].cardholderName").value("KAVEESHA KAPITIARACHCHI"));

    // A brand new account has no history to show.
    mockMvc
      .perform(
        get("/api/v1/accounts/" + accountId + "/transactions").header(USER_HEADER, customerId)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void theCustomersOwnNicknameIsKeptWhenOpeningAnAccount() throws Exception {
    String response = mockMvc
      .perform(
        post("/api/v1/accounts/open")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"accountType\":\"SAVINGS\",\"productCode\":\"SAV-GOAL\"," +
              "\"ownershipType\":\"INDIVIDUAL\",\"nickname\":\"  Holiday fund  \"}"
          )
      )
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    mockMvc
      .perform(
        post("/api/v1/accounts/open/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + VALID_TOTP_CODE + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.nickname").value("Holiday fund"));
  }

  @Test
  void theCustomersOwnNicknameIsKeptWhenLinkingAnAccount() throws Exception {
    AccountEntity unclaimed = account(null, "1234567891");
    unclaimed.setHolderNationalId("200229602936");
    accountRepository.save(unclaimed);

    String response = mockMvc
      .perform(
        post("/api/v1/accounts/link")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"accountNumber\":\"1234567891\",\"nationalIdOrPassport\":\"200229602936\"," +
              "\"nickname\":\"Salary account\"}"
          )
      )
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    mockMvc
      .perform(
        post("/api/v1/accounts/link/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + VALID_TOTP_CODE + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.nickname").value("Salary account"));
  }

  @Test
  void ledgerPostingsMoveTheBalanceAndShowUpAsActivity() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000002"));

    mockMvc
      .perform(
        post("/internal/v1/accounts/" + mine.getId() + "/credit")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"amount\":3200.00,\"currency\":\"LKR\",\"reference\":\"SALARY-1\"," +
              "\"merchant\":\"Salary Deposit\",\"category\":\"Income\",\"transactionType\":\"INCOME\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.newBalance").value(3200.00));

    mockMvc
      .perform(
        post("/internal/v1/accounts/" + mine.getId() + "/debit")
          .contentType(MediaType.APPLICATION_JSON)
          .content(
            "{\"amount\":200.00,\"currency\":\"LKR\",\"reference\":\"BILL-1\"," +
              "\"merchant\":\"Ceylon Electricity Board\",\"category\":\"Utilities\"," +
              "\"transactionType\":\"BILL_PAYMENT\"}"
          )
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.newBalance").value(3000.00));

    mockMvc
      .perform(get("/api/v1/accounts/primary").header(USER_HEADER, customerId))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(mine.getId()))
      .andExpect(jsonPath("$.balance").value(3000.00));

    mockMvc
      .perform(
        get("/api/v1/accounts/" + mine.getId() + "/transactions").header(USER_HEADER, customerId)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].merchant").value("Ceylon Electricity Board"));
  }

  @Test
  void aRepostedReferenceDoesNotChargeTheCustomerTwice() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000003"));
    String body =
      "{\"amount\":500.00,\"currency\":\"LKR\",\"reference\":\"PAYMENT-1\"," +
      "\"merchant\":\"Kumar's Grocers\"}";

    mockMvc
      .perform(
        post("/internal/v1/accounts/" + mine.getId() + "/credit")
          .contentType(MediaType.APPLICATION_JSON)
          .content(body)
      )
      .andExpect(status().isOk());
    mockMvc
      .perform(
        post("/internal/v1/accounts/" + mine.getId() + "/credit")
          .contentType(MediaType.APPLICATION_JSON)
          .content(body)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.newBalance").value(500.00));

    Assertions.assertThat(
      transactionRepository.findByAccountIdOrderByOccurredAtDesc(mine.getId())
    ).hasSize(1);
  }

  @Test
  void aDebitBeyondTheBalanceIsRefused() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000004"));

    mockMvc
      .perform(
        post("/internal/v1/accounts/" + mine.getId() + "/debit")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"amount\":10.00,\"currency\":\"LKR\",\"reference\":\"OVERDRAW\"}")
      )
      .andExpect(status().isConflict());
  }

  @Test
  void transactionHistorySupportsFiltering() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000005"));
    Instant when = Instant.now().minus(2, ChronoUnit.DAYS);
    transactionRepository.save(
      transaction(mine.getId(), "Kumar's Grocers", "CARD_PAYMENT", new BigDecimal("-46.75"), when)
    );
    transactionRepository.save(
      transaction(mine.getId(), "Salary Deposit", "INCOME", new BigDecimal("3200.00"), when)
    );

    mockMvc
      .perform(
        get("/api/v1/accounts/" + mine.getId() + "/transactions")
          .header(USER_HEADER, customerId)
          .param("direction", "OUT")
          .param("minAmount", "40")
          .param("maxAmount", "60")
          .param("type", "CARD_PAYMENT")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].merchant").value("Kumar's Grocers"))
      .andExpect(jsonPath("$[0].transactionType").value("CARD_PAYMENT"));
  }

  @Test
  void statementEndpointReturnsPdfDownload() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000006"));
    transactionRepository.save(
      transaction(
        mine.getId(),
        "Ceylon Electricity Board",
        "BILL_PAYMENT",
        new BigDecimal("-84.20"),
        Instant.now()
      )
    );

    byte[] pdf = mockMvc
      .perform(
        get("/api/v1/accounts/" + mine.getId() + "/statement").header(USER_HEADER, customerId)
      )
      .andExpect(status().isOk())
      .andExpect(
        header().string("Content-Disposition", "attachment; filename=\"securebank-statement.pdf\"")
      )
      .andExpect(content().contentType(MediaType.APPLICATION_PDF))
      .andReturn()
      .getResponse()
      .getContentAsByteArray();

    try (PDDocument document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);
      Assertions.assertThat(text)
        .contains("Bank Statement")
        .contains("Account Summary")
        .contains("Transactions")
        .contains("Kaveesha Kapitiarachchi")
        .contains("Ceylon Electricity Board");
    }
  }

  @Test
  void freezeFlowBelongsToAccountsService() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000007"));

    String response = mockMvc
      .perform(
        post("/api/v1/accounts/" + mine.getId() + "/freeze")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"reason\":\"Card lost\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("FREEZE_ACCOUNT"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");

    mockMvc
      .perform(
        post("/api/v1/accounts/changes/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"000000\"}")
      )
      .andExpect(status().isBadRequest());

    mockMvc
      .perform(
        post("/api/v1/accounts/changes/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + VALID_TOTP_CODE + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.frozen").value(true))
      .andExpect(jsonPath("$.freezeReason").value("Card lost"));
  }

  @Test
  void onlyAnUnclaimedAccountMatchingTheHoldersIdCanBeLinked() throws Exception {
    // Nothing in the database matches, which is what a fresh install looks like.
    mockMvc
      .perform(
        post("/api/v1/accounts/link")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountNumber\":\"1234567890\",\"nationalIdOrPassport\":\"200229602936\"}")
      )
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("We could not match those details to an account owned by you")
      );

    AccountEntity unclaimed = account(null, "1234567890");
    unclaimed.setHolderNationalId("200229602936");
    accountRepository.save(unclaimed);

    mockMvc
      .perform(
        post("/api/v1/accounts/link")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountNumber\":\"1234567890\",\"nationalIdOrPassport\":\"wrong-id\"}")
      )
      .andExpect(status().isNotFound());

    String response = mockMvc
      .perform(
        post("/api/v1/accounts/link")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"accountNumber\":\"1234567890\",\"nationalIdOrPassport\":\"200229602936\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("LINK_ACCOUNT"))
      .andExpect(jsonPath("$.deliveryTarget").value("Authenticator app"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    mockMvc
      .perform(
        post("/api/v1/accounts/link/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + VALID_TOTP_CODE + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.account.accountNumber").value("1234567890"));

    mockMvc
      .perform(get("/api/v1/accounts").header(USER_HEADER, customerId))
      .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void creditCardLinkNeedsACardTheBankHasIssued() throws Exception {
    AccountEntity mine = accountRepository.save(account(customerId, "8800000008"));
    String payload =
      "{\"cardNumber\":\"4485 1234 1234 5678\",\"expiryDate\":\"11/29\"," +
      "\"nationalIdOrPassport\":\"200229602936\",\"accountId\":\"" +
      mine.getId() +
      "\"}";

    mockMvc
      .perform(
        post("/api/v1/accounts/cards/link")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content(payload)
      )
      .andExpect(status().isNotFound());

    cardRepository.save(
      BankCardEntity.builder()
        .id("card-credit-1")
        .cardType("CREDIT")
        .productName("SecureBank Platinum")
        .cardNumber("4485123412345678")
        .maskedNumber("4485 **** **** 5678")
        .cardholderName("KAVEESHA KAPITIARACHCHI")
        .expiryDate("11/29")
        .holderNationalId("200229602936")
        .scheme("VISA")
        .status("Active")
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build()
    );

    String response = mockMvc
      .perform(
        post("/api/v1/accounts/cards/link")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content(payload)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.type").value("LINK_CREDIT_CARD"))
      .andReturn()
      .getResponse()
      .getContentAsString();

    String changeRequestId = com.jayway.jsonpath.JsonPath.read(response, "$.changeRequestId");
    mockMvc
      .perform(
        post("/api/v1/accounts/cards/link/" + changeRequestId + "/confirm")
          .header(USER_HEADER, customerId)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"otpCode\":\"" + VALID_TOTP_CODE + "\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.card.accountId").value(mine.getId()))
      .andExpect(jsonPath("$.card.cardType").value("CREDIT"));

    mockMvc
      .perform(get("/api/v1/accounts/" + mine.getId()).header(USER_HEADER, customerId))
      .andExpect(jsonPath("$.cards.length()").value(1));
  }

  @Test
  void theJournalOnlyListsPostedMovements() throws Exception {
    mockMvc
      .perform(get("/internal/v1/accounts/journal"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(0));

    AccountEntity mine = accountRepository.save(account(customerId, "8800000009"));
    transactionRepository.save(
      transaction(
        mine.getId(),
        "Mobile Reload",
        "BILL_PAYMENT",
        new BigDecimal("-12.00"),
        Instant.now()
      )
    );

    mockMvc
      .perform(get("/internal/v1/accounts/journal"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].merchant").value("Mobile Reload"));
  }

  @Test
  void refundAtomicallyMovesMoneyAndIsIdempotent() throws Exception {
    AccountEntity merchant = accountRepository.save(account(otherCustomerId, "8800000010"));
    merchant.setBalance(new BigDecimal("1000.00"));
    merchant = accountRepository.save(merchant);
    AccountEntity customer = accountRepository.save(account(customerId, "8800000011"));

    String body =
      "{" +
      "\"merchantAccountId\":\"" +
      merchant.getId() +
      "\"," +
      "\"customerUserId\":\"" +
      customerId +
      "\"," +
      "\"amount\":250.00," +
      "\"currency\":\"LKR\"," +
      "\"reference\":\"refund:test-1001\"," +
      "\"merchant\":\"Demo Merchant\"}";

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
        .perform(
          post("/internal/v1/accounts/refund").contentType(MediaType.APPLICATION_JSON).content(body)
        )
        .andExpect(status().isOk());
    }

    Assertions.assertThat(
      accountRepository.findById(merchant.getId()).orElseThrow().getBalance()
    ).isEqualByComparingTo("750.00");
    Assertions.assertThat(
      accountRepository.findById(customer.getId()).orElseThrow().getBalance()
    ).isEqualByComparingTo("250.00");
    Assertions.assertThat(transactionRepository.findAll()).hasSize(2);
  }

  @Test
  void serviceDebitCannotUseAnAccountOwnedByAnotherCustomer() throws Exception {
    AccountEntity account = account(otherCustomerId, "8800000012");
    account.setBalance(new BigDecimal("100.00"));
    account = accountRepository.save(account);
    LedgerEntryRequest movement = new LedgerEntryRequest(
      new BigDecimal("10.00"),
      "LKR",
      "BILL-OWNERSHIP-TEST",
      "Test Biller",
      "Utilities",
      "BILL_PAYMENT",
      "SecureBank Bill Pay"
    );

    mockMvc
      .perform(
        post(
          "/internal/v1/accounts/by-user/{userId}/accounts/{accountId}/debit",
          customerId,
          account.getId()
        )
          .contentType(MediaType.APPLICATION_JSON)
          .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movement))
      )
      .andExpect(status().isNotFound());

    Assertions.assertThat(
      accountRepository.findById(account.getId()).orElseThrow().getBalance()
    ).isEqualByComparingTo("100.00");
  }

  @Test
  void ledgerRejectsCurrencyThatDoesNotMatchTheAccount() throws Exception {
    AccountEntity account = account(customerId, "8800000013");
    account.setBalance(new BigDecimal("100.00"));
    account = accountRepository.save(account);
    LedgerEntryRequest movement = new LedgerEntryRequest(
      new BigDecimal("10.00"),
      "USD",
      "CURRENCY-MISMATCH-TEST",
      "Test Biller",
      "Utilities",
      "BILL_PAYMENT",
      "SecureBank Bill Pay"
    );

    mockMvc
      .perform(
        post("/internal/v1/accounts/{accountId}/debit", account.getId())
          .contentType(MediaType.APPLICATION_JSON)
          .content(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(movement))
      )
      .andExpect(status().isConflict())
      .andExpect(
        jsonPath("$.message").value("Transaction currency must match the account currency LKR")
      );

    Assertions.assertThat(
      accountRepository.findById(account.getId()).orElseThrow().getBalance()
    ).isEqualByComparingTo("100.00");
  }

  private AccountEntity account(UUID owner, String accountNumber) {
    Instant now = Instant.now();
    return AccountEntity.builder()
      .id("acc-" + UUID.randomUUID().toString().substring(0, 8))
      .userId(owner)
      .holderName("Kaveesha Kapitiarachchi")
      .holderAddressLine("42 Lake Drive")
      .holderCity("Kandy, Sri Lanka")
      .nickname("Everyday Current")
      .accountType("CURRENT")
      .productCode("CUR-EVERYDAY")
      .productName("Everyday Current")
      .accountNumber(accountNumber)
      .balance(BigDecimal.ZERO)
      .currency("LKR")
      .ifscCode("SBLK0007")
      .openedOn(LocalDate.now())
      .homeBranch("Digital Banking")
      .ownershipLabel("Individual account")
      .status("Active - Verified")
      .frozen(false)
      .createdAt(now)
      .updatedAt(now)
      .build();
  }

  private AccountTransactionEntity transaction(
    String accountId,
    String merchant,
    String type,
    BigDecimal amount,
    Instant occurredAt
  ) {
    return AccountTransactionEntity.builder()
      .id("txn-" + UUID.randomUUID())
      .accountId(accountId)
      .merchant(merchant)
      .category("Test")
      .transactionType(type)
      .location("Kandy")
      .amount(amount)
      .currency("LKR")
      .occurredAt(occurredAt)
      .journalId("J-TEST" + UUID.randomUUID().toString().substring(0, 4))
      .flagged(false)
      .createdAt(Instant.now())
      .build();
  }
}
