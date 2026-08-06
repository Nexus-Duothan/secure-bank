package com.securebank.accounts;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer facing account endpoints. Every route is answered for the caller the gateway
 * authenticated ({@code X-User-Id}); there is no default or shared account.
 */
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountsController {

  private final AccountsService accountsService;

  @GetMapping
  public List<AccountResponse> getLinkedAccounts(
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.getLinkedAccounts(callerUserId);
  }

  @GetMapping("/primary")
  public AccountResponse getPrimaryAccount(
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.getPrimaryAccount(callerUserId);
  }

  @GetMapping("/primary/transactions")
  public List<TransactionResponse> getPrimaryAccountTransactions(
    @RequestParam(defaultValue = "4") int limit,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.getPrimaryAccountTransactions(callerUserId, limit);
  }

  @GetMapping("/{id}/transactions/recent")
  public List<TransactionResponse> getRecentTransactions(
    @PathVariable String id,
    @RequestParam(defaultValue = "4") int limit,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.getRecentTransactions(callerUserId, id, limit);
  }

  @GetMapping("/products")
  public List<AccountProductResponse> getAccountProducts(
    @RequestParam(required = false) String accountType
  ) {
    return accountsService.getAccountProducts(accountType);
  }

  @PostMapping("/link")
  public OtpChallengeResponse requestAccountLink(
    @Valid @RequestBody LinkAccountRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.requestAccountLink(request, callerUserId);
  }

  @PostMapping("/link/{changeRequestId}/confirm")
  public LinkedAccountResponse confirmAccountLink(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.confirmAccountLink(callerUserId, changeRequestId, request);
  }

  @PostMapping("/open")
  public OtpChallengeResponse requestAccountOpening(
    @Valid @RequestBody OpenAccountRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.requestAccountOpening(request, callerUserId);
  }

  @PostMapping("/open/{changeRequestId}/confirm")
  public LinkedAccountResponse confirmAccountOpening(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId,
    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
  ) {
    return accountsService.confirmAccountOpening(
      callerUserId,
      authorization,
      changeRequestId,
      request
    );
  }

  @PostMapping("/cards/link")
  public OtpChallengeResponse requestCreditCardLink(
    @Valid @RequestBody LinkCreditCardRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.requestCreditCardLink(request, callerUserId);
  }

  @PostMapping("/cards/link/{changeRequestId}/confirm")
  public LinkedCardResponse confirmCreditCardLink(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.confirmCreditCardLink(callerUserId, changeRequestId, request);
  }

  @GetMapping("/{id}/transactions")
  public List<AccountActivityResponse> getTransactionHistory(
    @PathVariable String id,
    @RequestParam(defaultValue = "ALL") TransactionDirection direction,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate dateFrom,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
    @RequestParam(required = false) BigDecimal minAmount,
    @RequestParam(required = false) BigDecimal maxAmount,
    @RequestParam(required = false) String type,
    @RequestParam(defaultValue = "false") boolean flaggedOnly,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.getTransactionHistory(
      callerUserId,
      id,
      direction,
      dateFrom,
      dateTo,
      minAmount,
      maxAmount,
      type,
      flaggedOnly
    );
  }

  @GetMapping("/{id}")
  public AccountDetailResponse getAccountById(
    @PathVariable String id,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.getAccountById(callerUserId, id);
  }

  @PostMapping("/{id}/freeze")
  public OtpChallengeResponse requestFreeze(
    @PathVariable String id,
    @Valid @RequestBody FreezeAccountRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.requestFreeze(id, request, callerUserId);
  }

  @PostMapping("/{id}/unfreeze")
  public OtpChallengeResponse requestUnfreeze(
    @PathVariable String id,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.requestUnfreeze(id, callerUserId);
  }

  @PostMapping("/changes/{changeRequestId}/confirm")
  public AccountDetailResponse confirmChange(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return accountsService.confirmChange(callerUserId, changeRequestId, request);
  }

  @GetMapping("/{id}/statement")
  public ResponseEntity<byte[]> downloadStatement(
    @PathVariable String id,
    @RequestHeader(value = "X-User-Id", required = false) String callerUserId
  ) {
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"securebank-statement.pdf\"")
      .contentType(MediaType.APPLICATION_PDF)
      .body(accountsService.downloadStatement(callerUserId, id));
  }
}
