package com.securebank.accounts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Accounts, cards and the transaction ledger.
 *
 * <p>Every figure returned here is read from the database and scoped to the caller resolved from
 * the gateway identity header. A customer who has just registered owns no accounts, so the API
 * answers with an empty portfolio until they open or claim one - nothing is pre-populated.
 */
@Service
public class AccountsService {

  private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");
  private static final DateTimeFormatter STATEMENT_DATE = DateTimeFormatter.ofPattern(
    "dd MMM yyyy"
  );
  private static final DateTimeFormatter STATEMENT_SHORT_DATE = DateTimeFormatter.ofPattern(
    "MM/dd"
  );
  private static final DateTimeFormatter ACTIVITY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
  private static final DateTimeFormatter SHORT_ACTIVITY_DATE = DateTimeFormatter.ofPattern("MMM d");
  private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
  private static final int TOTP_MAX_ATTEMPTS = 5;
  /** Shown instead of a masked phone number now that codes come from the authenticator app. */
  private static final String TOTP_DELIVERY_TARGET = "Authenticator app";
  /** Branch details of the digital banking branch every self-service account is opened at. */
  private static final String BRANCH_IFSC = "SBLK0007";
  private static final String BRANCH_NAME = "Digital Banking";
  private static final String DEFAULT_CURRENCY = "LKR";
  private static final String FALLBACK_HOLDER_NAME = "Account holder";

  /**
   * The bank's account product catalogue. A customer can only open an
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

  /**
   * Challenges staged for the current authenticator step. They live for five minutes and are
   * deliberately not persisted - nothing here is customer data, only an in-flight confirmation.
   */
  private final ConcurrentMap<UUID, PendingAccountChange> pendingChanges =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, PendingAccountLink> pendingLinks = new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, PendingAccountOpening> pendingOpenings =
    new ConcurrentHashMap<>();
  private final ConcurrentMap<UUID, PendingCardLink> pendingCardLinks = new ConcurrentHashMap<>();

  private final AccountRepository accountRepository;
  private final AccountTransactionRepository transactionRepository;
  private final BankCardRepository cardRepository;
  private final TotpClient totpClient;
  private final UserProfileClient userProfileClient;

  public AccountsService(
    AccountRepository accountRepository,
    AccountTransactionRepository transactionRepository,
    BankCardRepository cardRepository,
    TotpClient totpClient,
    UserProfileClient userProfileClient
  ) {
    this.accountRepository = accountRepository;
    this.transactionRepository = transactionRepository;
    this.cardRepository = cardRepository;
    this.totpClient = totpClient;
    this.userProfileClient = userProfileClient;
  }

  // --------------------------------------------------------------------
  // Reads
  // --------------------------------------------------------------------

