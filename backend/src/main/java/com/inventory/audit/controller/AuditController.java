package com.inventory.audit.controller;

import com.inventory.audit.dto.AuditEventResponse;
import com.inventory.audit.service.AuditService;
import com.inventory.common.api.PagedResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PagedResponse<AuditEventResponse> list(
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return auditService.list(page, size);
    }
}
