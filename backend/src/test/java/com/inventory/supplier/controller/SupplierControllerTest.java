package com.inventory.supplier.controller;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupplierControllerTest extends AuthenticatedControllerTestSupport {

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
    void createsAndListsSuppliers() throws Exception {
        mockMvc.perform(post("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Shakti Cements Ltd",
                      "contactPerson": "Rahul Mehta",
                      "mobileNumber": "+91 9876543210",
                      "addressLine": "Industrial Road"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/suppliers/")))
            .andExpect(jsonPath("$.name").value("Shakti Cements Ltd"));

        mockMvc.perform(get("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Shakti Cements Ltd"));
    }

    @Test
    void archivesSupplier() throws Exception {
        String response = mockMvc.perform(post("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Shakti Cements Ltd",
                      "contactPerson": "Rahul Mehta",
                      "mobileNumber": "+91 9876543210",
                      "addressLine": "Industrial Road"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/suppliers/{id}/archive", id)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived").value(true));
    }

    @Test
    void returnsSummaryAndPurchaseHistoryWithOutstanding() throws Exception {
        LocalDate today = LocalDate.now();
        String productId = createProduct();
        String supplierId = createSupplier("Shakti Cements Ltd");

        // 10 * 315 = 3150, paid 1000 → outstanding 2150
        createPurchase(supplierId, productId, today, "1000.00", 10);
        // Fully paid bill still counted in summary
        createPurchase(supplierId, productId, today.minusDays(1), "3150.00", 10);
        // Cancelled purchase excluded from summary, still present in history
        String cancelledPurchaseId = createPurchase(supplierId, productId, today, "0", 2);
        mockMvc.perform(patch("/purchases/{id}/cancel", cancelledPurchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Entered by mistake"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/suppliers/{id}/summary", supplierId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Shakti Cements Ltd"))
            .andExpect(jsonPath("$.billCount").value(2))
            .andExpect(jsonPath("$.billedAmount").value(6300.0))
            .andExpect(jsonPath("$.amountPaid").value(4150.0))
            .andExpect(jsonPath("$.outstandingAmount").value(2150.0));

        mockMvc.perform(get("/suppliers/{id}/purchases", supplierId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.cancelled == true)]").exists())
            .andExpect(jsonPath("$[?(@.outstandingAmount == 2150.0)]").exists());
    }

    private String createSupplier(String name) throws Exception {
        String response = mockMvc.perform(post("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "contactPerson": "Rahul Mehta",
                      "mobileNumber": "+91 9876543210",
                      "addressLine": "Industrial Road"
                    }
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createPurchase(
        String supplierId,
        String productId,
        LocalDate purchaseDate,
        String amountPaid,
        int quantity
    ) throws Exception {
        String response = mockMvc.perform(post("/purchases")
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
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createProduct() throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Ultra Cement",
                      "sku":"CEM-SUP-001",
                      "category":"Cement",
                      "unit":"Bags",
                      "costPrice":320.00,
                      "sellingPrice":360.00,
                      "openingStock":100.000,
                      "lowStockThreshold":10.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
