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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SaleReturnControllerTest extends AuthenticatedControllerTestSupport {

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
    void createsPartialReturnAndRestoresStock() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleResponse = createSale(customerId, productId, "40.000");
        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleItemId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.items[0].id");

        mockMvc.perform(post("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Damaged bags",
                      "notes":"Customer brought back unused stock",
                      "items":[{"saleItemId":"%s","quantity":10.000}]
                    }
                    """.formatted(saleItemId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/sales/returns/")))
            .andExpect(jsonPath("$.returnNumber").value(containsString("RET/")))
            .andExpect(jsonPath("$.totalAmount").value(3600.0))
            .andExpect(jsonPath("$.items.length()").value(1));

        mockMvc.perform(get("/sales/{id}", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.returnedAmount").value(3600.0))
            .andExpect(jsonPath("$.netAmount").value(10800.0))
            .andExpect(jsonPath("$.hasReturns").value(true))
            .andExpect(jsonPath("$.items[0].returnedQuantity").value(10.0))
            .andExpect(jsonPath("$.items[0].returnableQuantity").value(30.0));

        mockMvc.perform(get("/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].currentStock").value(90.0));

        mockMvc.perform(get("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].reason").value("Damaged bags"));
    }

    @Test
    void rejectsReturnQuantityAboveReturnable() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleResponse = createSale(customerId, productId, "20.000");
        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleItemId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.items[0].id");

        mockMvc.perform(post("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Too many",
                      "items":[{"saleItemId":"%s","quantity":25.000}]
                    }
                    """.formatted(saleItemId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(containsString("Return quantity exceeds returnable quantity")));
    }

    @Test
    void blocksCancelWhenSaleHasReturns() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleResponse = createSale(customerId, productId, "20.000");
        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleItemId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.items[0].id");

        mockMvc.perform(post("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Partial unused",
                      "items":[{"saleItemId":"%s","quantity":5.000}]
                    }
                    """.formatted(saleItemId)))
            .andExpect(status().isCreated());

        mockMvc.perform(patch("/sales/{id}/cancel", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Trying to cancel after return"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Sales with returns cannot be cancelled"));
    }

    @Test
    void creditsReturnUsingTaxInclusiveLineTotal() throws Exception {
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
                      "gstInclusivePricing":false,
                      "gstin":"29ABCDE1234F1Z5",
                      "stateCode":"29",
                      "stateName":"Karnataka"
                    }
                    """))
            .andExpect(status().isOk());

        String customerId = createCustomer();
        String productId = createProductWithGst();
        String saleResponse = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":0,
                      "interstate":false,
                      "items":[{"productId":"%s","quantity":10.000,"sellingPrice":100.00,"gstRate":18.00}]
                    }
                    """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalAmount").value(1180.0))
            .andReturn().getResponse().getContentAsString();

        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleItemId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.items[0].id");

        mockMvc.perform(post("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Full return",
                      "items":[{"saleItemId":"%s","quantity":10.000}]
                    }
                    """.formatted(saleItemId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.totalAmount").value(1180.0));

        mockMvc.perform(get("/sales/{id}", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.returnedAmount").value(1180.0))
            .andExpect(jsonPath("$.netAmount").value(0.0));
    }

    private String createSale(String customerId, String productId, String quantity) throws Exception {
        return mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":0,
                      "notes":"Counter sale",
                      "items":[{"productId":"%s","quantity":%s,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, productId, quantity)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
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
                      "name":"Ultra Cement","sku":"CEM-001","category":"Cement","unit":"Bags",
                      "costPrice":320.00,"sellingPrice":360.00,"openingStock":120.000,"lowStockThreshold":40.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createProductWithGst() throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"GST Cement","sku":"CEM-GST","category":"Cement","unit":"Bags",
                      "costPrice":80.00,"sellingPrice":100.00,"openingStock":120.000,"lowStockThreshold":40.000,
                      "hsnCode":"252329",
                      "gstRate":18.00
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
