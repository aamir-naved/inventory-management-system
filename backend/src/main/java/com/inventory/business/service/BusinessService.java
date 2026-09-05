package com.inventory.business.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.security.CurrentUser;
import com.inventory.business.dto.BusinessRequest;
import com.inventory.business.dto.BusinessResponse;
import com.inventory.business.dto.QuickStartRequest;
import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.config.RegistrationPolicy;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUser currentUser;
    private final ShopOnboardingService shopOnboardingService;
    private final RegistrationPolicy registrationPolicy;

    public BusinessService(
        BusinessRepository businessRepository,
        BusinessMembershipRepository businessMembershipRepository,
        UserAccountRepository userAccountRepository,
        CurrentUser currentUser,
        ShopOnboardingService shopOnboardingService,
        RegistrationPolicy registrationPolicy
    ) {
        this.businessRepository = businessRepository;
        this.businessMembershipRepository = businessMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUser = currentUser;
        this.shopOnboardingService = shopOnboardingService;
        this.registrationPolicy = registrationPolicy;
    }

    public BusinessResponse quickStart(QuickStartRequest request) {
        BusinessResponse created = create(new BusinessRequest(
            request.shopName(),
            "Shop",
            null,
            request.mobileNumber(),
            "INR",
            "Asia/Kolkata",
            false,
            null,
            null,
            null,
            false
        ));
        shopOnboardingService.provisionNewShop(created.id(), currentUser.requireUserId());
        return created;
    }

    public BusinessResponse create(BusinessRequest request) {
        registrationPolicy.requireOpenSelfServe("Self-serve shop creation is disabled. Ask the operator to create your shop.");
        UUID userId = currentUser.requireUserId();
        if (businessMembershipRepository.existsByUser_IdAndActiveTrue(userId)) {
            throw new IllegalArgumentException("This account already has a business");
        }

        Business business = new Business();
        applyRequest(business, request);
        business.setAllowNegativeStock(false);
        business.setDefaultLowStockThreshold(BigDecimal.ZERO);
        business.setDateFormat("dd/MM/yyyy");
        business.setActive(true);
        business.setPlanCode("standard");

        Business savedBusiness = businessRepository.save(business);

        UserAccount userAccount = userAccountRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        BusinessMembership membership = new BusinessMembership();
        membership.setBusiness(savedBusiness);
        membership.setUser(userAccount);
        membership.setRole("OWNER");
        membership.setActive(true);
        businessMembershipRepository.save(membership);

        return toResponse(savedBusiness);
    }

    @Transactional(readOnly = true)
    public BusinessResponse getById(UUID id) {
        Business business = findOwnedBusiness(id);

        return toResponse(business);
    }

    public BusinessResponse update(UUID id, BusinessRequest request) {
        Business business = findOwnedBusiness(id);

        applyRequest(business, request);

        return toResponse(business);
    }

    private Business findOwnedBusiness(UUID id) {
        UUID userId = currentUser.requireUserId();
        if (businessMembershipRepository.findByBusiness_IdAndUser_IdAndActiveTrue(id, userId).isEmpty()) {
            throw new EntityNotFoundException("Business not found");
        }

        return businessRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Business not found"));
    }

    private void applyRequest(Business business, BusinessRequest request) {
        business.setName(request.name().trim());
        business.setBusinessType(request.businessType().trim());
        business.setAddressLine(normalize(request.addressLine()));
        business.setMobileNumber(request.mobileNumber().trim());
        business.setCurrencyCode(request.currencyCode().trim());
        business.setTimeZone(validateTimeZone(request.timeZone().trim()));
        if (request.gstEnabled() != null) {
            business.setGstEnabled(request.gstEnabled());
        }
        business.setGstin(normalizeGstin(request.gstin()));
        business.setStateCode(normalizeStateCode(request.stateCode()));
        business.setStateName(normalize(request.stateName()));
        if (request.gstInclusivePricing() != null) {
            business.setGstInclusivePricing(request.gstInclusivePricing());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String validateTimeZone(String timeZone) {
        try {
            java.time.ZoneId.of(timeZone);
            return timeZone;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Time zone must be a valid IANA zone");
        }
    }

    private BusinessResponse toResponse(Business business) {
        return new BusinessResponse(
            business.getId(),
            business.getName(),
            business.getBusinessType(),
            business.getAddressLine(),
            business.getMobileNumber(),
            business.getCurrencyCode(),
            business.getTimeZone(),
            business.isGstEnabled(),
            business.getGstin(),
            business.getStateCode(),
            business.getStateName(),
            business.isGstInclusivePricing(),
            business.getLogoContentType() != null && business.getLogoBytes() != null,
            business.isActive(),
            business.getCreatedAt(),
            business.getUpdatedAt()
        );
    }

    private String normalizeGstin(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeStateCode(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    public BusinessResponse uploadLogo(UUID id, byte[] bytes, String contentType) {
        Business business = findOwnedBusiness(id);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Choose a PNG or JPEG logo");
        }
        if (bytes.length > 512 * 1024) {
            throw new IllegalArgumentException("Logo must be 512 KB or smaller");
        }
        if (contentType == null || !(contentType.equals("image/png") || contentType.equals("image/jpeg"))) {
            throw new IllegalArgumentException("Logo must be a PNG or JPEG image");
        }
        business.setLogoBytes(bytes);
        business.setLogoContentType(contentType);
        return toResponse(business);
    }

    public void removeLogo(UUID id) {
        Business business = findOwnedBusiness(id);
        business.setLogoBytes(null);
        business.setLogoContentType(null);
    }

    public byte[] logoBytes(UUID id) {
        Business business = findOwnedBusiness(id);
        if (business.getLogoBytes() == null) {
            throw new EntityNotFoundException("Logo not found");
        }
        return business.getLogoBytes();
    }

    public String logoContentType(UUID id) {
        Business business = findOwnedBusiness(id);
        return business.getLogoContentType() == null ? "image/png" : business.getLogoContentType();
    }
}
