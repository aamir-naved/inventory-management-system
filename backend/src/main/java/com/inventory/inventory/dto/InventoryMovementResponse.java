package com.inventory.inventory.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovementResponse(
    UUID id,
    UUID productId,
    String productName,
    String movementType,
    BigDecimal quantityChange,
    BigDecimal quantityBefore,
    BigDecimal quantityAfter,
    String notes,
    OffsetDateTime createdAt
) {
}
