package com.inventory.document.controller;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DocumentControllerTest extends AuthenticatedControllerTestSupport {

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
    void downloadsSaleInvoicePdf() throws Exception {
        String customerId = createCustomer();
        String productId = createProduct();
        String saleResponse = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
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
        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleNumber = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.saleNumber");

        byte[] body = mockMvc.perform(get("/sales/{id}/invoice.pdf", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
            .andExpect(header().string(
                "Content-Disposition",
                containsString("invoice-" + saleNumber.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf")
            ))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertThat(body.length).isGreaterThan(100);
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void returnsNotFoundForMissingSaleInvoice() throws Exception {
        mockMvc.perform(get("/sales/{id}/invoice.pdf", UUID.randomUUID())
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isNotFound());
    }

    @Test
    void downloadsPurchaseBillPdf() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();
        String purchaseResponse = mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"2026-08-01",
                      "amountPaid":500.00,
                      "notes":"Restock",
                      "items":[{"productId":"%s","quantity":10.000,"purchasePrice":315.00}]
                    }
                    """.formatted(supplierId, productId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String purchaseId = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.id");
        String purchaseNumber = com.jayway.jsonpath.JsonPath.read(purchaseResponse, "$.purchaseNumber");

        byte[] body = mockMvc.perform(get("/purchases/{id}/bill.pdf", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
            .andExpect(header().string(
                "Content-Disposition",
                containsString("purchase-bill-" + purchaseNumber.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf")
            ))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertThat(body.length).isGreaterThan(100);
        assertThat(new String(body, 0, 4)).isEqualTo("%PDF");
    }

    @Test
    void returnsNotFoundForMissingPurchaseBill() throws Exception {
        mockMvc.perform(get("/purchases/{id}/bill.pdf", UUID.randomUUID())
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isNotFound());
    }

    private String createCustomer() throws Exception {
        String response = mockMvc.perform(post("/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Apex Builders","contactPerson":"Neha Shah","mobileNumber":"+91 9998887234","addressLine":"Ring Road"}
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
                    {"name":"Shakti Cements Ltd","contactPerson":"Ravi","mobileNumber":"+91 9888777666","addressLine":"Industrial Area"}
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
                      "name":"Ultra Cement","sku":"CEM-PDF","category":"Cement","unit":"Bags",
                      "costPrice":320.00,"sellingPrice":360.00,"openingStock":120.000,"lowStockThreshold":40.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
