package com.inventory.customer.controller;

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
class CustomerControllerTest extends AuthenticatedControllerTestSupport {
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
    void createsAndListsCustomers() throws Exception {
        mockMvc.perform(post("/customers").header("Authorization", authorizationHeader).header("X-Business-Id", businessId).contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Apex Builders","contactPerson":"Neha Shah","mobileNumber":"+91 9998887776","addressLine":"Ring Road"}
            """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/customers/")))
            .andExpect(jsonPath("$.name").value("Apex Builders"));

        mockMvc.perform(get("/customers").header("Authorization", authorizationHeader).header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void returnsSummaryAndSaleHistoryWithOutstanding() throws Exception {
        LocalDate today = LocalDate.now();
        String productId = createProduct();
        String customerId = createCustomer("Apex Builders");

        // 5 * 360 = 1800, paid 500 → outstanding 1300
        createSale(customerId, productId, today, "500.00", 5);
        // Fully paid invoice still counted in summary
        createSale(customerId, productId, today.minusDays(1), "1800.00", 5);
        // Cancelled sale excluded from summary, still present in history
        String cancelledSaleId = createSale(customerId, productId, today, "0", 2);
        mockMvc.perform(patch("/sales/{id}/cancel", cancelledSaleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Entered by mistake"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/customers/{id}/summary", customerId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Apex Builders"))
            .andExpect(jsonPath("$.invoiceCount").value(2))
            .andExpect(jsonPath("$.netBilled").value(3600.0))
            .andExpect(jsonPath("$.amountPaid").value(2300.0))
            .andExpect(jsonPath("$.outstandingAmount").value(1300.0));

        mockMvc.perform(get("/customers/{id}/sales", customerId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.cancelled == true)]").exists())
            .andExpect(jsonPath("$[?(@.outstandingAmount == 1300.0)]").exists());
    }

    private String createCustomer(String name) throws Exception {
        String response = mockMvc.perform(post("/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"%s",
                      "contactPerson":"Neha Shah",
                      "mobileNumber":"+91 9998887776",
                      "addressLine":"Ring Road"
                    }
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
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

    private String createProduct() throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Ultra Cement",
                      "sku":"CEM-001",
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
