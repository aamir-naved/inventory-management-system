package com.inventory.settings.service;

import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.common.tenant.TenantContext;
import com.inventory.settings.dto.SettingsRequest;
import com.inventory.settings.dto.SettingsResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class SettingsService {

    private static final Set<String> SUPPORTED_DATE_FORMATS = Set.of(
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy-MM-dd",
        "dd-MMM-yyyy"
    );

    private final BusinessRepository businessRepository;

    public SettingsService(BusinessRepository businessRepository) {
        this.businessRepository = businessRepository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return toResponse(findCurrentBusiness());
    }

    public SettingsResponse update(SettingsRequest request) {
        Business business = findCurrentBusiness();
        business.setCurrencyCode(request.currencyCode().trim());
        business.setDateFormat(validateDateFormat(request.dateFormat().trim()));
        business.setAllowNegativeStock(Boolean.TRUE.equals(request.allowNegativeStock()));
        business.setDefaultLowStockThreshold(request.defaultLowStockThreshold());
        if (request.gstEnabled() != null) {
            business.setGstEnabled(request.gstEnabled());
        }
        if (request.gstin() != null) {
            business.setGstin(normalizeGstin(request.gstin()));
        }
        if (request.stateCode() != null) {
            business.setStateCode(normalizeStateCode(request.stateCode()));
        }
        if (request.stateName() != null) {
            business.setStateName(normalizeName(request.stateName()));
        }
        if (request.gstInclusivePricing() != null) {
            business.setGstInclusivePricing(request.gstInclusivePricing());
        }
        return toResponse(business);
    }

    @Transactional(readOnly = true)
    public boolean isNegativeStockAllowed() {
        return findCurrentBusiness().isAllowNegativeStock();
    }

    @Transactional(readOnly = true)
    public boolean isGstEnabled() {
        return findCurrentBusiness().isGstEnabled();
    }

    @Transactional(readOnly = true)
    public boolean isGstInclusivePricing() {
        return findCurrentBusiness().isGstInclusivePricing();
    }

    @Transactional(readOnly = true)
    public String getStateCode() {
        return findCurrentBusiness().getStateCode();
    }

    private Business findCurrentBusiness() {
        UUID businessId = TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalStateException("Business context is required"));

        return businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("Business not found"));
    }

    private String validateDateFormat(String dateFormat) {
        if (!SUPPORTED_DATE_FORMATS.contains(dateFormat)) {
            throw new IllegalArgumentException(
                "Date format must be one of: dd/MM/yyyy, MM/dd/yyyy, yyyy-MM-dd, dd-MMM-yyyy"
            );
        }
        return dateFormat;
    }

    private SettingsResponse toResponse(Business business) {
        return new SettingsResponse(
            business.getId(),
            business.getCurrencyCode(),
            business.getDateFormat(),
            business.isAllowNegativeStock(),
            business.getDefaultLowStockThreshold(),
            business.isGstEnabled(),
            business.getGstin(),
            business.getStateCode(),
            business.getStateName(),
            business.isGstInclusivePricing()
        );
    }

    private String normalizeGstin(String value) {
        String trimmed = value.trim().toUpperCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeStateCode(String value) {
        String trimmed = value.trim().toUpperCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeName(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
