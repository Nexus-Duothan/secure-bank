package com.securebank.transfer.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/** The subset of accounts-service's account view that transfer-service needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountSnapshot(String id, BigDecimal balance, String currency) {}
