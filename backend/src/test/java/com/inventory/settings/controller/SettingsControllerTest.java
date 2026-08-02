package com.inventory.settings.controller;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SettingsControllerTest extends AuthenticatedControllerTestSupport {

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
    void returnsDefaultSettings() throws Exception {
        mockMvc.perform(get("/settings")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.businessId").value(businessId.toString()))
            .andExpect(jsonPath("$.currencyCode").value("INR"))
            .andExpect(jsonPath("$.dateFormat").value("dd/MM/yyyy"))
            .andExpect(jsonPath("$.allowNegativeStock").value(false))
            .andExpect(jsonPath("$.defaultLowStockThreshold").value(0));
    }

    @Test
    void updatesSettingsAndAllowsNegativeStockSales() throws Exception {
        mockMvc.perform(put("/settings")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currencyCode": "USD",
                      "dateFormat": "yyyy-MM-dd",
                      "allowNegativeStock": true,
                      "defaultLowStockThreshold": 25.000
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currencyCode").value("USD"))
            .andExpect(jsonPath("$.dateFormat").value("yyyy-MM-dd"))
            .andExpect(jsonPath("$.allowNegativeStock").value(true))
            .andExpect(jsonPath("$.defaultLowStockThreshold").value(25.0));

        String customerId = createCustomer();
        String productId = createProduct();

        mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":0,
                      "notes":"Oversell allowed",
                      "items":[{"productId":"%s","quantity":200.000,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].quantity").value(200.0));
    }

    @Test
    void rejectsUnsupportedDateFormat() throws Exception {
        mockMvc.perform(put("/settings")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currencyCode": "INR",
                      "dateFormat": "invalid",
                      "allowNegativeStock": false,
                      "defaultLowStockThreshold": 0
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(
                "Date format must be one of: dd/MM/yyyy, MM/dd/yyyy, yyyy-MM-dd, dd-MMM-yyyy"
            ));
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

    private String createProduct() throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Ultra Cement",
                      "sku": "CEM-001",
                      "category": "Cement",
                      "unit": "Bags",
                      "costPrice": 320.00,
                      "sellingPrice": 360.00,
                      "openingStock": 100.000,
                      "lowStockThreshold": 40.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
