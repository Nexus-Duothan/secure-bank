package com.securebank.payments.service;

import com.securebank.payments.dto.MerchantRegisterRequest;
import com.securebank.payments.dto.MerchantResponse;
import com.securebank.payments.entity.Merchant;
import com.securebank.payments.exception.MerchantInactiveException;
import com.securebank.payments.exception.ResourceNotFoundException;
import com.securebank.payments.repository.MerchantRepository;
import java.security.SecureRandom;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantService {

  private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 6;
  private static final SecureRandom RANDOM = new SecureRandom();

  private final MerchantRepository merchantRepository;

  @Transactional
  public MerchantResponse register(UUID merchantUserId, MerchantRegisterRequest request) {
    if (merchantRepository.findByMerchantUserId(merchantUserId).isPresent()) {
      throw new IllegalArgumentException("This user has already registered a merchant profile");
    }

    Merchant merchant = Merchant.builder()
      .merchantCode(generateUniqueMerchantCode())
      .businessName(request.getBusinessName())
      .category(request.getCategory())
      .settlementAccountId(request.getSettlementAccountId())
      .merchantUserId(merchantUserId)
      .active(true)
      .build();

    return toResponse(merchantRepository.save(merchant));
  }

  @Transactional(readOnly = true)
  public MerchantResponse getByCode(String merchantCode) {
    return toResponse(findActiveByCode(merchantCode));
  }

  @Transactional(readOnly = true)
  public Merchant findActiveByCode(String merchantCode) {
    Merchant merchant = merchantRepository
      .findByMerchantCode(merchantCode)
      .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + merchantCode));
    if (!merchant.isActive()) {
      throw new MerchantInactiveException(
        "Merchant is not currently accepting payments: " + merchantCode
      );
    }
    return merchant;
  }

  private String generateUniqueMerchantCode() {
    String code;
    do {
      code = "MCH-" + randomAlphanumeric(CODE_LENGTH);
    } while (merchantRepository.existsByMerchantCode(code));
    return code;
  }

  private String randomAlphanumeric(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
    }
    return sb.toString();
  }

  private MerchantResponse toResponse(Merchant merchant) {
    return MerchantResponse.builder()
      .id(merchant.getId())
      .merchantCode(merchant.getMerchantCode())
      .businessName(merchant.getBusinessName())
      .category(merchant.getCategory())
      .active(merchant.isActive())
      .createdAt(merchant.getCreatedAt())
      .build();
  }
}
