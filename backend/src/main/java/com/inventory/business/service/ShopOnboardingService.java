package com.inventory.business.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.auth.entity.MembershipRole;
import com.inventory.common.tenant.TenantContext;
import com.inventory.customer.dto.CustomerRequest;
import com.inventory.customer.service.CustomerService;
import com.inventory.product.dto.ProductRequest;
import com.inventory.product.service.ProductService;

@Service
public class ShopOnboardingService {

    private final CustomerService customerService;
    private final ProductService productService;

    public ShopOnboardingService(CustomerService customerService, ProductService productService) {
        this.customerService = customerService;
        this.productService = productService;
    }

    @Transactional
    public void provisionNewShop(UUID businessId, UUID userId) {
        TenantContext.set(businessId, MembershipRole.OWNER, userId);
        try {
            customerService.create(new CustomerRequest("Walk-in", null, null, null, null));
            for (ProductRequest product : starterCatalog()) {
                productService.create(product);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private static List<ProductRequest> starterCatalog() {
        return List.of(
            item("Cement", "CEM-001", "Cement", "Bags", "320", "380", "2523", "28"),
            item("TMT bar", "TMT-001", "Steel", "Kg", "55", "65", "7214", "18"),
            item("Emulsion paint 1L", "PNT-001", "Paint", "Pieces", "180", "220", "3208", "18"),
            item("Switch 6A", "ELC-001", "Electrical", "Pieces", "40", "60", "8536", "18"),
            item("PVC pipe", "SAN-001", "Sanitary", "Meters", "90", "120", "3917", "18")
        );
    }

    private static ProductRequest item(
        String name,
        String sku,
        String category,
        String unit,
        String cost,
        String sell,
        String hsn,
        String gst
    ) {
        return new ProductRequest(
            name,
            sku,
            category,
            unit,
            new BigDecimal(cost),
            new BigDecimal(sell),
            new BigDecimal("20.000"),
            new BigDecimal("5.000"),
            sku,
            hsn,
            new BigDecimal(gst)
        );
    }
}
