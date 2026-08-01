package com.securebank.accounts;

import java.math.BigDecimal;
import java.util.List;

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
  String ownershipLabel,
  String status,
  boolean frozen,
  String freezeReason,
  List<BankCardResponse> cards
) {}
