package com.securebank.payments.repository;

import com.securebank.payments.entity.BillPayment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillPaymentRepository extends JpaRepository<BillPayment, UUID> {}
