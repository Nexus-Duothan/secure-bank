package com.securebank.user.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record DeviceActionRequest(@NotNull UUID deviceId) {}
