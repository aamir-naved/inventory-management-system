package com.inventory.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.inventory.auth.entity.UserAccount;
import com.inventory.support.AuthenticatedControllerTestSupport;

@SpringBootTest(properties = "app.platform.open-registration=false")
@AutoConfigureMockMvc
class ClosedRegistrationControllerTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicConfigShowsRegistrationClosed() throws Exception {
        mockMvc.perform(get("/auth/public-config"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openRegistration").value(false))
            .andExpect(jsonPath("$.desktop").value(false));
    }

    @Test
    void rejectsPublicRegisterAndUnknownOtp() throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "fullName": "Blocked Owner",
                      "email": "blocked-owner@example.com",
                      "password": "password123"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(post("/auth/otp/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "phone": "9876500999" }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not registered")));
    }

    @Test
    void rejectsSelfServeShopCreateButAllowsPlatformCreate() throws Exception {
        UserAccount user = createUserAccount();
        mockMvc.perform(post("/businesses/quick-start")
                .header("Authorization", authorizationHeader(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "shopName": "Should Fail", "mobileNumber": "9876500888" }
                    """))
            .andExpect(status().isForbidden());

        UserAccount admin = createPlatformAdmin();
        String unique = String.valueOf(System.nanoTime());
        mockMvc.perform(post("/platform/shops")
                .header("Authorization", authorizationHeader(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "shopName": "Admin Created Shop",
                      "ownerName": "Shop Owner",
                      "email": "admin-created-%s@example.com",
                      "phone": "8%s",
                      "provisionStarterCatalog": false
                    }
                    """.formatted(unique, unique.substring(unique.length() - 9))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.shop.name").value("Admin Created Shop"));
    }
}
