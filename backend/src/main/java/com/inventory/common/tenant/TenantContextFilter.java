package com.inventory.common.tenant;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.inventory.auth.repository.BusinessMembershipRepository;
import com.inventory.auth.security.AuthenticatedUser;
import com.inventory.config.TenantProperties;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final TenantProperties tenantProperties;
    private final BusinessMembershipRepository businessMembershipRepository;

    public TenantContextFilter(
        TenantProperties tenantProperties,
        BusinessMembershipRepository businessMembershipRepository
    ) {
        this.tenantProperties = tenantProperties;
        this.businessMembershipRepository = businessMembershipRepository;
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
                try {
                    UUID businessId = UUID.fromString(headerValue);
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication is required");
                        return;
                    }
                    if (businessMembershipRepository.findByBusiness_IdAndUser_IdAndActiveTrue(businessId, user.userId()).isEmpty()) {
                        response.sendError(HttpServletResponse.SC_FORBIDDEN, "You do not have access to this business");
                        return;
                    }
                    TenantContext.setBusinessId(businessId);
                } catch (IllegalArgumentException exception) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid tenant header value");
                    return;
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
