package com.inventory.staff.service;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.audit.service.AuditService;
import com.inventory.auth.dto.AuthResponse;
import com.inventory.auth.entity.AuthTokenType;
import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.MembershipRole;
import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.mail.MailMessage;
import com.inventory.auth.mail.MailService;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.auth.security.JwtService;
import com.inventory.auth.service.AuthTokenService;
import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.common.api.ForbiddenException;
import com.inventory.common.tenant.TenantContext;
import com.inventory.config.AuthProperties;
import com.inventory.staff.dto.AcceptInviteRequest;
import com.inventory.staff.dto.StaffInvitePreviewResponse;
import com.inventory.staff.dto.StaffInviteRequest;
import com.inventory.staff.dto.StaffInviteResponse;
import com.inventory.staff.dto.StaffMemberResponse;
import com.inventory.staff.dto.StaffRosterResponse;
import com.inventory.staff.entity.StaffInvite;
import com.inventory.staff.repository.StaffInviteRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
@Transactional
public class StaffService {

    private final StaffInviteRepository staffInviteRepository;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final UserAccountRepository userAccountRepository;
    private final BusinessRepository businessRepository;
    private final MailService mailService;
    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthTokenService authTokenService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    public StaffService(
        StaffInviteRepository staffInviteRepository,
        BusinessMembershipRepository businessMembershipRepository,
        UserAccountRepository userAccountRepository,
        BusinessRepository businessRepository,
        MailService mailService,
        AuthProperties authProperties,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        AuthTokenService authTokenService,
        AuditService auditService
    ) {
        this.staffInviteRepository = staffInviteRepository;
        this.businessMembershipRepository = businessMembershipRepository;
        this.userAccountRepository = userAccountRepository;
        this.businessRepository = businessRepository;
        this.mailService = mailService;
        this.authProperties = authProperties;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authTokenService = authTokenService;
        this.auditService = auditService;
    }

    public StaffInviteResponse invite(StaffInviteRequest request) {
        requireOwner();
        UUID businessId = requireBusinessId();
        MembershipRole role = MembershipRole.from(request.role());
        if (!role.isStaffInviteRole()) {
            throw new IllegalArgumentException("Invites can only be sent for MANAGER or CLERK");
        }

        String email = request.email().trim().toLowerCase();
        UserAccount existing = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null
            && businessMembershipRepository.findByBusiness_IdAndUser_IdAndActiveTrue(businessId, existing.getId()).isPresent()) {
            throw new IllegalArgumentException("That person already belongs to this business");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        staffInviteRepository.findByBusiness_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(businessId, email, now)
            .ifPresent(staffInviteRepository::delete);

        String rawToken = generateRawToken();
        Business business = businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("Business not found"));
        UserAccount inviter = userAccountRepository.findById(requireUserId())
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

        StaffInvite invite = new StaffInvite();
        invite.setBusiness(business);
        invite.setEmail(email);
        invite.setRole(role.name());
        invite.setTokenHash(AuthTokenService.hashToken(rawToken));
        invite.setInvitedBy(inviter);
        invite.setExpiresAt(now.plusDays(7));
        StaffInvite saved = staffInviteRepository.saveAndFlush(invite);

