package com.inventory.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.inventory.auth.entity.UserAccount;
import com.inventory.auth.mail.LoggingMailService;
import com.inventory.business.entity.Business;
import com.inventory.support.AuthenticatedControllerTestSupport;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class PlatformControllerTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoggingMailService loggingMailService;

    private String adminAuth;
    private UserAccount shopOwner;
    private String shopOwnerAuth;
    private Business shop;

    @BeforeEach
    void setUp() {
        UserAccount admin = createPlatformAdmin();
        adminAuth = authorizationHeader(admin);
        shopOwner = createUserAccount();
        shopOwnerAuth = authorizationHeader(shopOwner);
        shop = createBusinessFor(shopOwner);
    }

    @Test
    void platformAdminReadsStatsAndCannotReadShopCatalog() throws Exception {
        mockMvc.perform(get("/platform/stats").header("Authorization", adminAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shopCount").isNumber());

        mockMvc.perform(get("/platform/shops").header("Authorization", adminAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/platform/users").header("Authorization", adminAuth))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());

        mockMvc.perform(get("/products").header("Authorization", adminAuth))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/products")
                .header("Authorization", adminAuth)
                .header("X-Business-Id", shop.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void shopOwnerCannotOpenPlatformConsole() throws Exception {
        mockMvc.perform(get("/platform/stats").header("Authorization", shopOwnerAuth))
            .andExpect(status().isForbidden());
    }

    @Test
    void shopOwnerCannotUseAnotherShopsBusinessId() throws Exception {
        UserAccount otherOwner = createUserAccount();
        Business otherShop = createBusinessFor(otherOwner);

        mockMvc.perform(get("/products")
                .header("Authorization", shopOwnerAuth)
                .header("X-Business-Id", otherShop.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void suspendedShopIsBlocked() throws Exception {
        shop.setActive(false);
        businessRepository.save(shop);

        mockMvc.perform(get("/products")
                .header("Authorization", shopOwnerAuth)
                .header("X-Business-Id", shop.getId()))
            .andExpect(status().isForbidden());
    }

    @Test
    void addShopProvisionsWalkInAndStarterCatalog() throws Exception {
        String unique = String.valueOf(System.nanoTime());
        String ownerEmail = "owner-%s@khanstore.com".formatted(unique);
        loggingMailService.clear();
        String created = mockMvc.perform(post("/platform/shops")
                .header("Authorization", adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "shopName": "Khan General Store",
                      "ownerName": "Aamir Khan",
                      "email": "%s",
                      "phone": "9%s",
                      "provisionStarterCatalog": true
                    }
                    """.formatted(ownerEmail, unique.substring(unique.length() - 9))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.temporaryPassword").doesNotExist())
            .andExpect(jsonPath("$.passwordDelivery").value("EMAIL"))
            .andExpect(jsonPath("$.shop.name").value("Khan General Store"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        String shopId = JsonPath.read(created, "$.shop.id");
        String mailBody = loggingMailService.findLatestTo(ownerEmail)
            .orElseThrow()
            .body();
        assertThat(mailBody).contains("Temporary password:");
        String temporaryPassword = mailBody.lines()
            .filter(line -> line.startsWith("Temporary password:"))
            .map(line -> line.substring("Temporary password:".length()).trim())
            .findFirst()
            .orElseThrow();
        assertThat(temporaryPassword).isNotBlank();

        UserAccount owner = userAccountRepository.findByEmailIgnoreCase(ownerEmail).orElseThrow();
        String ownerAuth = authorizationHeader(owner);

        mockMvc.perform(get("/customers")
                .header("Authorization", ownerAuth)
                .header("X-Business-Id", shopId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].name", hasItem("Walk-in")));

        mockMvc.perform(get("/products")
                .header("Authorization", ownerAuth)
                .header("X-Business-Id", shopId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalItems").value(5))
            .andExpect(jsonPath("$.items[*].barcode", hasItem("CEM-001")));
    }

    @Test
    void suspendAndActivateViaPlatformApi() throws Exception {
        mockMvc.perform(patch("/platform/shops/" + shop.getId())
                .header("Authorization", adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "active": false, "suspendedReason": "Pilot ended" }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(false))
            .andExpect(jsonPath("$.suspendedReason").value("Pilot ended"));

        mockMvc.perform(get("/products")
                .header("Authorization", shopOwnerAuth)
                .header("X-Business-Id", shop.getId()))
            .andExpect(status().isForbidden());

        mockMvc.perform(patch("/platform/shops/" + shop.getId())
                .header("Authorization", adminAuth)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "active": true }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.active").value(true));
    }
}
