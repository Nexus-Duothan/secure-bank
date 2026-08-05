package com.securebank.accounts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
@Slf4j
public class AccountsService {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
  private static final DateTimeFormatter EXPIRY_TIME = DateTimeFormatter.ofPattern(
    "dd MMM yyyy HH:mm"
  ).withZone(COLOMBO);
  private static final DateTimeFormatter STATEMENT_DATE = DateTimeFormatter.ofPattern(
    "dd MMM yyyy"
  );
  private static final DateTimeFormatter STATEMENT_SHORT_DATE = DateTimeFormatter.ofPattern(
    "MM/dd"
  );
  private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
  private static final String DEMO_USER_ID = "8f7113d3-9f5b-4a7b-88fd-0a0b7854b1d1";
  private static final String DEMO_FULL_NAME = "Kaveesha Kapitiarachchi";
  private static final String DEMO_EMAIL = "kaveesha.kapitiarachchi@securebank.lk";
  private static final String DEMO_PHONE = "+94 77 510 6101";
  private static final String DEMO_ADDRESS_LINE = "42 Lake Drive";
  private static final String DEMO_ADDRESS_CITY = "Kandy, Sri Lanka";
  private static final int OTP_MAX_ATTEMPTS = 5;
  private static final int OTP_LENGTH = 6;

  /**
   * The bank's account product catalogue (FR-09). A customer can only open an
   * account against one of these, so the product is picked from this list rather
   * than typed in.
   */
  private static final List<AccountProductResponse> ACCOUNT_PRODUCTS = List.of(
    new AccountProductResponse(
      "SAV-EVERYDAY",
      "Everyday Savings",
      "SAVINGS",
      "Day to day saving with instant access to your money.",
      "LKR",
      4.5,
      new BigDecimal("1000.00"),
      BigDecimal.ZERO
    ),
    new AccountProductResponse(
      "SAV-SUPER",
      "Super Saver",
      "SAVINGS",
      "Our highest everyday rate. Keep the minimum balance to earn it.",
      "LKR",
      7.25,
      new BigDecimal("25000.00"),
      BigDecimal.ZERO
    ),
    new AccountProductResponse(
      "SAV-GOAL",
      "Goal Savings",
      "SAVINGS",
      "Build savings toward a personal goal with a competitive interest rate.",
      "LKR",
      6.0,
      new BigDecimal("1000.00"),
      BigDecimal.ZERO
    ),
    new AccountProductResponse(
      "SAV-YOUTH",
      "Youth Savings",
      "SAVINGS",
      "For customers under 26. No monthly fee and a low opening balance.",
      "LKR",
      5.0,
      new BigDecimal("500.00"),
      BigDecimal.ZERO
    ),
    new AccountProductResponse(
      "SAV-SENIOR",
      "Senior Citizen Savings",
      "SAVINGS",
      "For customers over 60. Our best rate with a small minimum balance.",
      "LKR",
      8.0,
      new BigDecimal("10000.00"),
      BigDecimal.ZERO
    ),
    new AccountProductResponse(
      "CUR-EVERYDAY",
      "Everyday Current",
      "CURRENT",
      "For daily spending, salary, and bill payments.",
      "LKR",
      0.0,
      new BigDecimal("5000.00"),
      new BigDecimal("350.00")
    ),
    new AccountProductResponse(
      "CUR-BUSINESS",
      "Business Current",
      "CURRENT",
      "For small businesses, with higher daily transfer limits.",
      "LKR",
      0.0,
      new BigDecimal("25000.00"),
      new BigDecimal("1200.00")
    ),
    new AccountProductResponse(
      "CUR-PREMIER",
      "Premier Current",
      "CURRENT",
      "Earns interest on your balance. No monthly fee.",
      "LKR",
      1.5,
      new BigDecimal("100000.00"),
      BigDecimal.ZERO
    )
  );

  private final ConcurrentMap<UUID, PendingAccountChange> pendingChanges =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, PendingAccountLink> pendingLinks = new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, PendingAccountOpening> pendingOpenings =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, PendingCardLink> pendingCardLinks = new ConcurrentHashMap<>();
  private final Set<String> linkedAccountIds = ConcurrentHashMap.newKeySet();
  private final Set<String> linkedCardIds = ConcurrentHashMap.newKeySet();
  private final ConcurrentMap<String, AccountState> accountsById = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, List<BankCardResponse>> cardsByAccountId =
    new ConcurrentHashMap<>();
  /** Product name per opened account, so the detail page and card can show it. */
  private final ConcurrentMap<String, String> productNameByAccountId = new ConcurrentHashMap<>();
  private final RestClient restClient;
  private final boolean exposeOtpCode;
  private final AccountState primaryAccount = new AccountState(
    "acc-demo-primary",
    "Everyday Current",
    "CURRENT",
    "67",
    new BigDecimal("48231.76"),
    "LKR",
    2.4,
    "2m ago",
    "1234 5678 90067",
    "SBLK0007",
    "14 Mar 2024",
    "Kandy City",
    "Individual account",
    "Active - Verified",
    false,
    null
  );
  private final AccountState savingsAccount = new AccountState(
    "acc-demo-savings",
    "Goal Savings",
    "SAVINGS",
    "7890",
    new BigDecimal("128900.00"),
    "LKR",
    1.1,
    "1m ago",
    "1234567890",
    "SBLK0012",
    "08 Sep 2022",
    "Kandy City",
    "Joint account",
    "Active - Verified",
    false,
    null
  );
  private final Map<String, EligibleAccount> eligibleAccountsByNumber = Map.of(
    "1234567890",
    new EligibleAccount(savingsAccount, "200229602936", DEMO_PHONE)
  );

