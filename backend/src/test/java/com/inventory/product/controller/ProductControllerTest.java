package com.inventory.product.controller;

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
class ProductControllerTest extends AuthenticatedControllerTestSupport {

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
    void createsProduct() throws Exception {
        mockMvc.perform(post("/products")
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
            .andExpect(header().string("Location", containsString("/products/")))
            .andExpect(jsonPath("$.name").value("Ultra Cement"))
            .andExpect(jsonPath("$.currentStock").value(120.0))
            .andExpect(jsonPath("$.lowStockThreshold").value(40.0))
            .andExpect(jsonPath("$.businessId").value(businessId.toString()));
    }

    @Test
    void listsAndSearchesProductsWithinBusiness() throws Exception {
        createProduct("Ultra Cement", "CEM-001", "Cement");
        createProduct("Red Bricks", "BRK-001", "Bricks");

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("search", "cement"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("Ultra Cement"));
    }

    @Test
    void updatesProduct() throws Exception {
        String response = createProduct("Ultra Cement", "CEM-001", "Cement");
        String productId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/products/{id}", productId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Ultra Cement Premium",
                      "sku": "CEM-001",
                      "category": "Cement",
                      "unit": "Bags",
                      "costPrice": 330.00,
                      "sellingPrice": 370.00,
                      "openingStock": 140.000,
                      "lowStockThreshold": 50.000
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ultra Cement Premium"))
            .andExpect(jsonPath("$.openingStock").value(140.0))
            .andExpect(jsonPath("$.lowStockThreshold").value(50.0))
            .andExpect(jsonPath("$.currentStock").value(140.0));
    }

    @Test
    void archivesProduct() throws Exception {
        String response = createProduct("Ultra Cement", "CEM-001", "Cement");
        String productId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/products/{id}/archive", productId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived").value(true));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("includeArchived", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].archived").value(true));
    }

    @Test
    void requiresBusinessHeader() throws Exception {
        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("X-Business-Id header is required"));
    }

    private String createProduct(String name, String sku, String category) throws Exception {
        return mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "%s",
                      "sku": "%s",
                      "category": "%s",
                      "unit": "Bags",
                      "costPrice": 320.00,
                      "sellingPrice": 360.00,
                      "openingStock": 120.000,
                      "lowStockThreshold": 40.000
                    }
                    """.formatted(name, sku, category)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    }
}
