package com.securebank.accounts;

public record BankCardResponse(
  String id,
  String accountId,
  String cardType,
  String productName,
  String maskedNumber,
  String cardholderName,
  String expiryDate,
  String scheme,
  String status,
  boolean jointAccountCard
) {}