  private static final List<TransactionResponse> RECENT_TRANSACTIONS = List.of(
    new TransactionResponse(
      "txn-demo-001",
      "Ceylon Electricity Board",
      "Utilities",
      "Today",
      new BigDecimal("-84.20"),
      true
    ),
    new TransactionResponse(
      "txn-demo-002",
      "Salary Deposit",
      "Income",
      "Yesterday",
      new BigDecimal("3200.00"),
      true
    ),
    new TransactionResponse(
      "txn-demo-003",
      "Kumar's Grocers",
      "Groceries",
      "Jul 20",
      new BigDecimal("-46.75"),
      true
    ),
    new TransactionResponse(
      "txn-demo-004",
      "Transfer to A. Silva",
      "Transfer",
      "Jul 19",
      new BigDecimal("-150.00"),
      true
    )
  );

  private static final List<AccountActivityResponse> ACCOUNT_ACTIVITY = List.of(
    new AccountActivityResponse(
      "txn-demo-001",
      "Ceylon Electricity Board",
      "Bill payment",
      "BILL_PAYMENT",
      "Kandy",
      new BigDecimal("-84.20"),
      "LKR",
      Instant.parse("2026-08-01T03:42:00Z"),
      "Today - 01 Aug 2026",
      "J-94021",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-002",
      "Salary Deposit",
      "Income",
      "INCOME",
      "SecureBank Payroll",
      new BigDecimal("3200.00"),
      "LKR",
      Instant.parse("2026-07-31T03:15:00Z"),
      "Yesterday - 31 Jul 2026",
      "J-94020",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-003",
      "Kumar's Grocers",
      "Card payment",
      "CARD_PAYMENT",
      "Kandy",
      new BigDecimal("-46.75"),
      "LKR",
      Instant.parse("2026-07-20T12:50:00Z"),
      "20 Jul 2026",
      "J-93981",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-004",
      "Transfer to A. Silva",
      "Outgoing transfer",
      "TRANSFER",
      "SecureBank Transfer",
      new BigDecimal("-150.00"),
      "LKR",
      Instant.parse("2026-07-19T08:40:00Z"),
      "19 Jul 2026",
      "J-93972",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-005",
      "Highway Fuel Stop",
      "Transport",
      "TRANSPORT",
      "Kadawatha",
      new BigDecimal("-18.40"),
      "LKR",
      Instant.parse("2026-07-18T06:10:00Z"),
      "18 Jul 2026",
      "J-93958",
      true
    ),
    new AccountActivityResponse(
      "txn-demo-006",
      "Mobile Reload",
      "Bills",
      "BILL_PAYMENT",
      "Dialog",
      new BigDecimal("-12.00"),
      "LKR",
      Instant.parse("2026-07-17T14:35:00Z"),
      "17 Jul 2026",
      "J-93940",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-007",
      "Rent Collection",
      "Income",
      "INCOME",
      "Standing order",
      new BigDecimal("950.00"),
      "LKR",
      Instant.parse("2026-07-15T01:45:00Z"),
      "15 Jul 2026",
      "J-93901",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-008",
      "Pharmacy Care",
      "Health",
      "HEALTH",
      "Kandy",
      new BigDecimal("-36.90"),
      "LKR",
      Instant.parse("2026-07-14T12:15:00Z"),
      "14 Jul 2026",
      "J-93890",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-009",
      "Dialog Broadband",
      "Internet",
      "INTERNET",
      "Online",
      new BigDecimal("-42.50"),
      "LKR",
      Instant.parse("2026-07-13T04:52:00Z"),
      "13 Jul 2026",
      "J-93878",
      false
    ),
    new AccountActivityResponse(
      "txn-demo-010",
      "Bookshop",
      "Education",
      "EDUCATION",
      "Peradeniya",
      new BigDecimal("-22.10"),
      "LKR",
      Instant.parse("2026-07-12T08:05:00Z"),
      "12 Jul 2026",
      "J-93860",
      false
    )
  );

  private static final List<AccountActivityResponse> SAVINGS_ACCOUNT_ACTIVITY = List.of(
    new AccountActivityResponse(
      "txn-savings-001",
      "Monthly savings transfer",
      "Transfer in",
      "TRANSFER",
      "Everyday Current",
      new BigDecimal("15000.00"),
      "LKR",
      Instant.parse("2026-07-28T03:30:00Z"),
      "28 Jul 2026",
      "J-95021",
      false
    ),
    new AccountActivityResponse(
      "txn-savings-002",
      "Savings interest",
      "Interest",
      "INCOME",
      "SecureBank",
      new BigDecimal("640.25"),
      "LKR",
      Instant.parse("2026-07-25T03:30:00Z"),
      "25 Jul 2026",
      "J-95008",
      false
    ),
    new AccountActivityResponse(
      "txn-savings-003",
      "Emergency fund transfer",
      "Transfer out",
      "TRANSFER",
      "Everyday Current",
      new BigDecimal("-5000.00"),
      "LKR",
      Instant.parse("2026-07-12T05:10:00Z"),
      "12 Jul 2026",
      "J-94962",
      false
    ),
    new AccountActivityResponse(
      "txn-savings-004",
      "Monthly savings transfer",
      "Transfer in",
      "TRANSFER",
      "Everyday Current",
      new BigDecimal("15000.00"),
      "LKR",
      Instant.parse("2026-06-28T03:30:00Z"),
      "28 Jun 2026",
      "J-94874",
      false
    )
  );

