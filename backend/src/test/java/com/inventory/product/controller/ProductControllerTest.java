package com.inventory.product.controller;

import com.inventory.auth.entity.UserAccount;
import com.inventory.business.entity.Business;
import com.inventory.support.AuthenticatedControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalItems").value(2));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("search", "cement"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].name").value("Ultra Cement"));
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
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.totalItems").value(0));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("includeArchived", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].archived").value(true));

        mockMvc.perform(patch("/products/{id}/unarchive", productId)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.archived").value(false));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].archived").value(false));
    }

    @Test
    void requiresBusinessHeader() throws Exception {
        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("X-Business-Id header is required"));
    }

    @Test
    void exportsProductsExcel() throws Exception {
        createProduct("Ultra Cement", "CEM-001", "Cement");

        byte[] body = mockMvc.perform(get("/products/export.xlsx")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(
                MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            ))
            .andExpect(header().string("Content-Disposition", containsString("products.xlsx")))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();

        assertThat(body.length).isGreaterThan(100);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new java.io.ByteArrayInputStream(body))) {
            Sheet sheet = workbook.getSheet("Products");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Ultra Cement");
        }
    }

    @Test
    void importsProductsFromExcel() throws Exception {
        MockMultipartFile file = excelFile(new String[][] {
            {"Name", "SKU", "Category", "Unit", "Cost price", "Selling price", "Opening stock", "Low stock threshold"},
            {"Red Bricks", "BRK-001", "Bricks", "Pieces", "8", "12", "500", "50"}
        });

        mockMvc.perform(multipart("/products/import")
                .file(file)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.updated").value(0))
            .andExpect(jsonPath("$.failed").value(0));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("search", "bricks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("BRK-001"))
            .andExpect(jsonPath("$.items[0].currentStock").value(500.0));
    }

    @Test
    void importUpdatesBySkuWithoutChangingStock() throws Exception {
        createProduct("Ultra Cement", "CEM-001", "Cement");

        MockMultipartFile file = excelFile(new String[][] {
            {"Name", "SKU", "Category", "Unit", "Cost price", "Selling price", "Opening stock", "Low stock threshold"},
            {"Ultra Cement Premium", "CEM-001", "Cement", "Bags", "330", "400", "999", "50"}
        });

        mockMvc.perform(multipart("/products/import")
                .file(file)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(0))
            .andExpect(jsonPath("$.updated").value(1));

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].name").value("Ultra Cement Premium"))
            .andExpect(jsonPath("$.items[0].sellingPrice").value(400.0))
            .andExpect(jsonPath("$.items[0].currentStock").value(120.0))
            .andExpect(jsonPath("$.items[0].openingStock").value(120.0));
    }

    @Test
    void importReportsRowErrorsWithoutAbortingValidRows() throws Exception {
        MockMultipartFile file = excelFile(new String[][] {
            {"Name", "SKU", "Unit", "Selling price"},
            {"Steel Rod", "STL-001", "Tons", "62000"},
            {"", "BAD-001", "Bags", "10"}
        });

        mockMvc.perform(multipart("/products/import")
                .file(file)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.created").value(1))
            .andExpect(jsonPath("$.failed").value(1))
            .andExpect(jsonPath("$.errors[0].rowNumber").value(3))
            .andExpect(jsonPath("$.errors[0].message").value("Name is required"));
    }

    @Test
    void rejectsNonExcelImport() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "products.txt",
            "text/plain",
            "not excel".getBytes()
        );

        mockMvc.perform(multipart("/products/import")
                .file(file)
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Upload an Excel .xlsx file."));
    }

    @Test
    void paginatesProductList() throws Exception {
        createProduct("Alpha Cement", "CEM-A", "Cement");
        createProduct("Beta Bricks", "BRK-B", "Bricks");
        createProduct("Gamma Steel", "STL-G", "Steel");

        mockMvc.perform(get("/products")
                .header("Authorization", authorizationHeader)
                .header("X-Business-Id", businessId)
                .param("size", "2")
                .param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalItems").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
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

    private MockMultipartFile excelFile(String[][] rows) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Products");
            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                for (int columnIndex = 0; columnIndex < rows[rowIndex].length; columnIndex++) {
                    row.createCell(columnIndex).setCellValue(rows[rowIndex][columnIndex]);
                }
            }
            workbook.write(output);
            return new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                output.toByteArray()
            );
        }
    }
}
