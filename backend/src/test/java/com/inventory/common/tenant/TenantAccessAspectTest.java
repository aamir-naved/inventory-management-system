package com.inventory.common.tenant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.inventory.auth.entity.MembershipRole;
import com.inventory.auth.entity.UserAccount;
import com.inventory.business.entity.Business;
import com.inventory.product.repository.ProductRepository;
import com.inventory.support.AuthenticatedControllerTestSupport;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
class TenantAccessAspectTest extends AuthenticatedControllerTestSupport {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    private UserAccount ownerA;
    private String authA;
    private UUID businessA;
    private UUID productInB;

    @BeforeEach
    void setUp() throws Exception {
        ownerA = createUserAccount();
        authA = authorizationHeader(ownerA);
        Business shopA = createBusinessFor(ownerA);
        businessA = shopA.getId();

        UserAccount ownerB = createUserAccount();
        String authB = authorizationHeader(ownerB);
        Business shopB = createBusinessFor(ownerB);
        UUID businessB = shopB.getId();

        String created = mockMvc.perform(post("/products")
                .header("Authorization", authB)
                .header("X-Business-Id", businessB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Other Shop Cement","sku":"OTHER-CEM","category":"Cement","unit":"Bags",
                      "costPrice":100.00,"sellingPrice":120.00,"openingStock":10.000,"lowStockThreshold":1.000
                    }
                    """))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        productInB = UUID.fromString(JsonPath.read(created, "$.id"));
    }

    @Test
    void httpGetHidesForeignProductEvenWithOwnBusinessHeader() throws Exception {
        mockMvc.perform(get("/products/{id}", productInB)
                .header("Authorization", authA)
                .header("X-Business-Id", businessA))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void aspectBlocksBareFindByIdAcrossTenants() {
        TenantContext.set(businessA, MembershipRole.OWNER, ownerA.getId());
        try {
            assertThatThrownBy(() -> productRepository.findById(productInB))
                .isInstanceOf(EntityNotFoundException.class);
        } finally {
            TenantContext.clear();
        }
    }
}
