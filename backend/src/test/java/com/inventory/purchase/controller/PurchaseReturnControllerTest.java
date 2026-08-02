package com.inventory.purchase.controller;

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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PurchaseReturnControllerTest extends AuthenticatedControllerTestSupport {

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
    void createsPartialReturnAndReducesStock() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();
        String purchaseResponse = createPurchase(supplierId, productId, "40.000");
        String purchaseId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.id");
        String purchaseItemId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.items[0].id");

        mockMvc.perform(post("/purchases/{purchaseId}/returns", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Damaged bags",
                      "notes":"Sent unused stock back",
                      "items":[{"purchaseItemId":"%s","quantity":10.000}]
                    }
                    """.formatted(purchaseItemId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/purchases/returns/")))
            .andExpect(jsonPath("$.returnNumber").value(containsString("PRT-")))
            .andExpect(jsonPath("$.totalAmount").value(3150.0))
            .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(get("/purchases/{id}", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.returnedAmount").value(3150.0))
            .andExpect(jsonPath("$.netAmount").value(9450.0))
            .andExpect(jsonPath("$.hasReturns").value(true))
            .andExpect(jsonPath("$.items[0].returnedQuantity").value(10.0))
            .andExpect(jsonPath("$.items[0].returnableQuantity").value(30.0));

        mockMvc.perform(get("/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].currentStock").value(150.0));

        mockMvc.perform(get("/purchases/{purchaseId}/returns", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].reason").value("Damaged bags"));
    }

    @Test
    void rejectsReturnQuantityAboveReturnable() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();
        String purchaseResponse = createPurchase(supplierId, productId, "20.000");
        String purchaseId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.id");
        String purchaseItemId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.items[0].id");

        mockMvc.perform(post("/purchases/{purchaseId}/returns", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Too many",
                      "items":[{"purchaseItemId":"%s","quantity":25.000}]
                    }
                    """.formatted(purchaseItemId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Return quantity exceeds returnable quantity")));
    }

    @Test
    void blocksCancelWhenPurchaseHasReturns() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();
        String purchaseResponse = createPurchase(supplierId, productId, "20.000");
        String purchaseId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.id");
        String purchaseItemId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.items[0].id");

        mockMvc.perform(post("/purchases/{purchaseId}/returns", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Partial unused",
                      "items":[{"purchaseItemId":"%s","quantity":5.000}]
                    }
                    """.formatted(purchaseItemId)))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/purchases/{id}/cancel", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Trying to cancel after return"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Purchases with returns cannot be cancelled"));
    }

    private String createPurchase(String supplierId, String productId, String quantity) throws Exception {
        return mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"2026-08-01",
                      "amountPaid":0,
                      "notes":"Restocking cement",
                      "items":[{"productId":"%s","quantity":%s,"purchasePrice":315.00}]
                    }
                    """.formatted(supplierId, productId, quantity)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
    }

    private String createSupplier() throws Exception {
        String response = mockMvc.perform(post("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Shakti Cements Ltd","contactPerson":"Ravi","mobileNumber":"+91 9876501234","addressLine":"Industrial Area"}
                    """))
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
                      "name":"Ultra Cement","sku":"CEM-001","category":"Cement","unit":"Bags",
                      "costPrice":320.00,"sellingPrice":360.00,"openingStock":120.000,"lowStockThreshold":40.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
