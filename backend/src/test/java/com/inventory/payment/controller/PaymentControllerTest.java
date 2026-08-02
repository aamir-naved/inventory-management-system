package com.inventory.payment.controller;

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
class PaymentControllerTest extends AuthenticatedControllerTestSupport {

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
    void recordsFullSalePayment() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleId = createSale(customerId, productId, "0");

        mockMvc.perform(post("/sales/{saleId}/payments", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"paymentDate":"2026-08-02","amount":7200.00,"notes":"Cash settlement"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/payments/")))
            .andExpect(jsonPath("$.amount").value(7200.0))
            .andExpect(jsonPath("$.documentType").value("SALE"));

        mockMvc.perform(get("/sales/{id}", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("PAID"))
            .andExpect(jsonPath("$.amountPaid").value(7200.0))
            .andExpect(jsonPath("$.outstandingAmount").value(0.0));
    }

    @Test
    void recordsPartialPurchasePayment() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();
        String purchaseId = createPurchase(supplierId, productId, "0");

        mockMvc.perform(post("/purchases/{purchaseId}/payments", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"paymentDate":"2026-08-02","amount":2000.00,"notes":"Advance"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.amount").value(2000.0));

        mockMvc.perform(get("/purchases/{id}", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentStatus").value("PARTIAL"))
            .andExpect(jsonPath("$.amountPaid").value(2000.0))
            .andExpect(jsonPath("$.outstandingAmount").value(4300.0));

        mockMvc.perform(get("/purchases/{purchaseId}/payments", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void rejectsPaymentAboveOutstanding() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleId = createSale(customerId, productId, "1000");

        mockMvc.perform(post("/sales/{saleId}/payments", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"paymentDate":"2026-08-02","amount":7000.00}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Payment exceeds outstanding amount"));
    }

    @Test
    void rejectsPaymentOnCancelledSale() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleId = createSale(customerId, productId, "0");

        mockMvc.perform(patch("/sales/{id}/cancel", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Duplicate invoice"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/sales/{saleId}/payments", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"paymentDate":"2026-08-02","amount":100.00}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Cancelled sales cannot accept payments"));
    }

    @Test
    void saleReturnCanFlipPartialToPaid() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        // total 14400 (40 * 360), pay 10800 -> PARTIAL, outstanding 3600
        String saleResponse = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":10800.00,
                      "notes":"Counter sale",
                      "items":[{"productId":"%s","quantity":40.000,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentStatus").value("PARTIAL"))
            .andExpect(jsonPath("$.outstandingAmount").value(3600.0))
            .andReturn().getResponse().getContentAsString();

        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleItemId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.items[0].id");

        // return 10 bags = 3600; net becomes 10800; paid 10800 -> PAID
        mockMvc.perform(post("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Unused stock",
                      "items":[{"saleItemId":"%s","quantity":10.000}]
                    }
                    """.formatted(saleItemId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/sales/{id}", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.netAmount").value(10800.0))
            .andExpect(jsonPath("$.amountPaid").value(10800.0))
            .andExpect(jsonPath("$.outstandingAmount").value(0.0))
            .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void purchaseReturnCanFlipPartialToPaid() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();
        // total 12600 (40 * 315), pay 9450 -> PARTIAL, outstanding 3150
        String purchaseResponse = mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"2026-08-01",
                      "amountPaid":9450.00,
                      "notes":"Restocking",
                      "items":[{"productId":"%s","quantity":40.000,"purchasePrice":315.00}]
                    }
                    """.formatted(supplierId, productId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.paymentStatus").value("PARTIAL"))
            .andExpect(jsonPath("$.outstandingAmount").value(3150.0))
            .andReturn().getResponse().getContentAsString();

        String purchaseId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.id");
        String purchaseItemId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.items[0].id");

        // return 10 bags = 3150; net becomes 9450; paid 9450 -> PAID
        mockMvc.perform(post("/purchases/{purchaseId}/returns", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Unused stock",
                      "items":[{"purchaseItemId":"%s","quantity":10.000}]
                    }
                    """.formatted(purchaseItemId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/purchases/{id}", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.netAmount").value(9450.0))
            .andExpect(jsonPath("$.amountPaid").value(9450.0))
            .andExpect(jsonPath("$.outstandingAmount").value(0.0))
            .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    private String createSale(String customerId, String productId, String amountPaid) throws Exception {
        String response = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":%s,
                      "notes":"Counter sale",
                      "items":[{"productId":"%s","quantity":20.000,"sellingPrice":360.00}]
                    }
                    """.formatted(customerId, amountPaid, productId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createPurchase(String supplierId, String productId, String amountPaid) throws Exception {
        String response = mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"2026-08-01",
                      "amountPaid":%s,
                      "notes":"Restocking",
                      "items":[{"productId":"%s","quantity":20.000,"purchasePrice":315.00}]
                    }
                    """.formatted(supplierId, amountPaid, productId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
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