        String link = appLink("/accept-invite?token=" + rawToken);
        mailService.send(new MailMessage(
            email,
            "Join " + business.getName() + " on Inventory",
            "You have been invited as " + role.name().toLowerCase() + " for "
                + business.getName() + ".\n\nAccept the invite:\n" + link + "\n\nThis link expires in 7 days."
        ));
        auditService.record("STAFF_INVITED", "STAFF", saved.getId(), "Invited " + email + " as " + role.name());
        return toInviteResponse(saved);
    }

    @Transactional(readOnly = true)
    public StaffRosterResponse roster() {
        requireManager();
        UUID businessId = requireBusinessId();
        List<StaffMemberResponse> members = businessMembershipRepository.findAllByBusiness_Id(businessId).stream()
            .sorted(Comparator.comparing(BusinessMembership::getCreatedAt))
            .map(membership -> new StaffMemberResponse(
                membership.getId(),
                membership.getUser().getId(),
                membership.getUser().getFullName(),
                membership.getUser().getEmail(),
                membership.getRole(),
                membership.isActive()
            ))
            .toList();
        List<StaffInviteResponse> pending = staffInviteRepository
            .findAllByBusiness_IdAndAcceptedAtIsNullOrderByCreatedAtDesc(businessId)
            .stream()
            .filter(invite -> invite.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC)))
            .map(this::toInviteResponse)
            .toList();
        return new StaffRosterResponse(members, pending);
    }

    public void revokeInvite(UUID inviteId) {
        requireOwner();
        StaffInvite invite = staffInviteRepository.findById(inviteId)
            .orElseThrow(() -> new EntityNotFoundException("Invite not found"));
        if (!invite.getBusiness().getId().equals(requireBusinessId())) {
            throw new EntityNotFoundException("Invite not found");
        }
        if (invite.getAcceptedAt() != null) {
            throw new IllegalArgumentException("This invite has already been accepted");
        }
        staffInviteRepository.delete(invite);
        auditService.record("STAFF_INVITE_REVOKED", "STAFF", inviteId, "Revoked invite for " + invite.getEmail());
    }

    public StaffMemberResponse deactivateMember(UUID membershipId) {
        requireOwner();
        UUID businessId = requireBusinessId();
        BusinessMembership membership = businessMembershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Team member not found"));
        if (!membership.getBusiness().getId().equals(businessId)) {
            throw new EntityNotFoundException("Team member not found");
        }
        if (MembershipRole.OWNER.name().equals(membership.getRole())) {
            throw new IllegalArgumentException("The owner cannot be removed");
        }
        if (membership.getUser().getId().equals(requireUserId())) {
            throw new IllegalArgumentException("You cannot remove your own access");
        }
        membership.setActive(false);
        authTokenService.revokeSessions(membership.getUser());
        auditService.record(
            "STAFF_DEACTIVATED",
            "STAFF",
            membership.getId(),
            "Removed access for " + membership.getUser().getEmail()
        );
        return new StaffMemberResponse(
            membership.getId(),
            membership.getUser().getId(),
            membership.getUser().getFullName(),
            membership.getUser().getEmail(),
            membership.getRole(),
            membership.isActive()
        );
    }

    @Transactional(readOnly = true)
    public StaffInvitePreviewResponse preview(String token) {
        StaffInvite invite = requireInvite(token);
        return new StaffInvitePreviewResponse(
            invite.getBusiness().getName(),
            invite.getEmail(),
            invite.getRole(),
            invite.getExpiresAt()
        );
    }

    public AuthResponse accept(AcceptInviteRequest request) {
        StaffInvite invite = requireInvite(request.token());
        String email = invite.getEmail();
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new UserAccount();
            user.setFullName(request.fullName().trim());
            user.setEmail(email);
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            user.setEmailVerified(true);
            user.setActive(true);
            user = userAccountRepository.saveAndFlush(user);
        } else {
            if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                throw new IllegalArgumentException("An account already exists for this email. Use that password to join.");
            }
            user.setFullName(request.fullName().trim());
            user.setEmailVerified(true);
        }

        if (businessMembershipRepository.existsByUser_IdAndActiveTrue(user.getId())) {
            throw new IllegalArgumentException("This account already belongs to a business");
        }

        BusinessMembership membership = new BusinessMembership();
        membership.setBusiness(invite.getBusiness());
        membership.setUser(user);
        membership.setRole(invite.getRole());
        membership.setActive(true);
        businessMembershipRepository.save(membership);

        invite.setAcceptedAt(OffsetDateTime.now(ZoneOffset.UTC));
        staffInviteRepository.save(invite);

        JwtService.JwtToken accessToken = jwtService.issueToken(
            user.getId(),
            user.getEmail(),
            user.getTokenVersion()
        );
        AuthTokenService.IssuedToken refresh = authTokenService.issueToken(user, AuthTokenType.REFRESH);
        return new AuthResponse(
            accessToken.value(),
            "Bearer",
            accessToken.expiresAt(),
            refresh.rawToken(),
            refresh.expiresAt(),
            new AuthResponse.SessionUserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.isEmailVerified(),
                invite.getBusiness().getId(),
                invite.getBusiness().getName(),
                invite.getRole(),
                null
            )
        );
    }

    private StaffInvite requireInvite(String rawToken) {
        StaffInvite invite = staffInviteRepository.findByTokenHash(AuthTokenService.hashToken(rawToken))
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired invite"));
        if (invite.getAcceptedAt() != null || !invite.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("Invalid or expired invite");
        }
        return invite;
    }

    private StaffInviteResponse toInviteResponse(StaffInvite invite) {
        return new StaffInviteResponse(
            invite.getId(),
            invite.getEmail(),
            invite.getRole(),
            invite.getExpiresAt(),
            invite.getCreatedAt()
        );
    }

    private void requireOwner() {
        MembershipRole role = TenantContext.getRole().orElse(MembershipRole.CLERK);
        if (role != MembershipRole.OWNER) {
            throw new ForbiddenException("Only the owner can manage staff invites");
        }
    }

    private void requireManager() {
        MembershipRole role = TenantContext.getRole().orElse(MembershipRole.CLERK);
        if (!role.atLeast(MembershipRole.MANAGER)) {
            throw new ForbiddenException("Managers and owners can view the team");
        }
    }

    private UUID requireBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
    }

    private UUID requireUserId() {
        return TenantContext.getUserId()
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user is required"));
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String appLink(String path) {
        String base = authProperties.getPublicAppUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }
}
