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
    void assignsConsecutiveSaleNumbersPerFinancialYear() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        String first = mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":0,
              "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.saleNumber").value(org.hamcrest.Matchers.matchesPattern("SAL/\\d{4}-\\d{2}/000001")))
            .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-02",
              "amountPaid":0,
              "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.saleNumber").value(org.hamcrest.Matchers.matchesPattern("SAL/\\d{4}-\\d{2}/000002")));

        String firstNumber = com.jayway.jsonpath.JsonPath.read(first, "$.saleNumber");
        org.junit.jupiter.api.Assertions.assertTrue(firstNumber.startsWith("SAL/"));
    }

    @Test
    void saleItemKeepsProductNameAfterProductRename() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        String saleResponse = mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":0,
              "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].productName").value("Ultra Cement"))
            .andReturn().getResponse().getContentAsString();
        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");

        mockMvc.perform(patch("/products/{id}", productId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Renamed Cement",
                      "sku":"CEM-001",
                      "category":"Cement",
                      "unit":"Bags",
                      "costPrice":320.00,
                      "sellingPrice":360.00,
                      "openingStock":120.000,
                      "lowStockThreshold":40.000
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed Cement"));

        mockMvc.perform(get("/sales/{id}", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].productName").value("Ultra Cement"))
            .andExpect(jsonPath("$.items[0].unit").value("Bags"));
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
    void blocksSaleWhenDuplicateLinesExceedStock() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        mockMvc.perform(post("/sales").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":0,
              "notes":"Duplicate lines",
              "items":[
                {"productId":"%s","quantity":70.000,"sellingPrice":360.00},
                {"productId":"%s","quantity":70.000,"sellingPrice":360.00}
              ]
            }
            """.formatted(customerId, productId, productId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Stock cannot go below zero for Ultra Cement"));

        mockMvc.perform(get("/inventory").header("Authorization", authorizationHeader).header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].currentStock").value(120.0));
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
            .andExpect(jsonPath("$.cancelled").value(true))
            .andExpect(jsonPath("$.amountPaid").value(0.0));

        mockMvc.perform(get("/sales/{saleId}/payments", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.paymentKind == 'REFUND')].amount").value(org.hamcrest.Matchers.hasItem(1000.0)))
            .andExpect(jsonPath("$[?(@.paymentKind == 'RECEIPT')].amount").value(org.hamcrest.Matchers.hasItem(1000.0)));

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

    @Test
    void replaysSaleCreateWhenIdempotencyKeyRepeats() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String body = """
            {
              "customerId":"%s",
              "saleDate":"2026-08-01",
              "amountPaid":100,
              "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
            }
            """.formatted(customerId, productId);

        String first = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .header("Idempotency-Key", "sale-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amountPaid").value(100.0))
            .andReturn().getResponse().getContentAsString();

        String second = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .header("Idempotency-Key", "sale-key-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String firstId = com.jayway.jsonpath.JsonPath.read(first, "$.id");
        String secondId = com.jayway.jsonpath.JsonPath.read(second, "$.id");
        String firstNumber = com.jayway.jsonpath.JsonPath.read(first, "$.saleNumber");
        String secondNumber = com.jayway.jsonpath.JsonPath.read(second, "$.saleNumber");
        org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
        org.junit.jupiter.api.Assertions.assertEquals(firstNumber, secondNumber);

        mockMvc.perform(get("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    void rejectsIdempotencyKeyReuseWithDifferentBody() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();

        mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .header("Idempotency-Key", "sale-key-conflict")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":0,
                      "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, productId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .header("Idempotency-Key", "sale-key-conflict")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":50,
                      "items":[{"productId":"%s","quantity":1.000,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, productId)))
            .andExpect(status().isConflict());
    }

    @Test
    void derivesInterstateFromCustomerStateIgnoringCheckbox() throws Exception {
        mockMvc.perform(put("/settings")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currencyCode":"INR",
                      "dateFormat":"dd/MM/yyyy",
                      "allowNegativeStock":false,
                      "defaultLowStockThreshold":0,
                      "gstEnabled":true,
                      "gstin":"29ABCDE1234F1Z5",
                      "stateCode":"29",
                      "stateName":"Karnataka",
                      "gstInclusivePricing":false
                    }
                    """))
            .andExpect(status().isOk());

        String customerId = mockMvc.perform(post("/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Out of State Buyer",
                      "contactPerson":"Neha Shah",
                      "mobileNumber":"+91 9998887776",
                      "addressLine":"Mumbai",
                      "stateCode":"27"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String customerUuid = com.jayway.jsonpath.JsonPath.read(customerId, "$.id");

        String productResponse = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"GST Cement","sku":"GST-CEM","category":"Cement","unit":"Bags",
                      "costPrice":100.00,"sellingPrice":100.00,"openingStock":50.000,"lowStockThreshold":5.000,
                      "hsnCode":"2523","gstRate":18.00
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String productId = com.jayway.jsonpath.JsonPath.read(productResponse, "$.id");

        mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":0,
                      "interstate":false,
                      "items":[{"productId":"%s","quantity":1.000,"sellingPrice":100.00,"gstRate":18.00}]
                    }
                    """.formatted(customerUuid, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.interstate").value(true))
            .andExpect(jsonPath("$.igstAmount").value(18.00))
            .andExpect(jsonPath("$.cgstAmount").value(0))
            .andExpect(jsonPath("$.sgstAmount").value(0));
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
