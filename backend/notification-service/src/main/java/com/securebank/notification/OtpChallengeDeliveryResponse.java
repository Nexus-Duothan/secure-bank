package com.securebank.notification;

import java.util.List;

public record OtpChallengeDeliveryResponse(
  String status,
  List<String> channelsQueued,
  String notificationId
) {}
