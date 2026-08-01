package com.securebank.accounts;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {

  private static final AccountResponse PRIMARY_ACCOUNT = new AccountResponse(
    "acc-demo-primary",
    "Everyday Current",
    "CURRENT",
    "67",
    new BigDecimal("48231.76"),
    "LKR",
    2.4,
    "2m ago"
  );

  private static final AccountDetailResponse PRIMARY_ACCOUNT_DETAIL = new AccountDetailResponse(
    PRIMARY_ACCOUNT.id(),
    PRIMARY_ACCOUNT.nickname(),
    "Everyday Current",
    PRIMARY_ACCOUNT.currency(),
    PRIMARY_ACCOUNT.balance(),
    "1234 5678 90067",
    "SBLK0007",
    "14 Mar 2024",
    "Kandy City",
    "Active - Verified"
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

  @GetMapping("/primary")
  public AccountResponse getPrimaryAccount() {
    return PRIMARY_ACCOUNT;
  }

  @GetMapping("/primary/transactions")
  public List<TransactionResponse> getRecentTransactions(
    @RequestParam(defaultValue = "4") int limit
  ) {
    int safeLimit = Math.min(Math.max(limit, 1), RECENT_TRANSACTIONS.size());
    return RECENT_TRANSACTIONS.subList(0, safeLimit);
  }

  @GetMapping("/{id}")
  public AccountDetailResponse getAccountById(@PathVariable String id) {
    return new AccountDetailResponse(
      id,
      PRIMARY_ACCOUNT_DETAIL.nickname(),
      PRIMARY_ACCOUNT_DETAIL.accountTypeLabel(),
      PRIMARY_ACCOUNT_DETAIL.currency(),
      PRIMARY_ACCOUNT_DETAIL.balance(),
      PRIMARY_ACCOUNT_DETAIL.accountNumber(),
      PRIMARY_ACCOUNT_DETAIL.ifscCode(),
      PRIMARY_ACCOUNT_DETAIL.openedOn(),
      PRIMARY_ACCOUNT_DETAIL.homeBranch(),
      PRIMARY_ACCOUNT_DETAIL.status()
    );
  }

  public record AccountResponse(
    String id,
    String nickname,
    String accountType,
    String lastFourDigits,
    BigDecimal balance,
    String currency,
    double monthlyChangePercent,
    String verifiedLabel
  ) {}

  public record TransactionResponse(
    String id,
    String merchant,
    String category,
    String date,
    BigDecimal amount,
    boolean verified
  ) {}

  public record AccountDetailResponse(
    String id,
    String nickname,
    String accountTypeLabel,
    String currency,
    BigDecimal balance,
    String accountNumber,
    String ifscCode,
    String openedOn,
    String homeBranch,
    String status
  ) {}
}
