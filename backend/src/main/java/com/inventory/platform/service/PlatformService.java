package com.inventory.platform.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.PlatformRole;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.mail.MailMessage;
import com.inventory.auth.mail.MailService;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.service.AuthTokenService;
import com.inventory.auth.support.PhoneNumbers;
import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.business.service.ShopOnboardingService;
import com.inventory.common.api.ForbiddenException;
import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.config.AuthProperties;
import com.inventory.config.RegistrationPolicy;
import com.inventory.platform.dto.PlatformCreateShopRequest;
import com.inventory.platform.dto.PlatformCreateShopResponse;
import com.inventory.platform.dto.PlatformPasswordResetResponse;
import com.inventory.platform.dto.PlatformSettingsResponse;
import com.inventory.platform.dto.PlatformShopDetailResponse;
import com.inventory.platform.dto.PlatformShopSummaryResponse;
import com.inventory.platform.dto.PlatformShopUpdateRequest;
import com.inventory.platform.dto.PlatformStatsResponse;
import com.inventory.platform.dto.PlatformUserResponse;
import com.inventory.platform.dto.PlatformUserUpdateRequest;
import com.inventory.sales.repository.SaleRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class PlatformService {

    private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
    private static final char[] PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();

    private final BusinessRepository businessRepository;
    private final UserAccountRepository userAccountRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final SaleRepository saleRepository;
    private final ShopOnboardingService shopOnboardingService;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AuthProperties authProperties;
    private final AuthTokenService authTokenService;
    private final RegistrationPolicy registrationPolicy;
    private final SecureRandom secureRandom = new SecureRandom();

    public PlatformService(
        BusinessRepository businessRepository,
        UserAccountRepository userAccountRepository,
        BusinessMembershipRepository businessMembershipRepository,
        SaleRepository saleRepository,
        ShopOnboardingService shopOnboardingService,
        PasswordEncoder passwordEncoder,
        MailService mailService,
        AuthProperties authProperties,
        AuthTokenService authTokenService,
        RegistrationPolicy registrationPolicy
    ) {
        this.businessRepository = businessRepository;
        this.userAccountRepository = userAccountRepository;
        this.businessMembershipRepository = businessMembershipRepository;
        this.saleRepository = saleRepository;
        this.shopOnboardingService = shopOnboardingService;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.authProperties = authProperties;
        this.authTokenService = authTokenService;
        this.registrationPolicy = registrationPolicy;
    }

    @Transactional(readOnly = true)
    public PlatformStatsResponse stats() {
        LocalDate today = LocalDate.now(INDIA);
        long total = businessRepository.count();
        long active = businessRepository.countByActiveTrue();
        return new PlatformStatsResponse(
            total,
            active,
            businessRepository.countByActiveFalse(),
            userAccountRepository.count(),
            saleRepository.countActiveBySaleDate(today),
            saleRepository.sumTotalBySaleDate(today)
        );
    }

    @Transactional(readOnly = true)
    public PlatformSettingsResponse settings() {
        return new PlatformSettingsResponse(registrationPolicy.isOpen());
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlatformShopSummaryResponse> listShops(String status, String search, Integer page, Integer size) {
        Boolean active = parseStatus(status);
        String term = search == null ? "" : search.trim();
        Page<Business> result = businessRepository.search(active, term, Pagination.newestFirst(page, size));
        return Pagination.map(result, this::toSummary);
    }

    @Transactional(readOnly = true)
    public PlatformShopDetailResponse getShop(UUID id) {
        return toDetail(requireBusiness(id));
    }

    public PlatformCreateShopResponse createShop(PlatformCreateShopRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userAccountRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists");
        }
        String phone = PhoneNumbers.normalize(request.phone());
        if (userAccountRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("An account with this mobile number already exists");
        }

        String password = StringUtils.hasText(request.temporaryPassword())
            ? request.temporaryPassword().trim()
            : generatePassword();
        boolean passwordProvided = StringUtils.hasText(request.temporaryPassword());

        UserAccount owner = new UserAccount();
        owner.setFullName(request.ownerName().trim());
        owner.setEmail(email);
        owner.setPhone(phone);
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setEmailVerified(true);
        owner.setActive(true);
        owner = userAccountRepository.saveAndFlush(owner);

        Business business = new Business();
        business.setName(request.shopName().trim());
        business.setBusinessType("Shop");
        business.setMobileNumber(phone);
        business.setCurrencyCode("INR");
        business.setTimeZone("Asia/Kolkata");
        business.setAllowNegativeStock(false);
        business.setDefaultLowStockThreshold(BigDecimal.ZERO);
        business.setDateFormat("dd/MM/yyyy");
        business.setActive(true);
        business.setPlanCode("standard");
        business = businessRepository.saveAndFlush(business);

        BusinessMembership membership = new BusinessMembership();
        membership.setBusiness(business);
        membership.setUser(owner);
        membership.setRole("OWNER");
        membership.setActive(true);
        businessMembershipRepository.save(membership);

        boolean provision = request.provisionStarterCatalog() == null || request.provisionStarterCatalog();
        if (provision) {
            shopOnboardingService.provisionNewShop(business.getId(), owner.getId());
        }

        String loginUrl = authProperties.getPublicAppUrl();
        mailService.send(new MailMessage(
            email,
            "Your shop on Inventory Management",
            "Shop: " + business.getName() + "\n"
                + "Sign in: " + loginUrl + "\n"
                + "Email: " + email + "\n"
                + "Temporary password: " + password + "\n\n"
                + "Change this password after you sign in."
        ));

        String delivery = passwordProvided ? "PROVIDED" : "EMAIL";
        String message = passwordProvided
            ? "Shop created. Use the temporary password you entered; it was also emailed to the owner."
            : "Shop created. A temporary password was emailed to the owner and is not returned by the API.";
        return new PlatformCreateShopResponse(toDetail(business), delivery, message);
    }

    public PlatformShopDetailResponse updateShop(UUID id, PlatformShopUpdateRequest request) {
        Business business = requireBusiness(id);
        if (request.active() != null) {
            business.setActive(request.active());
            if (request.active()) {
                business.setSuspendedReason(null);
            } else {
                String reason = request.suspendedReason() == null ? null : request.suspendedReason().trim();
                business.setSuspendedReason(StringUtils.hasText(reason) ? reason : "Suspended by platform admin");
            }
        } else if (request.suspendedReason() != null) {
            business.setSuspendedReason(blankToNull(request.suspendedReason()));
        }
        return toDetail(business);
    }

    public PlatformPasswordResetResponse resetOwnerPassword(UUID shopId) {
        Business business = requireBusiness(shopId);
        BusinessMembership ownerMembership = businessMembershipRepository
            .findFirstByBusiness_IdAndRoleAndActiveTrue(shopId, "OWNER")
            .orElseThrow(() -> new EntityNotFoundException("This shop has no active owner"));
        UserAccount owner = ownerMembership.getUser();
        String password = generatePassword();
        owner.setPasswordHash(passwordEncoder.encode(password));
        owner.setActive(true);
        authTokenService.revokeSessions(owner);
        userAccountRepository.save(owner);
        if (owner.getEmail() != null) {
            mailService.send(new MailMessage(
                owner.getEmail(),
                "Password reset for " + business.getName(),
                "A platform admin reset your password.\n\n"
                    + "Sign in: " + authProperties.getPublicAppUrl() + "\n"
                    + "Temporary password: " + password + "\n"
            ));
        }
        return new PlatformPasswordResetResponse(
            "EMAIL",
            "A temporary password was emailed to the owner and is not returned by the API."
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<PlatformUserResponse> listUsers(String search, Integer page, Integer size) {
        String term = search == null ? "" : search.trim();
        Pageable pageable = Pagination.newestFirst(page, size);
        Page<UserAccount> result = userAccountRepository.search(term, pageable);
        List<UUID> ids = result.getContent().stream().map(UserAccount::getId).toList();
        Map<UUID, BusinessMembership> memberships = ids.isEmpty()
            ? Map.of()
            : businessMembershipRepository.findActiveWithBusinessByUserIds(ids).stream()
                .collect(Collectors.toMap(membership -> membership.getUser().getId(), membership -> membership, (a, b) -> a));
        return Pagination.map(result, user -> toUser(user, memberships.get(user.getId())));
    }

    public PlatformUserResponse updateUser(UUID id, PlatformUserUpdateRequest request) {
        UserAccount user = userAccountRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (request.active() != null) {
            if (!request.active() && user.isPlatformAdmin()) {
                long admins = userAccountRepository.countByPlatformRoleAndActiveTrue(PlatformRole.PLATFORM_ADMIN);
                if (user.isActive() && admins <= 1) {
                    throw new ForbiddenException("Cannot disable the last platform admin");
                }
            }
            user.setActive(request.active());
            if (!request.active()) {
                authTokenService.revokeSessions(user);
            }
        }
        BusinessMembership membership = businessMembershipRepository.findAllByUser_Id(user.getId()).stream()
            .filter(BusinessMembership::isActive)
            .findFirst()
            .orElse(null);
        return toUser(user, membership);
    }

    private Business requireBusiness(UUID id) {
        return businessRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Shop not found"));
    }

    private PlatformShopSummaryResponse toSummary(Business business) {
        BusinessMembership owner = businessMembershipRepository
            .findFirstByBusiness_IdAndRoleAndActiveTrue(business.getId(), "OWNER")
            .orElse(null);
        return new PlatformShopSummaryResponse(
            business.getId(),
            business.getName(),
            business.getMobileNumber(),
            business.isActive(),
            business.getPlanCode(),
            owner == null ? null : owner.getUser().getFullName(),
            owner == null ? null : owner.getUser().getEmail(),
            business.getCreatedAt()
        );
    }

    private PlatformShopDetailResponse toDetail(Business business) {
        BusinessMembership owner = businessMembershipRepository
            .findFirstByBusiness_IdAndRoleAndActiveTrue(business.getId(), "OWNER")
            .orElse(null);
        long staffCount = businessMembershipRepository.countByBusiness_IdAndActiveTrue(business.getId());
        return new PlatformShopDetailResponse(
            business.getId(),
            business.getName(),
            business.getMobileNumber(),
            business.isActive(),
            business.getSuspendedReason(),
            business.getPlanCode() == null ? "standard" : business.getPlanCode(),
            owner == null ? null : owner.getUser().getFullName(),
            owner == null ? null : owner.getUser().getEmail(),
            owner == null ? null : owner.getUser().getPhone(),
            owner == null ? null : owner.getUser().getId(),
            staffCount,
            business.getCreatedAt(),
            business.getUpdatedAt()
        );
    }

    private PlatformUserResponse toUser(UserAccount user, BusinessMembership membership) {
        return new PlatformUserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getPhone(),
            user.isActive(),
            user.getPlatformRole(),
            membership == null ? null : membership.getBusiness().getId(),
            membership == null ? null : membership.getBusiness().getName(),
            membership == null ? null : membership.getRole()
        );
    }

    private Boolean parseStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }
        if ("active".equalsIgnoreCase(status.trim())) {
            return true;
        }
        if ("suspended".equalsIgnoreCase(status.trim())) {
            return false;
        }
        throw new IllegalArgumentException("Status must be active, suspended, or all");
    }

    private String generatePassword() {
        char[] buffer = new char[16];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = PASSWORD_CHARS[secureRandom.nextInt(PASSWORD_CHARS.length)];
        }
        return new String(buffer);
    }

    private static String blankToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
