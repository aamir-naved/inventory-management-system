package com.inventory.common.tenant;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.inventory.auth.entity.BusinessMembership;
import com.inventory.auth.entity.MembershipRole;
import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.security.AuthenticatedUser;
import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.config.TenantProperties;
import com.inventory.security.FilterResponses;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantProperties tenantProperties;
    private final BusinessMembershipRepository businessMembershipRepository;
    private final BusinessRepository businessRepository;

    public TenantContextFilter(
        TenantProperties tenantProperties,
        BusinessMembershipRepository businessMembershipRepository,
        BusinessRepository businessRepository
    ) {
        this.tenantProperties = tenantProperties;
        this.businessMembershipRepository = businessMembershipRepository;
        this.businessRepository = businessRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String headerValue = request.getHeader(tenantProperties.getHeaderName());

        try {
            if (StringUtils.hasText(headerValue)) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
                    FilterResponses.json(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
                    return;
                }
                if (user.platformAdmin()) {
                    FilterResponses.json(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "Platform admins cannot access shop data"
                    );
                    return;
                }
                try {
                    UUID businessId = UUID.fromString(headerValue);
                    BusinessMembership membership = businessMembershipRepository
                        .findByBusiness_IdAndUser_IdAndActiveTrue(businessId, user.userId())
                        .orElse(null);
                    if (membership == null) {
                        FilterResponses.json(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "You do not have access to this business"
                        );
                        return;
                    }
                    Business business = businessRepository.findById(businessId).orElse(null);
                    if (business == null || !business.isActive()) {
                        FilterResponses.json(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "This shop is suspended"
                        );
                        return;
                    }
                    TenantContext.set(businessId, MembershipRole.from(membership.getRole()), user.userId());
                } catch (IllegalArgumentException exception) {
                    FilterResponses.json(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant header value");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
