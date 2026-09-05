package com.inventory.audit.service;

import com.inventory.audit.dto.AuditEventResponse;
import com.inventory.audit.entity.AuditEvent;
import com.inventory.audit.repository.AuditEventRepository;
import com.inventory.auth.entity.MembershipRole;
import com.inventory.common.api.ForbiddenException;
import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.common.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Transactional
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String action, String entityType, UUID entityId, String summary) {
        UUID businessId = TenantContext.getBusinessId().orElse(null);
        if (businessId == null) {
            return;
        }
        AuditEvent event = new AuditEvent();
        event.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        event.setBusinessId(businessId);
        event.setUserId(TenantContext.getUserId().orElse(null));
        event.setAction(action);
        event.setEntityType(entityType);
        event.setEntityId(entityId);
        event.setSummary(summary == null ? action : summary.substring(0, Math.min(summary.length(), 500)));
        auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AuditEventResponse> list(Integer page, Integer size) {
        MembershipRole role = TenantContext.getRole().orElse(MembershipRole.CLERK);
        if (!role.atLeast(MembershipRole.MANAGER)) {
            throw new ForbiddenException("Only managers and owners can view the activity log");
        }
        UUID businessId = TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
        return Pagination.map(
            auditEventRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, Pagination.pageable(page, size)),
            event -> new AuditEventResponse(
                event.getId(),
                event.getCreatedAt(),
                event.getUserId(),
                event.getAction(),
                event.getEntityType(),
                event.getEntityId(),
                event.getSummary()
            )
        );
    }
}
