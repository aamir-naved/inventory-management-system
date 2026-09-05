package com.inventory.security;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.inventory.auth.security.JwtAuthenticationFilter;
import com.inventory.common.tenant.TenantContextFilter;
import com.inventory.config.AuthProperties;
import com.inventory.config.CorsProperties;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        JwtAuthenticationFilter jwtAuthenticationFilter,
        TenantContextFilter tenantContextFilter,
        RoleAuthorizationFilter roleAuthorizationFilter
    ) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/actuator/health",
                    "/actuator/info",
                    "/auth/login",
                    "/auth/register",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/auth/verify-email",
                    "/auth/refresh",
                    "/auth/invite",
                    "/auth/accept-invite",
                    "/auth/otp/request",
                    "/auth/otp/verify",
                    "/auth/public-config"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(tenantContextFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(roleAuthorizationFilter, TenantContextFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
        CorsProperties corsProperties,
        AuthProperties authProperties
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = new ArrayList<>(corsProperties.getAllowedOrigins());
        String publicOrigin = originFromPublicAppUrl(authProperties.getPublicAppUrl());
        if (publicOrigin != null && !origins.contains(publicOrigin)) {
            origins.add(publicOrigin);
        }
        configuration.setAllowedOrigins(origins);
        if (!corsProperties.getAllowedOriginPatterns().isEmpty()) {
            configuration.setAllowedOriginPatterns(corsProperties.getAllowedOriginPatterns());
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static String originFromPublicAppUrl(String publicAppUrl) {
        if (publicAppUrl == null || publicAppUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(publicAppUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            if (uri.getPort() == -1) {
                return uri.getScheme() + "://" + uri.getHost();
            }
            return uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort();
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
