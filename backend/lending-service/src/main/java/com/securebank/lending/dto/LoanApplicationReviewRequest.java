package com.securebank.lending.dto;

import jakarta.validation.constraints.NotNull;

public record LoanApplicationReviewRequest(@NotNull Boolean approve, String note) {}
