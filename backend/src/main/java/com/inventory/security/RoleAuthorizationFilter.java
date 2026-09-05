package com.inventory.security;

import java.io.IOException;

import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import com.inventory.auth.entity.MembershipRole;
import com.inventory.auth.security.AuthenticatedUser;
import com.inventory.common.tenant.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RoleAuthorizationFilter extends OncePerRequestFilter {

    private static final UrlPathHelper URL_PATH_HELPER = new UrlPathHelper();

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = URL_PATH_HELPER.getPathWithinApplication(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser user = authentication != null
                && authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser
            ? authenticatedUser
            : null;

        if (path.startsWith("/platform")) {
            if (user == null) {
                filterChain.doFilter(request, response);
                return;
            }
            if (!user.platformAdmin()) {
                FilterResponses.json(response, HttpServletResponse.SC_FORBIDDEN, "Platform admin access is required");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (user != null && user.platformAdmin()) {
            if (path.startsWith("/auth") || path.startsWith("/actuator")) {
                filterChain.doFilter(request, response);
                return;
            }
            FilterResponses.json(response, HttpServletResponse.SC_FORBIDDEN, "Platform admins cannot access shop data");
            return;
        }

        MembershipRole role = TenantContext.getRole().orElse(null);
        if (role == null || role.atLeast(MembershipRole.MANAGER)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (clerkAllowed(request.getMethod(), path)) {
            filterChain.doFilter(request, response);
            return;
        }

        FilterResponses.json(response, HttpServletResponse.SC_FORBIDDEN, "Your role cannot perform this action");
    }

    static boolean clerkAllowed(String method, String path) {
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        if (path.startsWith("/auth") || path.startsWith("/actuator")) {
            return true;
        }
        if (httpMethod == HttpMethod.GET && path.matches("/businesses/[0-9a-fA-F-]+(/logo)?")) {
            return true;
        }
        if (httpMethod == HttpMethod.GET && path.equals("/settings")) {
            return true;
        }
        if (httpMethod == HttpMethod.GET && path.startsWith("/dashboard")) {
            return true;
        }
        if (httpMethod == HttpMethod.GET && path.startsWith("/notifications")) {
            return true;
        }
        if (path.startsWith("/customers")) {
            return true;
        }
        if (path.startsWith("/sales")) {
            return !path.endsWith("/cancel");
        }
        if (httpMethod == HttpMethod.GET && (path.equals("/products") || path.startsWith("/products/"))) {
            return !path.contains("/import") && !path.contains("/export") && !path.contains("/archive");
        }
        if (httpMethod == HttpMethod.GET && path.startsWith("/inventory")) {
            return true;
        }
        return false;
    }
}
