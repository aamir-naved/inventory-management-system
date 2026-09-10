package com.inventory.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID businessId,
    String partyType,
    UUID partyId,
    String documentType,
    UUID documentId,
    LocalDate paymentDate,
    BigDecimal amount,
    String paymentKind,
    String notes,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
