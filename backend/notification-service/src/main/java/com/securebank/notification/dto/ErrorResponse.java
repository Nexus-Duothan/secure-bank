package com.securebank.notification.dto;

import java.time.Instant;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {

  private int status;
  private String error;
  private String message;
  private String path;

  @Builder.Default
  private Instant timestamp = Instant.now();
}