  public AccountsService(
    @Value(
      "${securebank.notification.service-url:http://localhost:8088}"
    ) String notificationServiceUrl,
    @Value("${securebank.accounts.otp.expose-code:false}") boolean exposeOtpCode
  ) {
    this.restClient = RestClient.builder().baseUrl(notificationServiceUrl).build();
    this.exposeOtpCode = exposeOtpCode;
    accountsById.put(primaryAccount.id(), primaryAccount);
    accountsById.put(savingsAccount.id(), savingsAccount);
    linkedAccountIds.add(primaryAccount.id());
    cardsByAccountId.put(
      primaryAccount.id(),
      new ArrayList<>(
        List.of(
          new BankCardResponse(
            "card-debit-primary",
            primaryAccount.id(),
            "DEBIT",
            "Everyday Debit",
            "4910 12** **** 0067",
            DEMO_FULL_NAME.toUpperCase(),
            "01/29",
            "VISA",
            "Active",
            false
          )
        )
      )
    );
    cardsByAccountId.put(
      savingsAccount.id(),
      new ArrayList<>(
        List.of(
          new BankCardResponse(
            "card-debit-joint",
            savingsAccount.id(),
            "DEBIT",
            "Shared Savings Debit",
            "4910 12** **** 7890",
            "KAVEESHA K. / JOINT",
            "06/30",
            "VISA",
            "Active",
            true
          )
        )
      )
    );
  }

  public AccountResponse getPrimaryAccount() {
    return getPrimaryAccount(null);
  }

  public AccountResponse getPrimaryAccount(String callerUserId) {
    return toAccountResponse(primaryAccount);
  }

  public List<AccountResponse> getLinkedAccounts() {
    return getLinkedAccounts(null);
  }

  public List<AccountResponse> getLinkedAccounts(String callerUserId) {
    return accountsById
      .values()
      .stream()
      .filter(account -> linkedAccountIds.contains(account.id()))
      .sorted(Comparator.comparing(account -> account.id().equals(primaryAccount.id()) ? 0 : 1))
      .map(this::toAccountResponse)
      .toList();
  }

  public List<TransactionResponse> getRecentTransactions(int limit) {
    return getRecentTransactions(primaryAccount.id(), limit);
  }

  public List<TransactionResponse> getRecentTransactions(String id, int limit) {
    AccountState account = requireLinkedAccount(id);
    List<TransactionResponse> transactions = account.id().equals(primaryAccount.id())
      ? RECENT_TRANSACTIONS
      : accountActivity(account.id())
          .stream()
          .sorted(Comparator.comparing(AccountActivityResponse::timestamp).reversed())
          .map(transaction ->
            new TransactionResponse(
              transaction.id(),
              transaction.merchant(),
              transaction.category(),
              transaction.dateGroupLabel(),
              transaction.amount(),
              !transaction.flagged()
            )
          )
          .toList();
    int safeLimit = Math.min(Math.max(limit, 1), transactions.size());
    return transactions.subList(0, safeLimit);
  }

  public List<AccountActivityResponse> getTransactionHistory(
    String id,
    TransactionDirection direction,
    LocalDate dateFrom,
    LocalDate dateTo,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    String type,
    boolean flaggedOnly
  ) {
    requireLinkedAccount(id);
    String normalizedType = normalizeType(type);

    return accountActivity(id)
      .stream()
      .filter(transaction -> matchesDirection(transaction, direction))
      .filter(transaction -> matchesDateRange(transaction, dateFrom, dateTo))
      .filter(transaction -> matchesAmountRange(transaction, minAmount, maxAmount))
      .filter(transaction -> matchesType(transaction, normalizedType))
      .filter(transaction -> !flaggedOnly || transaction.flagged())
      .sorted(Comparator.comparing(AccountActivityResponse::timestamp).reversed())
      .toList();
  }

  public AccountDetailResponse getAccountById(String id) {
    return toAccountDetailResponse(requireLinkedAccount(id));
  }

  public OtpChallengeResponse requestFreeze(String id, FreezeAccountRequest request) {
    AccountState account = requireLinkedAccount(id);
    if (account.frozen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already frozen");
    }
    return createChange("FREEZE_ACCOUNT", id, request.reason());
  }

  public OtpChallengeResponse requestUnfreeze(String id) {
    AccountState account = requireLinkedAccount(id);
    if (!account.frozen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is not frozen");
    }
    return createChange("UNFREEZE_ACCOUNT", id, null);
  }

