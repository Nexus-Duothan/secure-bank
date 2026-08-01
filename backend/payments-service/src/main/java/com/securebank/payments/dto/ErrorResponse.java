package com.securebank.payments.dto;

import java.time.Instant;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

  private Instant timestamp;
  private int status;
  private String error;
  private String message;
  private String path;
}
