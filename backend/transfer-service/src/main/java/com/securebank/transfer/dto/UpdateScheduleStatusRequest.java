package com.securebank.transfer.dto;

import com.securebank.transfer.enums.ScheduleStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateScheduleStatusRequest(@NotNull ScheduleStatus status) {}
