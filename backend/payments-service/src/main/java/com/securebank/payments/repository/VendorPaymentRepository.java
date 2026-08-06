package com.securebank.payments.repository;

import com.securebank.payments.entity.VendorPayment;
import com.securebank.payments.enums.PaymentStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendorPaymentRepository extends JpaRepository<VendorPayment, UUID> {
  Page<VendorPayment> findByPayerUserId(UUID payerUserId, Pageable pageable);

  Page<VendorPayment> findByPayerUserIdAndStatus(
    UUID payerUserId,
    PaymentStatus status,
    Pageable pageable
  );

  List<VendorPayment> findByStatus(PaymentStatus status);

  List<VendorPayment> findByMerchantMerchantUserIdOrderByCreatedAtDesc(UUID merchantUserId);

  boolean existsByReferenceNumber(String referenceNumber);
}
