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
class PurchaseControllerTest extends AuthenticatedControllerTestSupport {

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
    void createsPurchaseAndIncreasesStock() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();

        mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId": "%s",
                      "purchaseDate": "2026-08-01",
                      "amountPaid": 0,
                      "notes": "Restocking cement",
                      "items": [
                        {
                          "productId": "%s",
                          "quantity": 20.000,
                          "purchasePrice": 315.00
                        }
                      ]
                    }
                    """.formatted(supplierId, productId)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", containsString("/purchases/")))
            .andExpect(jsonPath("$.supplierName").value("Shakti Cements Ltd"))
            .andExpect(jsonPath("$.totalAmount").value(6300.0))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
            .andExpect(jsonPath("$.amountPaid").value(0.0))
            .andExpect(jsonPath("$.outstandingAmount").value(6300.0))
            .andExpect(jsonPath("$.cancelled").value(false));

        mockMvc.perform(get("/products/{id}", productId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.costPrice").value(319.29))
            .andExpect(jsonPath("$.currentStock").value(140.0));

        mockMvc.perform(get("/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].currentStock").value(140.0));
    }

    @Test
    void listsAndCancelsPurchase() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();

        String response = mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId": "%s",
                      "purchaseDate": "2026-08-01",
                      "amountPaid": 2000.00,
                      "notes": "Restocking cement",
                      "items": [
                        {
                          "productId": "%s",
                          "quantity": 20.000,
                          "purchasePrice": 315.00
                        }
                      ]
                    }
                    """.formatted(supplierId, productId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String purchaseId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("search", "shakti"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].paymentStatus").value("PARTIAL"))
            .andExpect(jsonPath("$.items[0].amountPaid").value(2000.0));

        mockMvc.perform(patch("/purchases/{id}/cancel", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reason": "Supplier entered duplicate bill"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.cancelled").value(true))
            .andExpect(jsonPath("$.cancellationReason").value("Supplier entered duplicate bill"))
            .andExpect(jsonPath("$.amountPaid").value(0.0));

        mockMvc.perform(get("/purchases/{purchaseId}/payments", purchaseId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.paymentKind == 'REFUND')].amount").value(org.hamcrest.Matchers.hasItem(2000.0)));

        mockMvc.perform(get("/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].currentStock").value(120.0));
    }

    @Test
    void paginatesPurchaseList() throws Exception {
        String supplierId = createSupplier();
        String productId = createProduct();

        for (int day = 1; day <= 3; day++) {
            mockMvc.perform(post("/purchases")
                    .header("Authorization", authorizationHeader)
                    .header("X-Business-Id", businessId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "supplierId": "%s",
                          "purchaseDate": "2026-08-0%d",
                          "amountPaid": 0,
                          "notes": "Restock %d",
                          "items": [
                            {
                              "productId": "%s",
                              "quantity": 1.000,
                              "purchasePrice": 315.00
                            }
                          ]
                        }
                        """.formatted(supplierId, day, day, productId)))
                .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("size", "2")
                .param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    private String createSupplier() throws Exception {
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
                      "openingStock": 120.000,
                      "lowStockThreshold": 40.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
