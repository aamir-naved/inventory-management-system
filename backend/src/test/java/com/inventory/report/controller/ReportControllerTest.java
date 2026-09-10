package com.inventory.report.controller;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerTest extends AuthenticatedControllerTestSupport {

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
    void returnsInventorySalesPurchasesAndOutstandingReports() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        createProduct("Low Stock Screws", "SCR-001", 10.000, 40.000);
        String cementId = createProduct("Ultra Cement", "CEM-001", 100.000, 20.000);
        String customerId = createCustomer("Apex Builders");
        String supplierId = createSupplier("Shakti Cements Ltd");

        // Today sale: 5 * 360 = 1800, paid 500 → outstanding 1300
        createSale(customerId, cementId, today, "500.00", 5);
        // Yesterday sale: fully paid → excluded from outstanding, included in unfiltered sales
        createSale(customerId, cementId, yesterday, "1800.00", 5);
        // Cancelled sale must be excluded
        String cancelledSaleId = createSale(customerId, cementId, today, "0", 2);
        mockMvc.perform(patch("/sales/{id}/cancel", cancelledSaleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reason":"Entered by mistake"}
                    """))
            .andExpect(status().isOk());

        // Today purchase unpaid: 10 * 315 = 3150
        createPurchase(supplierId, cementId, today, "0", 10);
        // Yesterday purchase fully paid
        createPurchase(supplierId, cementId, yesterday, "3150.00", 10);

        mockMvc.perform(get("/reports/inventory")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalProducts").value(2))
            .andExpect(jsonPath("$.lowStockProducts").value(1))
            .andExpect(jsonPath("$.rows", hasSize(2)));

        mockMvc.perform(get("/reports/inventory")
                .param("lowStockOnly", "true")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalProducts").value(1))
            .andExpect(jsonPath("$.rows[0].productName").value("Low Stock Screws"))
            .andExpect(jsonPath("$.rows[0].lowStock").value(true));

        mockMvc.perform(get("/reports/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rowCount").value(2))
            .andExpect(jsonPath("$.totalNetAmount").value(3600.0))
            .andExpect(jsonPath("$.totalOutstanding").value(1300.0));

        mockMvc.perform(get("/reports/sales")
                .param("from", today.toString())
                .param("to", today.toString())
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.from").value(today.toString()))
            .andExpect(jsonPath("$.to").value(today.toString()))
            .andExpect(jsonPath("$.rowCount").value(1))
            .andExpect(jsonPath("$.totalNetAmount").value(1800.0))
            .andExpect(jsonPath("$.rows[0].outstandingAmount").value(1300.0))
            .andExpect(jsonPath("$.rows[0].paymentStatus").value("PARTIAL"));

        mockMvc.perform(get("/reports/purchases")
                .param("from", today.toString())
                .param("to", today.toString())
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rowCount").value(1))
            .andExpect(jsonPath("$.totalAmount").value(3150.0))
            .andExpect(jsonPath("$.totalOutstanding").value(3150.0))
            .andExpect(jsonPath("$.rows[0].supplierName").value("Shakti Cements Ltd"));

        mockMvc.perform(get("/reports/outstanding/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerCount").value(1))
            .andExpect(jsonPath("$.totalOutstanding").value(1300.0))
            .andExpect(jsonPath("$.rows[0].customerName").value("Apex Builders"))
            .andExpect(jsonPath("$.rows[0].invoiceCount").value(1));

        mockMvc.perform(get("/reports/outstanding/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supplierCount").value(1))
            .andExpect(jsonPath("$.totalOutstanding").value(3150.0))
            .andExpect(jsonPath("$.rows[0].supplierName").value("Shakti Cements Ltd"))
            .andExpect(jsonPath("$.rows[0].billCount").value(1));
    }

    @Test
    void returnsEmptyOutstandingReportsForSettledBusiness() throws Exception {
        mockMvc.perform(get("/reports/outstanding/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerCount").value(0))
            .andExpect(jsonPath("$.totalOutstanding").value(0))
            .andExpect(jsonPath("$.rows", hasSize(0)));

        mockMvc.perform(get("/reports/outstanding/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.supplierCount").value(0))
            .andExpect(jsonPath("$.totalOutstanding").value(0))
            .andExpect(jsonPath("$.rows", hasSize(0)));
    }

    @Test
    void gstReportSplitsOutputAndInputAndSubtractsReturns() throws Exception {
        mockMvc.perform(put("/settings")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "currencyCode":"INR",
                      "dateFormat":"dd/MM/yyyy",
                      "allowNegativeStock":false,
                      "defaultLowStockThreshold":0,
                      "gstEnabled":true,
                      "gstInclusivePricing":false,
                      "gstin":"29ABCDE1234F1Z5",
                      "stateCode":"29",
                      "stateName":"Karnataka"
                    }
                    """))
            .andExpect(status().isOk());

        String productId = createGstProduct();
        String customerId = createCustomer("GST Buyer");
        String supplierId = createSupplier("GST Supplier");

        String saleResponse = mockMvc.perform(post("/sales")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId":"%s",
                      "saleDate":"2026-08-01",
                      "amountPaid":0,
                      "interstate":false,
                      "items":[{"productId":"%s","quantity":10.000,"sellingPrice":100.00,"gstRate":18.00}]
                    }
                    """.formatted(customerId, productId)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        String saleId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.id");
        String saleItemId = com.jayway.jsonpath.JsonPath.read(saleResponse, "$.items[0].id");

        mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"2026-08-01",
                      "amountPaid":0,
                      "interstate":false,
                      "items":[{"productId":"%s","quantity":10.000,"purchasePrice":50.00,"gstRate":18.00}]
                    }
                    """.formatted(supplierId, productId)))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/sales/{saleId}/returns", saleId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "returnDate":"2026-08-02",
                      "reason":"Damaged",
                      "items":[{"saleItemId":"%s","quantity":5.000}]
                    }
                    """.formatted(saleItemId)))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/reports/gst")
                .param("from", "2026-08-01")
                .param("to", "2026-08-31")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outputTaxableAmount").value(500.0))
            .andExpect(jsonPath("$.outputTax").value(90.0))
            .andExpect(jsonPath("$.inputTaxableAmount").value(500.0))
            .andExpect(jsonPath("$.inputTax").value(90.0))
            .andExpect(jsonPath("$.netTax").value(0.0));
    }

    private String createGstProduct() throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"GST Cement","sku":"CEM-GST","category":"Cement","unit":"Bags",
                      "costPrice":50.00,"sellingPrice":100.00,"openingStock":200.000,"lowStockThreshold":10.000,
                      "hsnCode":"252329","gstRate":18.00
                    }
                    """))
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

    private void createPurchase(
        String supplierId,
        String productId,
        LocalDate purchaseDate,
        String amountPaid,
        int quantity
    ) throws Exception {
        mockMvc.perform(post("/purchases")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "supplierId":"%s",
                      "purchaseDate":"%s",
                      "amountPaid":%s,
                      "notes":"Restocking",
                      "items":[{"productId":"%s","quantity":%d.000,"purchasePrice":315.00}]
                    }
                    """.formatted(supplierId, purchaseDate, amountPaid, productId, quantity)))
            .andExpect(status().isCreated());
    }

    private String createCustomer(String name) throws Exception {
        String response = mockMvc.perform(post("/customers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"%s","contactPerson":"Neha Shah","mobileNumber":"+91 9998887776","addressLine":"Ring Road"}
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createSupplier(String name) throws Exception {
        String response = mockMvc.perform(post("/suppliers")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"%s",
                      "contactPerson":"Rahul Mehta",
                      "mobileNumber":"+91 9876543210",
                      "addressLine":"Industrial Road"
                    }
                    """.formatted(name)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }

    private String createProduct(String name, String sku, double openingStock, double lowStockThreshold)
        throws Exception {
        String response = mockMvc.perform(post("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"%s","sku":"%s","category":"Hardware","unit":"Bags",
                      "costPrice":320.00,"sellingPrice":360.00,"openingStock":%.3f,"lowStockThreshold":%.3f
                    }
                    """.formatted(name, sku, openingStock, lowStockThreshold)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.id");
    }
}