  public AccountDetailResponse confirmChange(UUID changeRequestId, ConfirmChangeRequest request) {
    PendingAccountChange change = pendingChanges.get(changeRequestId);
    if (change == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found");
    }
    if (change.expiresAt().isBefore(Instant.now())) {
      pendingChanges.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP challenge expired");
    }
    if (change.failedAttempts() >= OTP_MAX_ATTEMPTS) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Too many incorrect codes for this request; start the change again"
      );
    }
    if (!change.code().equals(request.otpCode())) {
      change.incrementAttempts();
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid OTP code, " +
          Math.max(0, OTP_MAX_ATTEMPTS - change.failedAttempts()) +
          " attempt(s) remaining"
      );
    }

    AccountState account = requireLinkedAccount(change.accountId());
    if ("FREEZE_ACCOUNT".equals(change.type())) {
      account.freeze(change.reason());
    } else if ("UNFREEZE_ACCOUNT".equals(change.type())) {
      account.unfreeze();
    }
    pendingChanges.remove(changeRequestId);
    return toAccountDetailResponse(account);
  }

  public byte[] downloadStatement(String id) {
    AccountState account = requireLinkedAccount(id);
    return buildStatementPdf(account, buildStatementSnapshot(account, accountActivity(id)));
  }

  public OtpChallengeResponse requestAccountLink(LinkAccountRequest request) {
    String accountNumber = normalizeIdentifier(request.accountNumber());
    EligibleAccount eligibleAccount = eligibleAccountsByNumber.get(accountNumber);
    if (
      eligibleAccount == null ||
      !eligibleAccount
        .nationalIdOrPassport()
        .equalsIgnoreCase(normalizeIdentifier(request.nationalIdOrPassport()))
    ) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "We could not match those details to an account owned by you"
      );
    }
    if (linkedAccountIds.contains(eligibleAccount.account().id())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is already linked");
    }

    String code = generateOtpCode();
    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingLinks.put(
      changeRequestId,
      new PendingAccountLink(
        eligibleAccount.account().id(),
        code,
        expiresAt,
        eligibleAccount.registeredPhone()
      )
    );
    dispatchOtp(code, expiresAt, "LINK_ACCOUNT", eligibleAccount.registeredPhone());

    return new OtpChallengeResponse(
      changeRequestId,
      "LINK_ACCOUNT",
      "",
      expiresAt,
      "Enter the six digit code sent by SMS to the mobile number registered with this account.",
      exposeOtpCode ? code : null
    );
  }

  public LinkedAccountResponse confirmAccountLink(
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    PendingAccountLink link = pendingLinks.get(changeRequestId);
    if (link == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account link request not found");
    }
    if (link.expiresAt().isBefore(Instant.now())) {
      pendingLinks.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP challenge expired");
    }
    if (link.failedAttempts() >= OTP_MAX_ATTEMPTS) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Too many incorrect codes for this request; start linking the account again"
      );
    }
    if (!link.code().equals(request.otpCode())) {
      link.incrementAttempts();
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Invalid OTP code, " +
          Math.max(0, OTP_MAX_ATTEMPTS - link.failedAttempts()) +
          " attempt(s) remaining"
      );
    }
    linkedAccountIds.add(link.accountId());
    pendingLinks.remove(changeRequestId);
    AccountResponse account = toAccountResponse(accountsById.get(link.accountId()));
    return new LinkedAccountResponse(account, "Account linked successfully");
  }

  /** The products a customer may open, optionally narrowed to one account type. */
  public List<AccountProductResponse> getAccountProducts(String accountType) {
    if (accountType == null || accountType.isBlank() || "ALL".equalsIgnoreCase(accountType)) {
      return ACCOUNT_PRODUCTS;
    }
    String requested = accountType.trim().toUpperCase();
    return ACCOUNT_PRODUCTS.stream()
      .filter(product -> product.accountType().equals(requested))
      .toList();
  }

  public OtpChallengeResponse requestAccountOpening(OpenAccountRequest request) {
    // Reject an unknown or mismatched product now, before the customer types a code.
    requireProduct(request.productCode(), request.accountType());

    String code = generateOtpCode();
    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingOpenings.put(changeRequestId, new PendingAccountOpening(request, code, expiresAt));
    dispatchOtp(code, expiresAt, "OPEN_ACCOUNT", DEMO_PHONE);
    return productChallenge(
      changeRequestId,
      "OPEN_ACCOUNT",
      expiresAt,
      "Enter the six digit code sent by SMS to open this account.",
      code
    );
  }

  public LinkedAccountResponse confirmAccountOpening(
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    PendingAccountOpening opening = pendingOpenings.get(changeRequestId);
    if (opening == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account opening request not found");
    }
    validateOpeningOtp(changeRequestId, opening, request.otpCode());

    AccountProductResponse product = requireProduct(
      opening.request().productCode(),
      opening.request().accountType()
    );
    String accountId = "acc-" + UUID.randomUUID().toString().substring(0, 8);
    String accountNumber = generateAccountNumber();
    AccountState account = new AccountState(
      accountId,
      product.name(),
      opening.request().accountType(),
      accountNumber.substring(accountNumber.length() - 4),
      BigDecimal.ZERO,
      "LKR",
      0,
      "Just now",
      accountNumber,
      "SBLK0007",
      "01 Aug 2026",
      "Digital Banking",
      "JOINT".equals(opening.request().ownershipType()) ? "Joint account" : "Individual account",
      "Active - Verified",
      false,
      null
    );
    accountsById.put(accountId, account);
    linkedAccountIds.add(accountId);
    productNameByAccountId.put(accountId, product.name());
    cardsByAccountId.put(
      accountId,
      new ArrayList<>(List.of(createDebitCard(account, product.name())))
    );
    pendingOpenings.remove(changeRequestId);
    return new LinkedAccountResponse(toAccountResponse(account), "New account opened successfully");
  }

  public OtpChallengeResponse requestCreditCardLink(LinkCreditCardRequest request) {
    AccountState account = requireLinkedAccount(request.accountId());
    String cardNumber = normalizeIdentifier(request.cardNumber());
    boolean matchesBankRecord =
      "4485123412345678".equals(cardNumber) &&
      "11/29".equals(request.expiryDate()) &&
      "200229602936".equals(normalizeIdentifier(request.nationalIdOrPassport()));
    if (!matchesBankRecord) {
      throw new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "We could not match those card details to a credit card owned by you"
      );
    }
    if (linkedCardIds.contains("card-credit-demo")) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This credit card is already linked");
    }

    String code = generateOtpCode();
    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingCardLinks.put(changeRequestId, new PendingCardLink(account.id(), code, expiresAt));
    dispatchOtp(code, expiresAt, "LINK_CREDIT_CARD", DEMO_PHONE);
    return productChallenge(
      changeRequestId,
      "LINK_CREDIT_CARD",
      expiresAt,
      "Enter the six digit code sent by SMS to link this credit card.",
      code
    );
  }

  public LinkedCardResponse confirmCreditCardLink(
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    PendingCardLink link = pendingCardLinks.get(changeRequestId);
    if (link == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card link request not found");
    }
    validateCardOtp(changeRequestId, link, request.otpCode());
    AccountState account = requireLinkedAccount(link.accountId());
    BankCardResponse card = new BankCardResponse(
      "card-credit-demo",
      account.id(),
      "CREDIT",
      "SecureBank Platinum",
      "4485 **** **** 5678",
      DEMO_FULL_NAME.toUpperCase(),
      "11/29",
      "VISA",
      "Active",
      false
    );
    cardsByAccountId.computeIfAbsent(account.id(), ignored -> new ArrayList<>()).add(card);
    linkedCardIds.add(card.id());
    pendingCardLinks.remove(changeRequestId);
    return new LinkedCardResponse(card, "Credit card linked successfully");
  }

  /**
   * Resolves a product code to a product the bank actually offers, and checks it
   * belongs to the requested account type. An account can never be opened
   * against a product that is not in the catalogue.
   */
  private AccountProductResponse requireProduct(String productCode, String accountType) {
    String code = productCode == null ? "" : productCode.trim().toUpperCase();
    AccountProductResponse product = ACCOUNT_PRODUCTS.stream()
      .filter(candidate -> candidate.code().equals(code))
      .findFirst()
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown account product")
      );
    if (!product.accountType().equals(accountType)) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        product.name() + " is not available as a " + accountType.toLowerCase() + " account"
      );
    }
    return product;
  }

  private OtpChallengeResponse productChallenge(
    UUID id,
    String type,
    Instant expiresAt,
    String message,
    String code
  ) {
    return new OtpChallengeResponse(id, type, "", expiresAt, message, exposeOtpCode ? code : null);
  }

  private void validateOpeningOtp(UUID id, PendingAccountOpening opening, String otpCode) {
    if (opening.expiresAt().isBefore(Instant.now())) {
      pendingOpenings.remove(id);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP challenge expired");
    }
    if (!opening.code().equals(otpCode)) {
      opening.incrementAttempts();
      throwOtpError(opening.failedAttempts());
    }
  }

  private void validateCardOtp(UUID id, PendingCardLink link, String otpCode) {
    if (link.expiresAt().isBefore(Instant.now())) {
      pendingCardLinks.remove(id);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP challenge expired");
    }
    if (!link.code().equals(otpCode)) {
      link.incrementAttempts();
      throwOtpError(link.failedAttempts());
    }
  }

  private void throwOtpError(int failedAttempts) {
    if (failedAttempts >= OTP_MAX_ATTEMPTS) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Too many incorrect codes; start the request again"
      );
    }
    throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Invalid OTP code, " + (OTP_MAX_ATTEMPTS - failedAttempts) + " attempt(s) remaining"
    );
  }

  private String generateAccountNumber() {
    long random = java.util.concurrent.ThreadLocalRandom.current().nextLong(0, 10_000_000_000L);
    return "88" + String.format("%010d", random);
  }

  private BankCardResponse createDebitCard(AccountState account, String productName) {
    String suffix = account.lastFourDigits();
    return new BankCardResponse(
      "card-debit-" + account.id(),
      account.id(),
      "DEBIT",
      productName + " Debit",
      "4910 12** **** " + suffix,
      DEMO_FULL_NAME.toUpperCase(),
      "08/31",
      "VISA",
      "Active",
      "Joint account".equals(account.ownershipLabel())
    );
  }

  private OtpChallengeResponse createChange(String type, String accountId, String reason) {
    String code = generateOtpCode();
    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingChanges.put(
      changeRequestId,
      new PendingAccountChange(changeRequestId, accountId, type, reason, code, expiresAt)
    );
    dispatchOtp(code, expiresAt, type, DEMO_PHONE);

    return new OtpChallengeResponse(
      changeRequestId,
      type,
      maskPhone(DEMO_PHONE),
      expiresAt,
      "Enter the six digit code sent to your registered mobile number to confirm this change.",
      exposeOtpCode ? code : null
    );
  }

  private void dispatchOtp(String code, Instant expiresAt, String type, String phoneNumber) {
    try {
      restClient
        .post()
        .uri("/api/v1/notifications/otp-challenges")
        .contentType(MediaType.APPLICATION_JSON)
        .body(
          new OtpChallengeDispatchRequest(
            UUID.fromString(DEMO_USER_ID),
            DEMO_FULL_NAME,
            DEMO_EMAIL,
            phoneNumber,
            false,
            true,
            type,
            code,
            expiresAt
          )
        )
        .retrieve()
        .toBodilessEntity();
    } catch (RuntimeException exception) {
      log.warn(
        "Unable to queue account OTP via notification-service. SMS fallback remains log-only. {}",
        exception.getMessage()
      );
    }
    log.info(
      "Account OTP queued for {}. Code {} expires at {}.",
      phoneNumber,
      code,
      EXPIRY_TIME.format(expiresAt)
    );
  }

  private String generateOtpCode() {
    int random = java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 1_000_000);
    return String.format("%0" + OTP_LENGTH + "d", random);
  }

  private boolean matchesDirection(
    AccountActivityResponse transaction,
    TransactionDirection direction
  ) {
    return switch (direction) {
      case IN -> transaction.amount().signum() > 0;
      case OUT -> transaction.amount().signum() < 0;
      case ALL -> true;
    };
  }

  private boolean matchesDateRange(
    AccountActivityResponse transaction,
    LocalDate dateFrom,
    LocalDate dateTo
  ) {
    LocalDate transactionDate = transaction.timestamp().atZone(COLOMBO).toLocalDate();
    if (dateFrom != null && transactionDate.isBefore(dateFrom)) {
      return false;
    }
    if (dateTo != null && transactionDate.isAfter(dateTo)) {
      return false;
    }
    return true;
  }

  private boolean matchesAmountRange(
    AccountActivityResponse transaction,
    BigDecimal minAmount,
    BigDecimal maxAmount
  ) {
    BigDecimal absoluteAmount = transaction.amount().abs();
    if (minAmount != null && absoluteAmount.compareTo(minAmount) < 0) {
      return false;
    }
    if (maxAmount != null && absoluteAmount.compareTo(maxAmount) > 0) {
      return false;
    }
    return true;
  }

  private boolean matchesType(AccountActivityResponse transaction, String type) {
    return type == null || transaction.transactionType().equalsIgnoreCase(type);
  }

  private String normalizeType(String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    return type.trim().toUpperCase();
  }

  private StatementSnapshot buildStatementSnapshot(
    AccountState account,
    List<AccountActivityResponse> accountTransactions
  ) {
    LocalDate statementDate = LocalDate.of(2026, 8, 1);
    LocalDate periodStart = LocalDate.of(2026, 7, 1);
    LocalDate periodEnd = LocalDate.of(2026, 7, 31);
    List<AccountActivityResponse> statementTransactions = accountTransactions
      .stream()
      .filter(
        transaction ->
          !transaction.timestamp().isBefore(periodStart.atStartOfDay(COLOMBO).toInstant())
      )
      .filter(transaction ->
        transaction.timestamp().isBefore(periodEnd.plusDays(1).atStartOfDay(COLOMBO).toInstant())
      )
      .sorted(Comparator.comparing(AccountActivityResponse::timestamp))
      .toList();

    BigDecimal credits = statementTransactions
      .stream()
      .map(AccountActivityResponse::amount)
      .filter(amount -> amount.signum() > 0)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal debits = statementTransactions
      .stream()
      .map(AccountActivityResponse::amount)
      .filter(amount -> amount.signum() < 0)
      .map(BigDecimal::abs)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal postPeriodNet = accountTransactions
      .stream()
      .filter(transaction ->
        transaction.timestamp().isAfter(periodEnd.plusDays(1).atStartOfDay(COLOMBO).toInstant())
      )
      .map(AccountActivityResponse::amount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal endingBalance = account.balance().subtract(postPeriodNet);
    BigDecimal previousBalance = endingBalance.subtract(credits.subtract(debits));
    BigDecimal runningBalance = previousBalance;

    List<StatementTransactionRow> rows = new ArrayList<>();
    for (AccountActivityResponse transaction : statementTransactions) {
      runningBalance = runningBalance.add(transaction.amount());
      rows.add(
        new StatementTransactionRow(
          STATEMENT_SHORT_DATE.format(transaction.timestamp().atZone(COLOMBO).toLocalDate()),
          transaction.merchant(),
          transaction.amount().signum() < 0 ? transaction.amount().abs() : null,
          transaction.amount().signum() > 0 ? transaction.amount() : null,
          runningBalance
        )
      );
    }

    return new StatementSnapshot(
      statementDate,
      periodStart,
      periodEnd,
      previousBalance,
      credits,
      debits,
      endingBalance,
      rows
    );
  }

  private byte[] buildStatementPdf(AccountState account, StatementSnapshot snapshot) {
    PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    try (
      PDDocument document = new PDDocument();
      ByteArrayOutputStream output = new ByteArrayOutputStream()
    ) {
      PDPage page = new PDPage(PDRectangle.LETTER);
      document.addPage(page);

      try (PDPageContentStream content = new PDPageContentStream(document, page)) {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();
        float left = 46;
        float right = pageWidth - 46;
        float y = pageHeight - 46;

        content.setStrokingColor(new Color(224, 228, 235));
        content.setLineWidth(1f);
        content.addRect(24, 24, pageWidth - 48, pageHeight - 48);
        content.stroke();

        drawBankIcon(content, left, y - 8);
        drawText(content, bold, 20, 92, y - 4, "Bank Statement");

        float rightColumnX = 370;
        drawText(content, bold, 14, rightColumnX, y - 2, DEMO_FULL_NAME);
        drawText(content, regular, 12, rightColumnX, y - 22, DEMO_ADDRESS_LINE);
        drawText(content, regular, 12, rightColumnX, y - 40, DEMO_ADDRESS_CITY);
        drawText(content, bold, 10, rightColumnX, y - 70, "ACCOUNT NUMBER:");
        drawText(
          content,
          bold,
          13,
          rightColumnX,
          y - 88,
          maskStatementAccountNumber(account.accountNumber())
        );

        float infoY = y - 78;
        drawText(
          content,
          bold,
          11,
          left,
          infoY,
          "Statement Date: " + STATEMENT_DATE.format(snapshot.statementDate())
        );
        drawText(
          content,
          bold,
          11,
          left,
          infoY - 22,
          "Period covered: " +
            STATEMENT_DATE.format(snapshot.periodStart()) +
            " - " +
            STATEMENT_DATE.format(snapshot.periodEnd())
        );

        float summaryTitleY = infoY - 78;
        drawText(content, bold, 18, left, summaryTitleY, "Account Summary");
        drawDivider(content, left, right, summaryTitleY - 12);

        float summaryRowY = summaryTitleY - 38;
        drawSummaryRow(
          content,
          bold,
          regular,
          left,
          right,
          summaryRowY,
          "PREVIOUS BALANCE (" + snapshot.periodStart().format(STATEMENT_DATE).toUpperCase() + "):",
          formatMoney(snapshot.previousBalance(), true)
        );
        drawSummaryRow(
          content,
          regular,
          regular,
          left,
          right,
          summaryRowY - 24,
          "Total money in:",
          formatMoney(snapshot.totalMoneyIn(), true)
        );
        drawSummaryRow(
          content,
          regular,
          regular,
          left,
          right,
          summaryRowY - 48,
          "Total money out:",
          formatMoney(snapshot.totalMoneyOut(), true)
        );
        drawSummaryRow(
          content,
          bold,
          bold,
          left,
          right,
          summaryRowY - 78,
          "ENDING BALANCE (" + snapshot.periodEnd().format(STATEMENT_DATE).toUpperCase() + "):",
          formatMoney(snapshot.endingBalance(), true)
        );

        float transactionsTitleY = summaryRowY - 130;
        drawText(content, bold, 18, left, transactionsTitleY, "Transactions");
        drawDivider(content, left, right, transactionsTitleY - 12);

        float headerY = transactionsTitleY - 38;
        float colDate = left;
        float colDescription = left + 70;
        float colWithdraw = left + 255;
        float colDeposit = left + 345;
        float colBalance = right - 4;

        drawText(content, bold, 10, colDate, headerY, "DATE");
        drawText(content, bold, 10, colDescription, headerY, "DESCRIPTION");
        drawText(content, bold, 10, colWithdraw, headerY, "WITHDRAW");
        drawText(content, bold, 10, colDeposit, headerY, "DEPOSIT");
        drawRightAlignedText(content, bold, 10, colBalance, headerY, "BALANCE");

        drawDivider(content, left, right, headerY - 8);

        float rowY = headerY - 28;
        for (StatementTransactionRow row : snapshot.rows()) {
          drawText(content, regular, 12, colDate, rowY, row.dateLabel());
          drawText(content, regular, 12, colDescription, rowY, row.description());
          if (row.withdraw() != null) {
            drawText(
              content,
              regular,
              12,
              colWithdraw,
              rowY,
              "-" + formatMoney(row.withdraw(), false)
            );
          }
          if (row.deposit() != null) {
            drawText(content, regular, 12, colDeposit, rowY, formatMoney(row.deposit(), false));
          }
          drawRightAlignedText(
            content,
            regular,
            12,
            colBalance,
            rowY,
            formatMoney(row.balance(), false)
          );
          rowY -= 24;
        }
      }

      document.save(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to generate account statement PDF", exception);
    }
  }

  private void drawBankIcon(PDPageContentStream content, float x, float y) throws IOException {
    content.setStrokingColor(new Color(130, 96, 230));
    content.setLineWidth(1.8f);
    content.moveTo(x + 2, y);
    content.lineTo(x + 16, y + 10);
    content.lineTo(x + 30, y);
    content.stroke();

    content.moveTo(x + 6, y - 1);
    content.lineTo(x + 6, y - 15);
    content.moveTo(x + 16, y - 1);
    content.lineTo(x + 16, y - 15);
    content.moveTo(x + 26, y - 1);
    content.lineTo(x + 26, y - 15);
    content.moveTo(x + 2, y - 15);
    content.lineTo(x + 30, y - 15);
    content.stroke();
  }

  private void drawDivider(PDPageContentStream content, float left, float right, float y)
    throws IOException {
    content.setStrokingColor(new Color(130, 96, 230));
    content.setLineWidth(1.2f);
    content.moveTo(left, y);
    content.lineTo(right, y);
    content.stroke();
  }

  private void drawSummaryRow(
    PDPageContentStream content,
    PDFont labelFont,
    PDFont valueFont,
    float left,
    float right,
    float y,
    String label,
    String value
  ) throws IOException {
    drawText(content, labelFont, 11, left, y, label);
    drawRightAlignedText(content, valueFont, 11, right, y, value);
  }

  private void drawText(
    PDPageContentStream content,
    PDFont font,
    float fontSize,
    float x,
    float y,
    String text
  ) throws IOException {
    content.beginText();
    content.setFont(font, fontSize);
    content.setNonStrokingColor(new Color(55, 65, 81));
    content.newLineAtOffset(x, y);
    content.showText(text);
    content.endText();
  }

  private void drawRightAlignedText(
    PDPageContentStream content,
    PDFont font,
    float fontSize,
    float rightX,
    float y,
    String text
  ) throws IOException {
    float textWidth = (font.getStringWidth(text) / 1000f) * fontSize;
    drawText(content, font, fontSize, rightX - textWidth, y, text);
  }

  private String maskStatementAccountNumber(String accountNumber) {
    return "**** **** *" + accountNumber.substring(accountNumber.length() - 4);
  }

  private String formatMoney(BigDecimal amount, boolean includeCurrency) {
    String formatted = MONEY_FORMAT.format(amount);
    return includeCurrency ? "LKR " + formatted : formatted;
  }

  private AccountState requireLinkedAccount(String id) {
    AccountState account = accountsById.get(id);
    if (account == null || !linkedAccountIds.contains(id)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
    }
    return account;
  }

  private List<AccountActivityResponse> accountActivity(String accountId) {
    if (accountId.equals(primaryAccount.id())) {
      return ACCOUNT_ACTIVITY;
    }
    if (accountId.equals(savingsAccount.id())) {
      return SAVINGS_ACCOUNT_ACTIVITY;
    }
    return List.of();
  }

  private String normalizeIdentifier(String value) {
    return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
  }

  private String maskPhone(String phone) {
    String value = phone.trim();
    return value.substring(0, 3) + "***" + value.substring(value.length() - 4);
  }

  private AccountResponse toAccountResponse(AccountState state) {
    return new AccountResponse(
      state.id(),
      state.nickname(),
      state.accountType(),
      state.lastFourDigits(),
      state.accountNumber(),
      state.balance(),
      state.currency(),
      state.monthlyChangePercent(),
      state.verifiedLabel(),
      state.status(),
      state.frozen(),
      state.freezeReason()
    );
  }

  private AccountDetailResponse toAccountDetailResponse(AccountState state) {
    return new AccountDetailResponse(
      state.id(),
      state.nickname(),
      productNameByAccountId.getOrDefault(
        state.id(),
        "CURRENT".equals(state.accountType()) ? "Current account" : "Savings account"
      ),
      state.currency(),
      state.balance(),
      state.accountNumber(),
      state.ifscCode(),
      state.openedOn(),
      state.homeBranch(),
      state.ownershipLabel(),
      state.status(),
      state.frozen(),
      state.freezeReason(),
      List.copyOf(cardsByAccountId.getOrDefault(state.id(), List.of()))
    );
  }

  private static final class PendingAccountChange {

    private final UUID id;
    private final String accountId;
    private final String type;
    private final String reason;
    private final String code;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingAccountChange(
      UUID id,
      String accountId,
      String type,
      String reason,
      String code,
      Instant expiresAt
    ) {
      this.id = id;
      this.accountId = accountId;
      this.type = type;
      this.reason = reason;
      this.code = code;
      this.expiresAt = expiresAt;
    }

    private String type() {
      return type;
    }

    private String accountId() {
      return accountId;
    }

    private String reason() {
      return reason;
    }

    private String code() {
      return code;
    }

    private Instant expiresAt() {
      return expiresAt;
    }

    private int failedAttempts() {
      return failedAttempts;
    }

    private void incrementAttempts() {
      failedAttempts += 1;
    }
  }

  private record EligibleAccount(
    AccountState account,
    String nationalIdOrPassport,
    String registeredPhone
  ) {}

  private static final class PendingAccountLink {

    private final String accountId;
    private final String code;
    private final Instant expiresAt;
    private final String registeredPhone;
    private int failedAttempts;

    private PendingAccountLink(
      String accountId,
      String code,
      Instant expiresAt,
      String registeredPhone
    ) {
      this.accountId = accountId;
      this.code = code;
      this.expiresAt = expiresAt;
      this.registeredPhone = registeredPhone;
    }

    private String accountId() {
      return accountId;
    }

    private String code() {
      return code;
    }

    private Instant expiresAt() {
      return expiresAt;
    }

    private String registeredPhone() {
      return registeredPhone;
    }

    private int failedAttempts() {
      return failedAttempts;
    }

    private void incrementAttempts() {
      failedAttempts += 1;
    }
  }

  private static final class PendingAccountOpening {

    private final OpenAccountRequest request;
    private final String code;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingAccountOpening(OpenAccountRequest request, String code, Instant expiresAt) {
      this.request = request;
      this.code = code;
      this.expiresAt = expiresAt;
    }

    private OpenAccountRequest request() {
      return request;
    }

    private String code() {
      return code;
    }

    private Instant expiresAt() {
      return expiresAt;
    }

    private int failedAttempts() {
      return failedAttempts;
    }

    private void incrementAttempts() {
      failedAttempts += 1;
    }
  }

  private static final class PendingCardLink {

    private final String accountId;
    private final String code;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingCardLink(String accountId, String code, Instant expiresAt) {
      this.accountId = accountId;
      this.code = code;
      this.expiresAt = expiresAt;
    }

    private String accountId() {
      return accountId;
    }

    private String code() {
      return code;
    }

    private Instant expiresAt() {
      return expiresAt;
    }

    private int failedAttempts() {
      return failedAttempts;
    }

    private void incrementAttempts() {
      failedAttempts += 1;
    }
  }

  private record StatementSnapshot(
    LocalDate statementDate,
    LocalDate periodStart,
    LocalDate periodEnd,
    BigDecimal previousBalance,
    BigDecimal totalMoneyIn,
    BigDecimal totalMoneyOut,
    BigDecimal endingBalance,
    List<StatementTransactionRow> rows
  ) {}

  private record StatementTransactionRow(
    String dateLabel,
    String description,
    BigDecimal withdraw,
    BigDecimal deposit,
    BigDecimal balance
  ) {}

  private static final class AccountState {

    private final String id;
    private final String nickname;
    private final String accountType;
    private final String lastFourDigits;
    private final BigDecimal balance;
    private final String currency;
    private final double monthlyChangePercent;
    private final String verifiedLabel;
    private final String accountNumber;
    private final String ifscCode;
    private final String openedOn;
    private final String homeBranch;
    private final String ownershipLabel;
    private String status;
    private boolean frozen;
    private String freezeReason;

    private AccountState(
      String id,
      String nickname,
      String accountType,
      String lastFourDigits,
      BigDecimal balance,
      String currency,
      double monthlyChangePercent,
      String verifiedLabel,
      String accountNumber,
      String ifscCode,
      String openedOn,
      String homeBranch,
      String ownershipLabel,
      String status,
      boolean frozen,
      String freezeReason
    ) {
      this.id = id;
      this.nickname = nickname;
      this.accountType = accountType;
      this.lastFourDigits = lastFourDigits;
      this.balance = balance;
      this.currency = currency;
      this.monthlyChangePercent = monthlyChangePercent;
      this.verifiedLabel = verifiedLabel;
      this.accountNumber = accountNumber;
      this.ifscCode = ifscCode;
      this.openedOn = openedOn;
      this.homeBranch = homeBranch;
      this.ownershipLabel = ownershipLabel;
      this.status = status;
      this.frozen = frozen;
      this.freezeReason = freezeReason;
    }

    private String id() {
      return id;
    }

    private String nickname() {
      return nickname;
    }

    private String accountType() {
      return accountType;
    }

    private String lastFourDigits() {
      return lastFourDigits;
    }

    private BigDecimal balance() {
      return balance;
    }

    private String currency() {
      return currency;
    }

    private double monthlyChangePercent() {
      return monthlyChangePercent;
    }

    private String verifiedLabel() {
      return verifiedLabel;
    }

    private String accountNumber() {
      return accountNumber;
    }

    private String ifscCode() {
      return ifscCode;
    }

    private String openedOn() {
      return openedOn;
    }

    private String homeBranch() {
      return homeBranch;
    }

    private String ownershipLabel() {
      return ownershipLabel;
    }

    private String status() {
      return status;
    }

    private boolean frozen() {
      return frozen;
    }

    private String freezeReason() {
      return freezeReason;
    }

    private void freeze(String reason) {
      frozen = true;
      freezeReason = reason == null || reason.isBlank() ? "Customer protection request" : reason;
      status = "Frozen - Protected";
    }

    private void unfreeze() {
      frozen = false;
      freezeReason = null;
      status = "Active - Verified";
    }
  }
}
