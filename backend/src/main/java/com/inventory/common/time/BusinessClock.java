package com.inventory.common.time;

import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.common.tenant.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BusinessClock {

    private final BusinessRepository businessRepository;

    public BusinessClock(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    public LocalDate today() {
        return LocalDate.now(zoneId());
    }

    public ZoneId zoneId() {
        UUID businessId = TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("Business not found"));
        return ZoneId.of(business.getTimeZone());
    }
}
