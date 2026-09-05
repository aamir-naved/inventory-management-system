package com.inventory.business.controller;

import com.inventory.auth.entity.UserAccount;
import com.inventory.support.AuthenticatedControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BusinessControllerTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    private String authorizationHeader;

    @BeforeEach
    void setUp() {
        UserAccount userAccount = createUserAccount();
        authorizationHeader = authorizationHeader(userAccount);
    }

    @Test
    void quickStartCreatesWalkInAndStarterProducts() throws Exception {
        String created = mockMvc.perform(post("/businesses/quick-start")
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "shopName": "Ram Hardware",
                      "mobileNumber": "9876500200"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Ram Hardware"))
            .andExpect(jsonPath("$.gstEnabled").value(false))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String businessId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

        mockMvc.perform(get("/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].name", org.hamcrest.Matchers.hasItem("Walk-in")));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(5))
            .andExpect(jsonPath("$.items[*].barcode", org.hamcrest.Matchers.hasItem("CEM-001")));
    }

    @Test
    void createsBusiness() throws Exception {
        mockMvc.perform(post("/businesses")
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "North Star Traders",
                      "businessType": "Hardware Store",
                      "addressLine": "42 Market Road",
                      "mobileNumber": "+91 9876543210",
                      "currencyCode": "INR",
                      "timeZone": "Asia/Kolkata"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/businesses/")))
            .andExpect(jsonPath("$.name").value("North Star Traders"))
            .andExpect(jsonPath("$.currencyCode").value("INR"));
    }

    @Test
    void updatesBusiness() throws Exception {
        String response = mockMvc.perform(post("/businesses")
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "North Star Traders",
                      "businessType": "Hardware Store",
                      "addressLine": "42 Market Road",
                      "mobileNumber": "+91 9876543210",
                      "currencyCode": "INR",
                      "timeZone": "Asia/Kolkata"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(patch("/businesses/{id}", id)
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "North Star Wholesale",
                      "businessType": "Building Materials",
                      "addressLine": "52 New Market Road",
                      "mobileNumber": "+91 9876500000",
                      "currencyCode": "INR",
                      "timeZone": "Asia/Kolkata"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("North Star Wholesale"))
            .andExpect(jsonPath("$.businessType").value("Building Materials"));
    }

    @Test
    void returnsValidationErrors() throws Exception {
        mockMvc.perform(post("/businesses")
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "",
                      "businessType": "",
                      "mobileNumber": "12",
                      "currencyCode": "rupee",
                      "timeZone": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.name").exists())
            .andExpect(jsonPath("$.fieldErrors.currencyCode").exists());
    }

    @Test
    void fetchesBusinessById() throws Exception {
        String response = mockMvc.perform(post("/businesses")
                .header("Authorization", authorizationHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "North Star Traders",
                      "businessType": "Hardware Store",
                      "addressLine": "42 Market Road",
                      "mobileNumber": "+91 9876543210",
                      "currencyCode": "INR",
                      "timeZone": "Asia/Kolkata"
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get("/businesses/{id}", id)
                .header("Authorization", authorizationHeader))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.name").value("North Star Traders"));
    }
}