  /**
   * The account the dashboard opens on: the caller's oldest account. A customer who has not opened
   * or linked one yet has no primary account, which is a 404 rather than an invented one.
   */
  @Transactional(readOnly = true)
  public AccountResponse getPrimaryAccount(String callerUserId) {
    AccountEntity account = accountRepository
      .findFirstByUserIdOrderByCreatedAtAsc(requireCaller(callerUserId))
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "You do not have an account yet")
      );
    return toAccountResponse(account);
  }

  @Transactional(readOnly = true)
  public List<AccountResponse> getLinkedAccounts(String callerUserId) {
    return accountRepository
      .findByUserIdOrderByCreatedAtAsc(requireCaller(callerUserId))
      .stream()
      .map(this::toAccountResponse)
      .toList();
  }

  /** Recent activity on the dashboard's primary account; empty while the caller has no account. */
  @Transactional(readOnly = true)
  public List<TransactionResponse> getPrimaryAccountTransactions(String callerUserId, int limit) {
    Optional<AccountEntity> account = accountRepository.findFirstByUserIdOrderByCreatedAtAsc(
      requireCaller(callerUserId)
    );
    return account.map(entity -> recentTransactions(entity, limit)).orElseGet(List::of);
  }

  @Transactional(readOnly = true)
  public List<TransactionResponse> getRecentTransactions(
    String callerUserId,
    String accountId,
    int limit
  ) {
    return recentTransactions(requireOwnedAccount(callerUserId, accountId), limit);
  }

  @Transactional(readOnly = true)
  public List<AccountActivityResponse> getTransactionHistory(
    String callerUserId,
    String accountId,
    TransactionDirection direction,
    LocalDate dateFrom,
    LocalDate dateTo,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    String type,
    boolean flaggedOnly
  ) {
    AccountEntity account = requireOwnedAccount(callerUserId, accountId);
    String normalizedType = normalizeType(type);

    return transactionRepository
      .findByAccountIdOrderByOccurredAtDesc(account.getId())
      .stream()
      .filter(transaction -> matchesDirection(transaction, direction))
      .filter(transaction -> matchesDateRange(transaction, dateFrom, dateTo))
      .filter(transaction -> matchesAmountRange(transaction, minAmount, maxAmount))
      .filter(transaction -> matchesType(transaction, normalizedType))
      .filter(transaction -> !flaggedOnly || transaction.isFlagged())
      .map(this::toActivityResponse)
      .toList();
  }

  @Transactional(readOnly = true)
  public AccountDetailResponse getAccountById(String callerUserId, String accountId) {
    return toAccountDetailResponse(requireOwnedAccount(callerUserId, accountId));
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

  // --------------------------------------------------------------------
  // Freeze / unfreeze
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public OtpChallengeResponse requestFreeze(
    String accountId,
    FreezeAccountRequest request,
    String callerUserId
  ) {
    AccountEntity account = requireOwnedAccount(callerUserId, accountId);
    if (account.isFrozen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is already frozen");
    }
    return createChange(
      "FREEZE_ACCOUNT",
      account.getId(),
      request.reason(),
      requireCaller(callerUserId)
    );
  }

  @Transactional(readOnly = true)
  public OtpChallengeResponse requestUnfreeze(String accountId, String callerUserId) {
    AccountEntity account = requireOwnedAccount(callerUserId, accountId);
    if (!account.isFrozen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is not frozen");
    }
    return createChange("UNFREEZE_ACCOUNT", account.getId(), null, requireCaller(callerUserId));
  }

  @Transactional
  public AccountDetailResponse confirmChange(
    String callerUserId,
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    UUID caller = requireCaller(callerUserId);
    PendingAccountChange change = pendingChanges.get(changeRequestId);
    if (change == null || !change.userId().equals(caller)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found");
    }
    if (change.expiresAt().isBefore(Instant.now())) {
      pendingChanges.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request expired");
    }
    if (change.failedAttempts() >= TOTP_MAX_ATTEMPTS) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Too many incorrect codes for this request; start the change again"
      );
    }
    if (!totpClient.verify(change.userId(), request.otpCode())) {
      change.incrementAttempts();
      throwTotpError(change.failedAttempts());
    }

    AccountEntity account = requireOwnedAccount(callerUserId, change.accountId());
    if ("FREEZE_ACCOUNT".equals(change.type())) {
      account.freeze(change.reason());
    } else if ("UNFREEZE_ACCOUNT".equals(change.type())) {
      account.unfreeze();
    }
    pendingChanges.remove(changeRequestId);
    return toAccountDetailResponse(accountRepository.save(account));
  }

  // --------------------------------------------------------------------
  // Linking an existing account
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public OtpChallengeResponse requestAccountLink(LinkAccountRequest request, String callerUserId) {
    UUID caller = requireCaller(callerUserId);
    String accountNumber = normalizeIdentifier(request.accountNumber());
    AccountEntity account = accountRepository
      .findByAccountNumberAndUserIdIsNull(accountNumber)
      .filter(
        candidate ->
          candidate.getHolderNationalId() != null &&
          candidate
            .getHolderNationalId()
            .equalsIgnoreCase(normalizeIdentifier(request.nationalIdOrPassport()))
      )
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "We could not match those details to an account owned by you"
        )
      );

    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingLinks.put(
      changeRequestId,
      new PendingAccountLink(
        account.getId(),
        caller,
        nicknameOrDefault(request.nickname(), account.getNickname()),
        expiresAt
      )
    );

    return challenge(
      changeRequestId,
      "LINK_ACCOUNT",
      expiresAt,
      "Enter the current six digit code from your authenticator app to link this account."
    );
  }

  @Transactional
  public LinkedAccountResponse confirmAccountLink(
    String callerUserId,
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    UUID caller = requireCaller(callerUserId);
    PendingAccountLink link = pendingLinks.get(changeRequestId);
    if (link == null || !link.userId().equals(caller)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account link request not found");
    }
    if (link.expiresAt().isBefore(Instant.now())) {
      pendingLinks.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request expired");
    }
    if (link.failedAttempts() >= TOTP_MAX_ATTEMPTS) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Too many incorrect codes for this request; start linking the account again"
      );
    }
    if (!totpClient.verify(link.userId(), request.otpCode())) {
      link.incrementAttempts();
      throwTotpError(link.failedAttempts());
    }

    AccountEntity account = accountRepository
      .findById(link.accountId())
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    if (account.getUserId() != null) {
      pendingLinks.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This account is already linked");
    }
    account.setUserId(caller);
    // The customer names the account as they add it; the bank's own label is the fallback.
    account.setNickname(nicknameOrDefault(link.nickname(), account.getNickname()));
    AccountEntity saved = accountRepository.save(account);
    // Cards already issued against the account move across with it.
    cardRepository.findByAccountIdOrderByCreatedAtAsc(saved.getId()).forEach(card -> {
      card.setUserId(caller);
      cardRepository.save(card);
    });
    pendingLinks.remove(changeRequestId);
    return new LinkedAccountResponse(toAccountResponse(saved), "Account linked successfully");
  }

  // --------------------------------------------------------------------
  // Opening a new account
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public OtpChallengeResponse requestAccountOpening(
    OpenAccountRequest request,
    String callerUserId
  ) {
    // Reject an unknown or mismatched product now, before the customer types a code.
    requireProduct(request.productCode(), request.accountType());

    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingOpenings.put(
      changeRequestId,
      new PendingAccountOpening(request, requireCaller(callerUserId), expiresAt)
    );
    return challenge(
      changeRequestId,
      "OPEN_ACCOUNT",
      expiresAt,
      "Enter the current six digit code from your authenticator app to open this account."
    );
  }

  @Transactional
  public LinkedAccountResponse confirmAccountOpening(
    String callerUserId,
    String authorizationHeader,
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    UUID caller = requireCaller(callerUserId);
    PendingAccountOpening opening = pendingOpenings.get(changeRequestId);
    if (opening == null || !opening.userId().equals(caller)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account opening request not found");
    }
    if (opening.expiresAt().isBefore(Instant.now())) {
      pendingOpenings.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request expired");
    }
    if (!totpClient.verify(opening.userId(), request.otpCode())) {
      opening.incrementAttempts();
      throwTotpError(opening.failedAttempts());
    }

    AccountProductResponse product = requireProduct(
      opening.request().productCode(),
      opening.request().accountType()
    );
    UserProfileClient.UserProfileSnapshot profile = userProfileClient
      .getProfile(authorizationHeader, callerUserId)
      .orElse(null);

    Instant now = Instant.now();
    String accountNumber = generateAccountNumber();
    AccountEntity account = AccountEntity.builder()
      .id("acc-" + UUID.randomUUID().toString().substring(0, 8))
      .userId(caller)
      .holderName(profile == null ? null : profile.fullName())
      .holderAddressLine(profile == null ? null : profile.addressLine())
      .holderCity(profile == null ? null : profile.city())
      // The customer's own label wins; the product name is only the fallback.
      .nickname(nicknameOrDefault(opening.request().nickname(), product.name()))
      .accountType(opening.request().accountType())
      .productCode(product.code())
      .productName(product.name())
      .accountNumber(accountNumber)
      .balance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
      .currency(product.currency())
      .ifscCode(BRANCH_IFSC)
      .openedOn(LocalDate.now(COLOMBO))
      .homeBranch(BRANCH_NAME)
      .ownershipLabel(
        "JOINT".equals(opening.request().ownershipType()) ? "Joint account" : "Individual account"
      )
      .status("Active - Verified")
      .frozen(false)
      .createdAt(now)
      .updatedAt(now)
      .build();
    AccountEntity saved = accountRepository.save(account);
    cardRepository.save(buildDebitCard(saved, product.name(), now));

    pendingOpenings.remove(changeRequestId);
    return new LinkedAccountResponse(toAccountResponse(saved), "New account opened successfully");
  }

  // --------------------------------------------------------------------
  // Linking a credit card the bank already issued
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public OtpChallengeResponse requestCreditCardLink(
    LinkCreditCardRequest request,
    String callerUserId
  ) {
    UUID caller = requireCaller(callerUserId);
    AccountEntity account = requireOwnedAccount(callerUserId, request.accountId());
    BankCardEntity card = cardRepository
      .findByCardNumberAndUserIdIsNull(normalizeIdentifier(request.cardNumber()))
      .filter(candidate -> candidate.getExpiryDate().equals(request.expiryDate()))
      .filter(
        candidate ->
          candidate.getHolderNationalId() != null &&
          candidate
            .getHolderNationalId()
            .equalsIgnoreCase(normalizeIdentifier(request.nationalIdOrPassport()))
      )
      .orElseThrow(() ->
        new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "We could not match those card details to a credit card owned by you"
        )
      );

    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingCardLinks.put(
      changeRequestId,
      new PendingCardLink(account.getId(), card.getId(), caller, expiresAt)
    );
    return challenge(
      changeRequestId,
      "LINK_CREDIT_CARD",
      expiresAt,
      "Enter the current six digit code from your authenticator app to link this credit card."
    );
  }

  @Transactional
  public LinkedCardResponse confirmCreditCardLink(
    String callerUserId,
    UUID changeRequestId,
    ConfirmChangeRequest request
  ) {
    UUID caller = requireCaller(callerUserId);
    PendingCardLink link = pendingCardLinks.get(changeRequestId);
    if (link == null || !link.userId().equals(caller)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card link request not found");
    }
    if (link.expiresAt().isBefore(Instant.now())) {
      pendingCardLinks.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Verification request expired");
    }
    if (!totpClient.verify(link.userId(), request.otpCode())) {
      link.incrementAttempts();
      throwTotpError(link.failedAttempts());
    }

    AccountEntity account = requireOwnedAccount(callerUserId, link.accountId());
    BankCardEntity card = cardRepository
      .findById(link.cardId())
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Credit card not found")
      );
    if (card.getUserId() != null) {
      pendingCardLinks.remove(changeRequestId);
      throw new ResponseStatusException(HttpStatus.CONFLICT, "This credit card is already linked");
    }
    card.setUserId(caller);
    card.setAccountId(account.getId());
    BankCardEntity saved = cardRepository.save(card);
    pendingCardLinks.remove(changeRequestId);
    return new LinkedCardResponse(toCardResponse(saved), "Credit card linked successfully");
  }

  // --------------------------------------------------------------------
  // Ledger (service to service)
  // --------------------------------------------------------------------

  /**
   * The bank-wide transaction journal behind the admin audit view, newest first. It is
   * read straight off the ledger, so it only ever shows movements that really happened.
   */
  @Transactional(readOnly = true)
  public List<JournalEntryResponse> getJournal(int limit) {
    int safeLimit = Math.min(Math.max(limit, 1), 500);
    return transactionRepository
      .findAllByOrderByOccurredAtDesc(PageRequest.of(0, safeLimit))
      .stream()
      .map(transaction ->
        new JournalEntryResponse(
          transaction.getId(),
          transaction.getAccountId(),
          transaction.getMerchant(),
          transaction.getCategory(),
          transaction.getTransactionType(),
          transaction.getLocation(),
          transaction.getAmount(),
          transaction.getCurrency(),
          transaction.getOccurredAt(),
          transaction.getJournalId(),
          transaction.isFlagged()
        )
      )
      .toList();
  }

  /** Balance lookup for another core service; ownership is the caller's responsibility. */
  @Transactional(readOnly = true)
  public AccountSnapshotResponse getAccountSnapshot(String accountId) {
    AccountEntity account = accountRepository
      .findById(accountId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    return new AccountSnapshotResponse(
      account.getId(),
      account.getAccountNumber(),
      account.getBalance(),
      account.getCurrency(),
      account.isFrozen()
    );
  }

  /** Takes money off an account and appends the movement to its ledger. */
  @Transactional
  public LedgerEntryResponse debit(String accountId, LedgerEntryRequest request) {
    return postMovement(accountId, request, request.amount().negate());
  }

  /** Puts money onto an account and appends the movement to its ledger. */
  @Transactional
  public LedgerEntryResponse credit(String accountId, LedgerEntryRequest request) {
    return postMovement(accountId, request, request.amount());
  }

  /**
   * Debits a customer's primary account. Used where the paying service names the customer rather
   * than an account, such as a vendor or QR payment.
   */
  @Transactional
  public LedgerEntryResponse debitPrimaryAccount(String userId, LedgerEntryRequest request) {
    AccountEntity account = accountRepository
      .findFirstByUserIdOrderByCreatedAtAsc(requireCaller(userId))
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "This customer has no account")
      );
    return postMovement(account.getId(), request, request.amount().negate());
  }

  @Transactional
  public LedgerEntryResponse debitOwnedAccount(
    String userId,
    String accountId,
    LedgerEntryRequest request
  ) {
    UUID ownerId = requireCaller(userId);
    accountRepository
      .findByIdAndUserId(accountId, ownerId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found for this customer")
      );
    return postMovement(accountId, request, request.amount().negate());
  }

  @Transactional
  public RefundLedgerResponse refund(RefundLedgerRequest request) {
    UUID customerUserId = requireCaller(request.customerUserId());
    AccountEntity customer = accountRepository
      .findFirstByUserIdOrderByCreatedAtAsc(customerUserId)
      .orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "The customer has no account")
      );
    if (request.merchantAccountId().equals(customer.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund accounts must be different");
    }

    LedgerEntryResponse merchantEntry = debit(
      request.merchantAccountId(),
      new LedgerEntryRequest(
        request.amount(),
        request.currency(),
        request.reference() + ":merchant",
        request.merchant(),
        "Refunds",
        "REFUND_DEBIT",
        "SecureBank"
      )
    );
    LedgerEntryResponse customerEntry = credit(
      customer.getId(),
      new LedgerEntryRequest(
        request.amount(),
        request.currency(),
        request.reference() + ":customer",
        request.merchant(),
        "Refunds",
        "REFUND_CREDIT",
        "SecureBank"
      )
    );
    return new RefundLedgerResponse(
      request.merchantAccountId(),
      merchantEntry.newBalance(),
      customer.getId(),
      customerEntry.newBalance()
    );
  }

  /**
   * Credits the SecureBank account holding this account number. Used for an in-bank transfer,
   * where the sending service only knows the beneficiary's account number.
   */
  @Transactional
  public LedgerEntryResponse creditByAccountNumber(
    String accountNumber,
    LedgerEntryRequest request
  ) {
    AccountEntity account = accountRepository
      .findByAccountNumber(normalizeIdentifier(accountNumber))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    return postMovement(account.getId(), request, request.amount());
  }

  private LedgerEntryResponse postMovement(
    String accountId,
    LedgerEntryRequest request,
    BigDecimal signedAmount
  ) {
    AccountEntity account = accountRepository
      .findByIdForUpdate(accountId)
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

    // A replayed call must not move money a second time.
    if (request.reference() != null && !request.reference().isBlank()) {
      Optional<AccountTransactionEntity> existing =
        transactionRepository.findByAccountIdAndReference(accountId, request.reference());
      if (existing.isPresent()) {
        AccountTransactionEntity posted = existing.get();
        return new LedgerEntryResponse(
          accountId,
          posted.getId(),
          posted.getJournalId(),
          account.getBalance()
        );
      }
    }

    if (account.isFrozen()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Account is frozen");
    }
    if (
      request.currency() != null &&
      !request.currency().isBlank() &&
      !account.getCurrency().equalsIgnoreCase(request.currency())
    ) {
      throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Transaction currency must match the account currency " + account.getCurrency()
      );
    }
    BigDecimal newBalance = account.getBalance().add(signedAmount);
    if (newBalance.signum() < 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient funds");
    }

    Instant now = Instant.now();
    AccountTransactionEntity transaction = AccountTransactionEntity.builder()
      .id("txn-" + UUID.randomUUID())
      .accountId(accountId)
      .merchant(blankTo(request.merchant(), signedAmount.signum() < 0 ? "Payment" : "Deposit"))
      .category(blankTo(request.category(), signedAmount.signum() < 0 ? "Payments" : "Income"))
      .transactionType(
        blankTo(request.transactionType(), signedAmount.signum() < 0 ? "PAYMENT" : "INCOME")
      )
      .location(request.location())
      .amount(signedAmount.setScale(2, RoundingMode.HALF_UP))
      .currency(blankTo(request.currency(), account.getCurrency()))
      .balanceAfter(newBalance)
      .occurredAt(now)
      .journalId(nextJournalId())
      .flagged(false)
      .reference(
        request.reference() == null || request.reference().isBlank() ? null : request.reference()
      )
      .createdAt(now)
      .build();
    transactionRepository.save(transaction);

    account.setBalance(newBalance);
    account.setUpdatedAt(now);
    accountRepository.save(account);

    return new LedgerEntryResponse(
      accountId,
      transaction.getId(),
      transaction.getJournalId(),
      newBalance
    );
  }

  // --------------------------------------------------------------------
  // Statement
  // --------------------------------------------------------------------

  @Transactional(readOnly = true)
  public byte[] downloadStatement(String callerUserId, String accountId) {
    AccountEntity account = requireOwnedAccount(callerUserId, accountId);
    List<AccountTransactionEntity> activity =
      transactionRepository.findByAccountIdOrderByOccurredAtDesc(account.getId());
    return buildStatementPdf(account, buildStatementSnapshot(account, activity));
  }

  // --------------------------------------------------------------------
  // Internals
  // --------------------------------------------------------------------

  /**
   * The caller the gateway authenticated. There is no fallback identity: without a verified user
   * the service has no idea whose money is being asked about, so the request is rejected.
   */
  private UUID requireCaller(String callerUserId) {
    if (callerUserId == null || callerUserId.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing caller identity");
    }
    try {
      return UUID.fromString(callerUserId.trim());
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid caller identity");
    }
  }

  /** An account only exists as far as the API is concerned if the caller owns it. */
  private AccountEntity requireOwnedAccount(String callerUserId, String accountId) {
    return accountRepository
      .findByIdAndUserId(accountId, requireCaller(callerUserId))
      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
  }

  private List<TransactionResponse> recentTransactions(AccountEntity account, int limit) {
    int safeLimit = Math.max(limit, 1);
    return transactionRepository
      .findByAccountIdOrderByOccurredAtDesc(account.getId())
      .stream()
      .limit(safeLimit)
      .map(transaction ->
        new TransactionResponse(
          transaction.getId(),
          transaction.getMerchant(),
          transaction.getCategory(),
          shortDateLabel(transaction.getOccurredAt()),
          transaction.getAmount(),
          !transaction.isFlagged()
        )
      )
      .toList();
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

  private OtpChallengeResponse challenge(UUID id, String type, Instant expiresAt, String message) {
    return new OtpChallengeResponse(id, type, TOTP_DELIVERY_TARGET, expiresAt, message, null);
  }

  private OtpChallengeResponse createChange(
    String type,
    String accountId,
    String reason,
    UUID callerUserId
  ) {
    Instant expiresAt = Instant.now().plusSeconds(300);
    UUID changeRequestId = UUID.randomUUID();
    pendingChanges.put(
      changeRequestId,
      new PendingAccountChange(accountId, type, reason, callerUserId, expiresAt)
    );

    return challenge(
      changeRequestId,
      type,
      expiresAt,
      "Enter the current six digit code from your authenticator app to confirm this change."
    );
  }

  private void throwTotpError(int failedAttempts) {
    if (failedAttempts >= TOTP_MAX_ATTEMPTS) {
      throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST,
        "Too many incorrect codes; start the request again"
      );
    }
    throw new ResponseStatusException(
      HttpStatus.BAD_REQUEST,
      "Invalid authenticator code, " +
        (TOTP_MAX_ATTEMPTS - failedAttempts) +
        " attempt(s) remaining"
    );
  }

  private String generateAccountNumber() {
    for (int attempt = 0; attempt < 10; attempt++) {
      String candidate =
        "88" + String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L));
      if (!accountRepository.existsByAccountNumber(candidate)) {
        return candidate;
      }
    }
    throw new ResponseStatusException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Could not allocate an account number, please try again"
    );
  }

  private String nextJournalId() {
    return "J-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
  }

  private BankCardEntity buildDebitCard(AccountEntity account, String productName, Instant now) {
    String suffix = account.lastFourDigits();
    String cardNumber =
      "491012" + String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000)) + suffix;
    return BankCardEntity.builder()
      .id("card-debit-" + account.getId())
      .accountId(account.getId())
      .userId(account.getUserId())
      .cardType("DEBIT")
      .productName(productName + " Debit")
      .cardNumber(cardNumber)
      .maskedNumber("4910 12** **** " + suffix)
      .cardholderName(cardholderName(account))
      .expiryDate(LocalDate.now(COLOMBO).plusYears(5).format(DateTimeFormatter.ofPattern("MM/yy")))
      .holderNationalId(account.getHolderNationalId())
      .scheme("VISA")
      .status("Active")
      .jointAccountCard("Joint account".equals(account.getOwnershipLabel()))
      .createdAt(now)
      .updatedAt(now)
      .build();
  }

  private String cardholderName(AccountEntity account) {
    String holder = account.getHolderName();
    return holder == null || holder.isBlank()
      ? FALLBACK_HOLDER_NAME.toUpperCase()
      : holder.toUpperCase();
  }

  private boolean matchesDirection(
    AccountTransactionEntity transaction,
    TransactionDirection direction
  ) {
    return switch (direction) {
      case IN -> transaction.getAmount().signum() > 0;
      case OUT -> transaction.getAmount().signum() < 0;
      case ALL -> true;
    };
  }

  private boolean matchesDateRange(
    AccountTransactionEntity transaction,
    LocalDate dateFrom,
    LocalDate dateTo
  ) {
    LocalDate transactionDate = transaction.getOccurredAt().atZone(COLOMBO).toLocalDate();
    if (dateFrom != null && transactionDate.isBefore(dateFrom)) {
      return false;
    }
    return dateTo == null || !transactionDate.isAfter(dateTo);
  }

  private boolean matchesAmountRange(
    AccountTransactionEntity transaction,
    BigDecimal minAmount,
    BigDecimal maxAmount
  ) {
    BigDecimal absoluteAmount = transaction.getAmount().abs();
    if (minAmount != null && absoluteAmount.compareTo(minAmount) < 0) {
      return false;
    }
    return maxAmount == null || absoluteAmount.compareTo(maxAmount) <= 0;
  }

  private boolean matchesType(AccountTransactionEntity transaction, String type) {
    return type == null || transaction.getTransactionType().equalsIgnoreCase(type);
  }

  private String normalizeType(String type) {
    if (type == null || type.isBlank()) {
      return null;
    }
    return type.trim().toUpperCase();
  }

  private String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private StatementSnapshot buildStatementSnapshot(
    AccountEntity account,
    List<AccountTransactionEntity> accountTransactions
  ) {
    LocalDate today = LocalDate.now(COLOMBO);
    LocalDate periodStart = today.withDayOfMonth(1);
    LocalDate periodEnd = today;
    Instant periodStartInstant = periodStart.atStartOfDay(COLOMBO).toInstant();
    Instant periodEndInstant = periodEnd.plusDays(1).atStartOfDay(COLOMBO).toInstant();

    List<AccountTransactionEntity> statementTransactions = accountTransactions
      .stream()
      .filter(transaction -> !transaction.getOccurredAt().isBefore(periodStartInstant))
      .filter(transaction -> transaction.getOccurredAt().isBefore(periodEndInstant))
      .sorted(Comparator.comparing(AccountTransactionEntity::getOccurredAt))
      .toList();

    BigDecimal credits = statementTransactions
      .stream()
      .map(AccountTransactionEntity::getAmount)
      .filter(amount -> amount.signum() > 0)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal debits = statementTransactions
      .stream()
      .map(AccountTransactionEntity::getAmount)
      .filter(amount -> amount.signum() < 0)
      .map(BigDecimal::abs)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Anything posted after the period (nothing, since the period ends today) is backed out so the
    // closing figure always reconciles with the stored balance.
    BigDecimal postPeriodNet = accountTransactions
      .stream()
      .filter(transaction -> !transaction.getOccurredAt().isBefore(periodEndInstant))
      .map(AccountTransactionEntity::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal endingBalance = account.getBalance().subtract(postPeriodNet);
    BigDecimal previousBalance = endingBalance.subtract(credits.subtract(debits));
    BigDecimal runningBalance = previousBalance;

    List<StatementTransactionRow> rows = new ArrayList<>();
    for (AccountTransactionEntity transaction : statementTransactions) {
      runningBalance = runningBalance.add(transaction.getAmount());
      rows.add(
        new StatementTransactionRow(
          STATEMENT_SHORT_DATE.format(transaction.getOccurredAt().atZone(COLOMBO).toLocalDate()),
          transaction.getMerchant(),
          transaction.getAmount().signum() < 0 ? transaction.getAmount().abs() : null,
          transaction.getAmount().signum() > 0 ? transaction.getAmount() : null,
          runningBalance
        )
      );
    }

    return new StatementSnapshot(
      today,
      periodStart,
      periodEnd,
      previousBalance,
      credits,
      debits,
      endingBalance,
      rows
    );
  }

  private byte[] buildStatementPdf(AccountEntity account, StatementSnapshot snapshot) {
    PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    String holderName =
      account.getHolderName() == null || account.getHolderName().isBlank()
        ? FALLBACK_HOLDER_NAME
        : account.getHolderName();
    String addressLine =
      account.getHolderAddressLine() == null ? "" : account.getHolderAddressLine();
    String addressCity = account.getHolderCity() == null ? "" : account.getHolderCity();

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
        drawText(content, bold, 14, rightColumnX, y - 2, holderName);
        if (!addressLine.isBlank()) {
          drawText(content, regular, 12, rightColumnX, y - 22, addressLine);
        }
        if (!addressCity.isBlank()) {
          drawText(content, regular, 12, rightColumnX, y - 40, addressCity);
        }
        drawText(content, bold, 10, rightColumnX, y - 70, "ACCOUNT NUMBER:");
        drawText(
          content,
          bold,
          13,
          rightColumnX,
          y - 88,
          maskStatementAccountNumber(account.getAccountNumber())
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
        if (snapshot.rows().isEmpty()) {
          drawText(
            content,
            regular,
            12,
            colDate,
            rowY,
            "No transactions were posted in this period."
          );
        }
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
    return includeCurrency ? DEFAULT_CURRENCY + " " + formatted : formatted;
  }

  private String normalizeIdentifier(String value) {
    return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
  }

  /**
   * The nickname the customer typed, trimmed; the supplied fallback is used when they left the
   * field empty. The column is limited to 120 characters, so a longer label is cut to fit.
   */
  private String nicknameOrDefault(String nickname, String fallback) {
    if (nickname == null || nickname.isBlank()) {
      return fallback;
    }
    String trimmed = nickname.trim();
    return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
  }

  // --------------------------------------------------------------------
  // Derived presentation values (all computed from stored rows)
  // --------------------------------------------------------------------

  /** Net movement over the last 30 days as a percentage of the balance 30 days ago. */
  private double monthlyChangePercent(AccountEntity account) {
    BigDecimal net = transactionRepository
      .findByAccountIdAndOccurredAtAfter(account.getId(), Instant.now().minus(Duration.ofDays(30)))
      .stream()
      .map(AccountTransactionEntity::getAmount)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal opening = account.getBalance().subtract(net);
    if (opening.signum() == 0) {
      return 0;
    }
    return net
      .multiply(BigDecimal.valueOf(100))
      .divide(opening.abs(), 1, RoundingMode.HALF_UP)
      .doubleValue();
  }

  /** How fresh the balance is, based on when the account was last touched. */
  private String verifiedLabel(AccountEntity account) {
    Instant reference =
      account.getUpdatedAt() == null ? account.getCreatedAt() : account.getUpdatedAt();
    if (reference == null) {
      return "Just now";
    }
    long minutes = Duration.between(reference, Instant.now()).toMinutes();
    if (minutes < 1) {
      return "Just now";
    }
    if (minutes < 60) {
      return minutes + "m ago";
    }
    long hours = minutes / 60;
    if (hours < 24) {
      return hours + "h ago";
    }
    return hours / 24 + "d ago";
  }

  private String dateGroupLabel(Instant occurredAt) {
    LocalDate date = occurredAt.atZone(COLOMBO).toLocalDate();
    LocalDate today = LocalDate.now(COLOMBO);
    String formatted = ACTIVITY_DATE.format(date);
    if (date.equals(today)) {
      return "Today - " + formatted;
    }
    if (date.equals(today.minusDays(1))) {
      return "Yesterday - " + formatted;
    }
    return formatted;
  }

  private String shortDateLabel(Instant occurredAt) {
    LocalDate date = occurredAt.atZone(COLOMBO).toLocalDate();
    LocalDate today = LocalDate.now(COLOMBO);
    if (date.equals(today)) {
      return "Today";
    }
    if (date.equals(today.minusDays(1))) {
      return "Yesterday";
    }
    return SHORT_ACTIVITY_DATE.format(date);
  }

  private AccountResponse toAccountResponse(AccountEntity account) {
    return new AccountResponse(
      account.getId(),
      account.getNickname(),
      account.getAccountType(),
      account.lastFourDigits(),
      account.getAccountNumber(),
      account.getBalance(),
      account.getCurrency(),
      monthlyChangePercent(account),
      verifiedLabel(account),
      account.getStatus(),
      account.isFrozen(),
      account.getFreezeReason()
    );
  }

  private AccountDetailResponse toAccountDetailResponse(AccountEntity account) {
    return new AccountDetailResponse(
      account.getId(),
      account.getNickname(),
      account.getProductName() == null
        ? "CURRENT".equals(account.getAccountType())
          ? "Current account"
          : "Savings account"
        : account.getProductName(),
      account.getCurrency(),
      account.getBalance(),
      account.getAccountNumber(),
      account.getIfscCode(),
      STATEMENT_DATE.format(account.getOpenedOn()),
      account.getHomeBranch(),
      account.getOwnershipLabel(),
      account.getStatus(),
      account.isFrozen(),
      account.getFreezeReason(),
      cardRepository
        .findByAccountIdOrderByCreatedAtAsc(account.getId())
        .stream()
        .map(this::toCardResponse)
        .toList()
    );
  }

  private BankCardResponse toCardResponse(BankCardEntity card) {
    return new BankCardResponse(
      card.getId(),
      card.getAccountId(),
      card.getCardType(),
      card.getProductName(),
      card.getMaskedNumber(),
      card.getCardholderName(),
      card.getExpiryDate(),
      card.getScheme(),
      card.getStatus(),
      card.isJointAccountCard()
    );
  }

  private AccountActivityResponse toActivityResponse(AccountTransactionEntity transaction) {
    return new AccountActivityResponse(
      transaction.getId(),
      transaction.getMerchant(),
      transaction.getCategory(),
      transaction.getTransactionType(),
      transaction.getLocation(),
      transaction.getAmount(),
      transaction.getCurrency(),
      transaction.getOccurredAt(),
      dateGroupLabel(transaction.getOccurredAt()),
      transaction.getJournalId(),
      transaction.isFlagged()
    );
  }

  // --------------------------------------------------------------------
  // In-flight confirmation state
  // --------------------------------------------------------------------

  private static final class PendingAccountChange {

    private final String accountId;
    private final String type;
    private final String reason;
    private final UUID userId;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingAccountChange(
      String accountId,
      String type,
      String reason,
      UUID userId,
      Instant expiresAt
    ) {
      this.accountId = accountId;
      this.type = type;
      this.reason = reason;
      this.userId = userId;
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

    private UUID userId() {
      return userId;
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

  private static final class PendingAccountLink {

    private final String accountId;
    private final UUID userId;
    private final String nickname;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingAccountLink(String accountId, UUID userId, String nickname, Instant expiresAt) {
      this.accountId = accountId;
      this.userId = userId;
      this.nickname = nickname;
      this.expiresAt = expiresAt;
    }

    private String accountId() {
      return accountId;
    }

    private UUID userId() {
      return userId;
    }

    private String nickname() {
      return nickname;
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

  private static final class PendingAccountOpening {

    private final OpenAccountRequest request;
    private final UUID userId;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingAccountOpening(OpenAccountRequest request, UUID userId, Instant expiresAt) {
      this.request = request;
      this.userId = userId;
      this.expiresAt = expiresAt;
    }

    private OpenAccountRequest request() {
      return request;
    }

    private UUID userId() {
      return userId;
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
    private final String cardId;
    private final UUID userId;
    private final Instant expiresAt;
    private int failedAttempts;

    private PendingCardLink(String accountId, String cardId, UUID userId, Instant expiresAt) {
      this.accountId = accountId;
      this.cardId = cardId;
      this.userId = userId;
      this.expiresAt = expiresAt;
    }

    private String accountId() {
      return accountId;
    }

    private String cardId() {
      return cardId;
    }

    private UUID userId() {
      return userId;
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
}
