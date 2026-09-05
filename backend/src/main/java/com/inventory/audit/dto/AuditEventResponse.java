package com.inventory.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    OffsetDateTime createdAt,
    UUID userId,
    String action,
    String entityType,
    UUID entityId,
    String summary
) {
}
