package com.inventory.payment.repository;

import com.inventory.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByBusinessIdAndDocumentTypeAndDocumentIdOrderByPaymentDateDescCreatedAtDesc(
        UUID businessId,
        String documentType,
        UUID documentId
    );

    @Query("""
        select coalesce(sum(p.amount), 0)
        from Payment p
        where p.businessId = :businessId
          and p.documentType = :documentType
          and p.documentId = :documentId
        """)
    BigDecimal sumAmountForDocument(
        @Param("businessId") UUID businessId,
        @Param("documentType") String documentType,
        @Param("documentId") UUID documentId
    );
}
