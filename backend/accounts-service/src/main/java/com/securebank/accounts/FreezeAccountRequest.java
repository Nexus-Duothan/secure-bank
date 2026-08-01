package com.securebank.accounts;

import jakarta.validation.constraints.Size;

public record FreezeAccountRequest(@Size(max = 180) String reason) {}
