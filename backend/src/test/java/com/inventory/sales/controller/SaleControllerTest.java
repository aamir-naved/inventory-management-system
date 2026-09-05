package com.inventory.sales.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SaleControllerTest extends AuthenticatedControllerTestSupport {
    @Autowired MockMvc mockMvc;
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
    void createsSaleAndDecreasesStock() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":0,
              "notes":"Counter sale",
              "items":[{"productId":"%s","quantity":20.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/sales/")))
            .andExpect(jsonPath("$.customerName").value("Apex Builders"))
            .andExpect(jsonPath("$.totalAmount").value(7200.0))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
            .andExpect(jsonPath("$.amountPaid").value(0.0))
            .andExpect(jsonPath("$.outstandingAmount").value(7200.0));

        mockMvc.perform(get("/inventory").header("Authorization", authorizationHeader).header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].currentStock").value(100.0));
    }

    @Test
    void blocksSaleWhenStockInsufficient() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":0,
              "notes":"Bulk order",
              "items":[{"productId":"%s","quantity":200.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Stock cannot go below zero for Ultra Cement"));
    }

    @Test
    void cancelsSaleAndRestoresStock() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        String response = mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":1000.00,
              "notes":"Counter sale",
              "items":[{"productId":"%s","quantity":20.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String saleId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/sales/{id}/cancel", saleId).header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {"reason":"Duplicate invoice"}
            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cancelled").value(true));

        mockMvc.perform(get("/inventory").header("Authorization", authorizationHeader).header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].currentStock").value(120.0));
    }

    @Test
    void paginatesSaleList() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        for (int day = 1; day <= 3; day++) {
            mockMvc.perform(post("/sales")
                    .header("Authorization", authorizationHeader)
                    .header("X-Business-Id", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "customerId":"%s",
                          "saleDate":"2026-08-0%d",
                          "amountPaid":0,
                          "notes":"Sale %d",
                          "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
                        }
                        """.formatted(customerId, day, day, productId)))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("size", "2")
                .param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    private String createCustomer() throws Exception {
        String response = mockMvc.perform(post("/customers").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Apex Builders","contactPerson":"Neha Shah","mobileNumber":"+91 9998887776","addressLine":"Ring Road"}
            """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createProduct() throws Exception {
        String response = mockMvc.perform(post("/products").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
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
