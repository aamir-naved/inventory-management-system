package com.inventory.product.controller;

import com.inventory.common.api.PagedResponse;
import com.inventory.product.dto.ProductImportResponse;
import com.inventory.product.dto.ProductRequest;
import com.inventory.product.dto.ProductResponse;
import com.inventory.product.excel.ProductExcelService;
import com.inventory.product.excel.ProductWorkbook;
import com.inventory.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ProductService productService;
    private final ProductExcelService productExcelService;

    public ProductController(ProductService productService, ProductExcelService productExcelService) {
        this.productService = productService;
        this.productExcelService = productExcelService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PagedResponse<ProductResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean includeArchived,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return productService.list(search, includeArchived, page, size);
    }

    @GetMapping("/export.xlsx")
    public ResponseEntity<byte[]> exportExcel() {
        byte[] content = productExcelService.exportCatalog();
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment()
                    .filename(ProductWorkbook.FILENAME, StandardCharsets.UTF_8)
                    .build()
                    .toString()
            )
            .contentType(EXCEL_MEDIA_TYPE)
            .body(content);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImportResponse importExcel(@RequestParam("file") MultipartFile file) {
        return productExcelService.importCatalog(file);
    }

    @GetMapping("/by-barcode/{barcode}")
    public ProductResponse getByBarcode(@PathVariable String barcode) {
        return productService.getByBarcode(barcode);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @PatchMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public ProductResponse archive(@PathVariable UUID id) {
        return productService.archive(id);
    }

    @PatchMapping("/{id}/unarchive")
    public ProductResponse unarchive(@PathVariable UUID id) {
        return productService.unarchive(id);
    }
}
