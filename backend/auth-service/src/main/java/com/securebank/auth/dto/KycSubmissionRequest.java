package com.securebank.auth.dto;

import com.securebank.auth.enums.DocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmissionRequest {

  @NotNull(message = "Document type is required")
  private DocumentType documentType;

  @NotBlank(message = "Document number is required")
  private String documentNumber;

  @NotBlank(message = "Document payload or file URL is required")
  private String documentPayload;
}
