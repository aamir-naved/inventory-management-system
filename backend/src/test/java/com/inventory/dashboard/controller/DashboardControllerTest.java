package com.inventory.dashboard.controller;

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

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerTest extends AuthenticatedControllerTestSupport {

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
    void returnsLiveDashboardMetrics() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        createProduct("Low Stock Screws", "SCR-001", 10.000, 40.000);
        String cementId = createProduct("Ultra Cement", "CEM-001", 100.000, 20.000);
        String customerId = createCustomer();
        String supplierId = createSupplier();

        // Today sale: 5 * 360 = 1800, paid 500 → outstanding 1300
        createSale(customerId, cementId, today, "500.00", 5);
        // Past sale: 5 * 360 = 1800, fully paid → contributes to lifetime revenue only
        createSale(customerId, cementId, yesterday, "1800.00", 5);
        // Cancelled today sale must be excluded from all aggregates
        String cancelledSaleId = createSale(customerId, cementId, today, "0", 2);
        mockMvc.perform(patch("/sales/{id}/cancel", cancelledSaleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Entered by mistake"}
                    """))
            .andExpect(status().isOk());

        // Today purchase: 10 * 315 = 3150, unpaid → supplier outstanding 3150
        createPurchase(supplierId, cementId, today, "0", 10);

        // Stock after flows: cement 100 -5 -5 -2 +2(cancel restore) +10 = 100
        // Inventory value: screws 10*320 + cement 100*320 = 35200
        mockMvc.perform(get("/dashboard/metrics")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.asOfDate").value(today.toString()))
            .andExpect(jsonPath("$.todaysSalesAmount").value(1800.0))
            .andExpect(jsonPath("$.todaysPurchasesAmount").value(3150.0))
            .andExpect(jsonPath("$.totalRevenue").value(3600.0))
            .andExpect(jsonPath("$.totalProducts").value(2))
            .andExpect(jsonPath("$.inventoryValue").value(35200.0))
            .andExpect(jsonPath("$.lowStockProducts").value(1))
            .andExpect(jsonPath("$.outstandingCustomers").value(1300.0))
            .andExpect(jsonPath("$.outstandingSuppliers").value(3150.0));
    }

    @Test
    void returnsZeroMetricsForEmptyBusiness() throws Exception {
        mockMvc.perform(get("/dashboard/metrics")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todaysSalesAmount").value(0))
            .andExpect(jsonPath("$.todaysPurchasesAmount").value(0))
            .andExpect(jsonPath("$.totalRevenue").value(0))
            .andExpect(jsonPath("$.totalProducts").value(0))
            .andExpect(jsonPath("$.inventoryValue").value(0))
            .andExpect(jsonPath("$.lowStockProducts").value(0))
            .andExpect(jsonPath("$.outstandingCustomers").value(0))
            .andExpect(jsonPath("$.outstandingSuppliers").value(0));
    }

    private String createSale(
        String customerId,
        String productId,
        LocalDate saleDate,
        String amountPaid,
        int quantity
    ) throws Exception {
        String response = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"%s",
                      "amountPaid":%s,
                      "notes":"Counter sale",
                      "items":[{"productId":"%s","quantity":%d.000,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, saleDate, amountPaid, productId, quantity)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private void createPurchase(
        String supplierId,
        String productId,
        LocalDate purchaseDate,
        String amountPaid,
        int quantity
    ) throws Exception {
        mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"%s",
                      "amountPaid":%s,
                      "notes":"Restocking",
                      "items":[{"productId":"%s","quantity":%d.000,"purchasePrice":315.00}]
                    }
                    """.formatted(supplierId, purchaseDate, amountPaid, productId, quantity)))
            .andExpect(status().isCreated());
    }

    private String createCustomer() throws Exception {
        String response = mockMvc.perform(post("/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Apex Builders","contactPerson":"Neha Shah","mobileNumber":"+91 9998887776","addressLine":"Ring Road"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createSupplier() throws Exception {
        String response = mockMvc.perform(post("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Shakti Cements Ltd",
                      "contactPerson":"Rahul Mehta",
                      "mobileNumber":"+91 9876543210",
                      "addressLine":"Industrial Road"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createProduct(String name, String sku, double openingStock, double lowStockThreshold)
        throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"%s","sku":"%s","category":"Hardware","unit":"Bags",
                      "costPrice":320.00,"sellingPrice":360.00,"openingStock":%.3f,"lowStockThreshold":%.3f
                    }
                    """.formatted(name, sku, openingStock, lowStockThreshold)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
