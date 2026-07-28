package com.securebank.auth.service;

import com.securebank.auth.dto.KycApplicationResponse;
import com.securebank.auth.dto.KycSubmissionRequest;
import com.securebank.auth.dto.OfficerKycReviewRequest;
import com.securebank.auth.entity.KycApplication;
import com.securebank.auth.entity.UserCredential;
import com.securebank.auth.enums.KycStatus;
import com.securebank.auth.enums.UserStatus;
import com.securebank.auth.repository.KycApplicationRepository;
import com.securebank.auth.repository.UserCredentialRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KycService {

  private final KycApplicationRepository kycApplicationRepository;
  private final UserCredentialRepository userCredentialRepository;

  @Transactional
  public KycApplicationResponse submitKyc(UUID userId, KycSubmissionRequest request) {
    UserCredential user = userCredentialRepository
      .findById(userId)
      .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (user.getStatus() == UserStatus.ACTIVE) {
      throw new IllegalStateException("Account is already verified and active");
    }

    KycApplication application = kycApplicationRepository
      .findByUserId(userId)
      .orElse(KycApplication.builder().userId(userId).build());

    application.setDocumentType(request.getDocumentType());
    application.setDocumentNumber(request.getDocumentNumber());
    application.setDocumentPayload(request.getDocumentPayload());
    application.setStatus(KycStatus.UNDER_REVIEW);
    application.setRejectionReason(null);

    KycApplication saved = kycApplicationRepository.save(application);

    user.setStatus(UserStatus.UNDER_REVIEW);
    userCredentialRepository.save(user);

    return mapToResponse(saved);
  }

  @Transactional(readOnly = true)
  public List<KycApplicationResponse> getPendingApplications() {
    return kycApplicationRepository
      .findByStatus(KycStatus.UNDER_REVIEW)
      .stream()
      .map(this::mapToResponse)
      .collect(Collectors.toList());
  }

  @Transactional
  public KycApplicationResponse reviewKycApplication(
    UUID applicationId,
    String officerUsername,
    OfficerKycReviewRequest request
  ) {
    KycApplication application = kycApplicationRepository
      .findById(applicationId)
      .orElseThrow(() -> new IllegalArgumentException("KYC application not found"));

    UserCredential user = userCredentialRepository
      .findById(application.getUserId())
      .orElseThrow(() -> new IllegalArgumentException("Associated user not found"));

    if (request.getAction() == KycStatus.APPROVED) {
      application.setStatus(KycStatus.APPROVED);
      application.setRejectionReason(null);
      user.setStatus(UserStatus.ACTIVE);
    } else if (request.getAction() == KycStatus.REJECTED) {
      application.setStatus(KycStatus.REJECTED);
      application.setRejectionReason(
        request.getRejectionReason() != null ? request.getRejectionReason() : "Rejected by officer"
      );
      user.setStatus(UserStatus.REJECTED);
    } else {
      throw new IllegalArgumentException("Invalid review action. Must be APPROVED or REJECTED");
    }

    application.setReviewedAt(Instant.now());
    application.setReviewedBy(officerUsername);

    userCredentialRepository.save(user);
    KycApplication saved = kycApplicationRepository.save(application);

    return mapToResponse(saved);
  }

  private KycApplicationResponse mapToResponse(KycApplication app) {
    return KycApplicationResponse.builder()
      .applicationId(app.getId())
      .userId(app.getUserId())
      .documentType(app.getDocumentType())
      .documentNumber(app.getDocumentNumber())
      .status(app.getStatus())
      .rejectionReason(app.getRejectionReason())
      .submittedAt(app.getSubmittedAt())
      .reviewedAt(app.getReviewedAt())
      .reviewedBy(app.getReviewedBy())
      .build();
  }
}
