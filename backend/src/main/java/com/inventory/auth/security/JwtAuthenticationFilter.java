package com.inventory.auth.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.repository.UserAccountRepository;
import com.inventory.security.FilterResponses;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;

    public JwtAuthenticationFilter(JwtService jwtService, UserAccountRepository userAccountRepository) {
        this.jwtService = jwtService;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
            try {
                AuthenticatedUser parsed = jwtService.parse(authorization.substring(7));
                UserAccount account = userAccountRepository.findById(parsed.userId()).orElse(null);
                if (account == null || !account.isActive() || account.getTokenVersion() != parsed.tokenVersion()) {
                    FilterResponses.json(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired access token");
                    return;
                }
                AuthenticatedUser user = new AuthenticatedUser(
                    account.getId(),
                    account.getEmail() != null ? account.getEmail() : account.getPhone(),
                    account.isPlatformAdmin(),
                    account.getTokenVersion()
                );
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    AuthorityUtils.createAuthorityList("ROLE_USER")
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException exception) {
                FilterResponses.json(response, HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
