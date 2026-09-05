package com.inventory.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAuthorizationFilterTest {

    @Test
    void clerkCanSellAndLookUpProducts() {
        assertThat(RoleAuthorizationFilter.clerkAllowed("POST", "/sales")).isTrue();
        assertThat(RoleAuthorizationFilter.clerkAllowed("GET", "/products")).isTrue();
        assertThat(RoleAuthorizationFilter.clerkAllowed("GET", "/products/by-barcode/890123")).isTrue();
        assertThat(RoleAuthorizationFilter.clerkAllowed("GET", "/notifications")).isTrue();
    }

    @Test
    void clerkCannotManageCatalogOrCancel() {
        assertThat(RoleAuthorizationFilter.clerkAllowed("POST", "/products")).isFalse();
        assertThat(RoleAuthorizationFilter.clerkAllowed("PATCH", "/sales/1/cancel")).isFalse();
        assertThat(RoleAuthorizationFilter.clerkAllowed("POST", "/inventory/adjustments")).isFalse();
        assertThat(RoleAuthorizationFilter.clerkAllowed("GET", "/reports/sales")).isFalse();
        assertThat(RoleAuthorizationFilter.clerkAllowed("POST", "/staff/invites")).isFalse();
        assertThat(RoleAuthorizationFilter.clerkAllowed("PUT", "/settings")).isFalse();
    }
}
