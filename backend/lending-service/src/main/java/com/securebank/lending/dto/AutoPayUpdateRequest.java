package com.securebank.lending.dto;

import jakarta.validation.constraints.NotNull;

public record AutoPayUpdateRequest(@NotNull Boolean enabled) {}
