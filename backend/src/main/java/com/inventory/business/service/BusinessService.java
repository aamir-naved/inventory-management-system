package com.inventory.business.service;

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
import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUser currentUser;

    public BusinessService(
        BusinessRepository businessRepository,
        BusinessMembershipRepository businessMembershipRepository,
        UserAccountRepository userAccountRepository,
        CurrentUser currentUser
    ) {
        this.businessRepository = businessRepository;
        this.businessMembershipRepository = businessMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUser = currentUser;
    }

    public BusinessResponse create(BusinessRequest request) {
        UUID userId = currentUser.requireUserId();
        if (businessMembershipRepository.existsByUser_IdAndActiveTrue(userId)) {
            throw new IllegalArgumentException("This account already has a business");
        }

        Business business = new Business();
        applyRequest(business, request);
        business.setActive(true);

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
            business.isActive(),
            business.getCreatedAt(),
            business.getUpdatedAt()
        );
    }
}
