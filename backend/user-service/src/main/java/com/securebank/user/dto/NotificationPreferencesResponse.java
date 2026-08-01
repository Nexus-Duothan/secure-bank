package com.securebank.user.dto;

public record NotificationPreferencesResponse(boolean email, boolean sms, boolean push) {}
