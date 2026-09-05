package com.inventory.staff.controller;

import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.mail.LoggingMailService;
import com.inventory.business.entity.Business;
import com.inventory.support.AuthenticatedControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StaffControllerTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoggingMailService loggingMailService;

    private UUID businessId;
    private String ownerAuth;

    @BeforeEach
    void setUp() {
        loggingMailService.clear();
        UserAccount owner = createUserAccount();
        ownerAuth = authorizationHeader(owner);
        Business business = createBusinessFor(owner);
        businessId = business.getId();
    }

    @Test
    void ownerCanInviteAndStaffCanAccept() throws Exception {
        mockMvc.perform(post("/staff/invites")
                .header("Authorization", ownerAuth)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"clerk@example.com","role":"CLERK"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("clerk@example.com"))
            .andExpect(jsonPath("$.role").value("CLERK"));

        String token = extractInviteToken();

        mockMvc.perform(get("/auth/invite").param("token", token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("clerk@example.com"));

        MvcResult accepted = mockMvc.perform(post("/auth/accept-invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"token":"%s","fullName":"Counter Clerk","password":"password123"}
                    """.formatted(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.role").value("CLERK"))
            .andExpect(jsonPath("$.user.businessId").value(businessId.toString()))
            .andReturn();

        String clerkToken = "Bearer " + com.jayway.jsonpath.JsonPath.read(
            accepted.getResponse().getContentAsString(),
            "$.accessToken"
        );

        mockMvc.perform(post("/products")
                .header("Authorization", clerkToken)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": "Blocked",
                      "sku": "BLK-1",
                      "category": "A",
                      "unit": "Pcs",
                      "costPrice": 1,
                      "sellingPrice": 2,
                      "openingStock": 1,
                      "lowStockThreshold": 0
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    private String extractInviteToken() {
        String body = loggingMailService.findLatestTo("clerk@example.com")
            .orElseThrow()
            .body();
        Matcher matcher = Pattern.compile("token=([^\\s]+)").matcher(body);
        if (!matcher.find()) {
            throw new IllegalStateException("Invite mail did not contain a token");
        }
        return matcher.group(1);
    }
}
