package com.inventory.inventory.controller;

import com.inventory.auth.entity.UserAccount;
import com.inventory.business.entity.Business;
import com.inventory.support.AuthenticatedControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class InventoryControllerTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private UUID businessId;
    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        UserAccount userAccount = createUserAccount();
        authorizationHeader = authorizationHeader(userAccount);
        Business business = createBusinessFor(userAccount);
        businessId = business.getId();
    }

    @Test
    void returnsInventorySummaryAndLowStockFilter() throws Exception {
        createProduct("Ultra Cement", "CEM-001", "Cement", 120.000, 40.000);
        createProduct("Red Bricks", "BRK-001", "Bricks", 8.000, 10.000);

        mockMvc.perform(get("/inventory/summary")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalProducts").value(2))
            .andExpect(jsonPath("$.lowStockProducts").value(1));

        mockMvc.perform(get("/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("lowStockOnly", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].productName").value("Red Bricks"))
            .andExpect(jsonPath("$[0].lowStock").value(true));
    }

    @Test
    void adjustsStockAndCreatesHistory() throws Exception {
        String productResponse = createProduct("Ultra Cement", "CEM-001", "Cement", 120.000, 40.000);
        String productId = com.jayway.jsonpath.JsonPath.read(productResponse, "$.id");

        mockMvc.perform(post("/inventory/adjustments")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": "%s",
                      "adjustmentQuantity": -20.000,
                      "reason": "Damaged bags found during physical count"
                    }
                    """.formatted(productId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.movementType").value("MANUAL_ADJUSTMENT"))
            .andExpect(jsonPath("$.quantityAfter").value(100.0));

        mockMvc.perform(get("/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].currentStock").value(100.0));

        mockMvc.perform(get("/inventory/movements")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("productId", productId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].movementType").value("MANUAL_ADJUSTMENT"));
    }

    @Test
    void blocksNegativeStockAdjustments() throws Exception {
        String productResponse = createProduct("Ultra Cement", "CEM-001", "Cement", 10.000, 5.000);
        String productId = com.jayway.jsonpath.JsonPath.read(productResponse, "$.id");

        mockMvc.perform(post("/inventory/adjustments")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "productId": "%s",
                      "adjustmentQuantity": -20.000,
                      "reason": "Manual correction"
                    }
                    """.formatted(productId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Stock cannot go below zero"));
    }

    private String createProduct(
        String name,
        String sku,
        String category,
        double openingStock,
        double lowStockThreshold
    ) throws Exception {
        return mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "sku": "%s",
                      "category": "%s",
                      "unit": "Bags",
                      "costPrice": 320.00,
                      "sellingPrice": 360.00,
                      "openingStock": %.3f,
                      "lowStockThreshold": %.3f
                    }
                    """.formatted(name, sku, category, openingStock, lowStockThreshold)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }
}
