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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountsController {

  private final AccountsService accountsService;

  @GetMapping
  public List<AccountResponse> getLinkedAccounts() {
    return accountsService.getLinkedAccounts();
  }

  @GetMapping("/primary")
  public AccountResponse getPrimaryAccount() {
    return accountsService.getPrimaryAccount();
  }

  @GetMapping("/primary/transactions")
  public List<TransactionResponse> getRecentTransactions(
    @RequestParam(defaultValue = "4") int limit
  ) {
    return accountsService.getRecentTransactions(limit);
  }

  @GetMapping("/{id}/transactions/recent")
  public List<TransactionResponse> getRecentTransactions(
    @PathVariable String id,
    @RequestParam(defaultValue = "4") int limit
  ) {
    return accountsService.getRecentTransactions(id, limit);
  }

  @GetMapping("/products")
  public List<AccountProductResponse> getAccountProducts(
    @RequestParam(required = false) String accountType
  ) {
    return accountsService.getAccountProducts(accountType);
  }

  @PostMapping("/link")
  public OtpChallengeResponse requestAccountLink(@Valid @RequestBody LinkAccountRequest request) {
    return accountsService.requestAccountLink(request);
  }

  @PostMapping("/link/{changeRequestId}/confirm")
  public LinkedAccountResponse confirmAccountLink(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request
  ) {
    return accountsService.confirmAccountLink(changeRequestId, request);
  }

  @PostMapping("/open")
  public OtpChallengeResponse requestAccountOpening(
    @Valid @RequestBody OpenAccountRequest request
  ) {
    return accountsService.requestAccountOpening(request);
  }

  @PostMapping("/open/{changeRequestId}/confirm")
  public LinkedAccountResponse confirmAccountOpening(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request
  ) {
    return accountsService.confirmAccountOpening(changeRequestId, request);
  }

  @PostMapping("/cards/link")
  public OtpChallengeResponse requestCreditCardLink(
    @Valid @RequestBody LinkCreditCardRequest request
  ) {
    return accountsService.requestCreditCardLink(request);
  }

  @PostMapping("/cards/link/{changeRequestId}/confirm")
  public LinkedCardResponse confirmCreditCardLink(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request
  ) {
    return accountsService.confirmCreditCardLink(changeRequestId, request);
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
    @RequestParam(defaultValue = "false") boolean flaggedOnly
  ) {
    return accountsService.getTransactionHistory(
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
  public AccountDetailResponse getAccountById(@PathVariable String id) {
    return accountsService.getAccountById(id);
  }

  @PostMapping("/{id}/freeze")
  public OtpChallengeResponse requestFreeze(
    @PathVariable String id,
    @Valid @RequestBody FreezeAccountRequest request
  ) {
    return accountsService.requestFreeze(id, request);
  }

  @PostMapping("/{id}/unfreeze")
  public OtpChallengeResponse requestUnfreeze(@PathVariable String id) {
    return accountsService.requestUnfreeze(id);
  }

  @PostMapping("/changes/{changeRequestId}/confirm")
  public AccountDetailResponse confirmChange(
    @PathVariable UUID changeRequestId,
    @Valid @RequestBody ConfirmChangeRequest request
  ) {
    return accountsService.confirmChange(changeRequestId, request);
  }

  @GetMapping("/{id}/statement")
  public ResponseEntity<byte[]> downloadStatement(@PathVariable String id) {
    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"securebank-statement.pdf\"")
      .contentType(MediaType.APPLICATION_PDF)
      .body(accountsService.downloadStatement(id));
  }
}
