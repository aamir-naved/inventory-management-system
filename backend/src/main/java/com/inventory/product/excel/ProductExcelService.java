package com.inventory.product.excel;

import com.inventory.common.tenant.TenantContext;
import com.inventory.product.dto.ProductImportResponse;
import com.inventory.product.dto.ProductImportRowError;
import com.inventory.product.dto.ProductRequest;
import com.inventory.product.entity.Product;
import com.inventory.product.excel.ProductWorkbook.ParsedRow;
import com.inventory.product.repository.ProductRepository;
import com.inventory.product.service.ProductService;
import com.inventory.settings.service.SettingsService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductExcelService {

    private final ProductService productService;
    private final ProductRepository productRepository;
    private final SettingsService settingsService;

    public ProductExcelService(
        ProductService productService,
        ProductRepository productRepository,
        SettingsService settingsService
    ) {
        this.productService = productService;
        this.productRepository = productRepository;
        this.settingsService = settingsService;
    }

    public byte[] exportCatalog() {
        UUID businessId = currentBusinessId();
        List<Product> products = productRepository.searchAll(businessId, "", false);
        return ProductWorkbook.export(products);
    }

    public ProductImportResponse importCatalog(MultipartFile file) {
        validateFile(file);

        List<ParsedRow> rows;
        try (InputStream inputStream = file.getInputStream()) {
            rows = ProductWorkbook.parse(inputStream);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read the Excel file.");
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The Excel file has no product rows to import.");
        }

        BigDecimal defaultLowStock = settingsService.get().defaultLowStockThreshold();
        Set<String> skusInFile = new HashSet<>();
        int created = 0;
        int updated = 0;
        List<ProductImportRowError> errors = new ArrayList<>();

        for (ParsedRow row : rows) {
            try {
                if (importRow(row, defaultLowStock, skusInFile)) {
                    updated += 1;
                } else {
                    created += 1;
                }
            } catch (RuntimeException exception) {
                errors.add(new ProductImportRowError(
                    row.rowNumber(),
                    exception.getMessage() == null ? "Unable to import this row" : exception.getMessage()
                ));
            }
        }

        return new ProductImportResponse(created, updated, errors.size(), List.copyOf(errors));
    }

    private boolean importRow(ParsedRow row, BigDecimal defaultLowStock, Set<String> skusInFile) {
        String name = requiredText(row.name(), "Name is required", 150, "Product name");
        String unit = requiredText(row.unit(), "Unit is required", 30, "Unit");
        String sku = optionalText(row.sku(), 60, "SKU");
        String category = optionalText(row.category(), 100, "Category");

        if (sku != null) {
            String skuKey = sku.toLowerCase(Locale.ROOT);
            if (!skusInFile.add(skuKey)) {
                throw new IllegalArgumentException("SKU " + sku + " is used more than once in this file");
            }
        }

        ProductRequest request = new ProductRequest(
            name,
            sku,
            category,
            unit,
            parseMoney(row.costPrice(), "Cost price"),
            parseMoney(row.sellingPrice(), "Selling price"),
            parseQuantity(row.openingStock(), "Opening stock"),
            parseQuantityOrDefault(row.lowStockThreshold(), "Low stock threshold", defaultLowStock),
            optionalText(row.barcode(), 64, "Barcode"),
            optionalText(row.hsnCode(), 8, "HSN"),
            parseGstRate(row.gstRate())
        );

        Optional<Product> existing = findExisting(sku, name);
        if (existing.isPresent()) {
            Product product = existing.get();
            if (product.isArchived()) {
                throw new IllegalArgumentException("SKU " + product.getSku() + " belongs to an archived product");
            }

            productService.update(product.getId(), new ProductRequest(
                request.name(),
                request.sku() != null ? request.sku() : product.getSku(),
                request.category(),
                request.unit(),
                request.costPrice(),
                request.sellingPrice(),
                product.getOpeningStock(),
                request.lowStockThreshold(),
                request.barcode() != null ? request.barcode() : product.getBarcode(),
                request.hsnCode() != null ? request.hsnCode() : product.getHsnCode(),
                request.gstRate()
            ));
            return true;
        }

        productService.create(request);
        return false;
    }

    private Optional<Product> findExisting(String sku, String name) {
        UUID businessId = currentBusinessId();
        if (sku != null) {
            return productRepository.findFirstByBusinessIdAndSkuIgnoreCase(businessId, sku);
        }

        List<Product> matches = productRepository.findByBusinessIdAndNameIgnoreCaseAndArchivedFalse(businessId, name);
        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                "More than one product is named \"" + name + "\". Add a SKU so the correct row can be updated."
            );
        }

        return matches.stream().findFirst();
    }

    private UUID currentBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an Excel .xlsx file to import.");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".xls") && !filename.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Save the file as .xlsx and try again.");
        }
        if (!filename.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Upload an Excel .xlsx file.");
        }
    }

    private static String requiredText(String value, String missingMessage, int maxLength, String fieldName) {
        String normalized = optionalText(value, maxLength, fieldName);
        if (normalized == null) {
            throw new IllegalArgumentException(missingMessage);
        }
        return normalized;
    }

    private static String optionalText(String value, int maxLength, String fieldName) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " must be " + maxLength + " characters or fewer");
        }
        return trimmed;
    }

    private static BigDecimal parseMoney(String value, String fieldName) {
        return parseDecimal(value, fieldName, 2, BigDecimal.ZERO);
    }

    private static BigDecimal parseQuantity(String value, String fieldName) {
        return parseDecimal(value, fieldName, 3, BigDecimal.ZERO);
    }

    private static BigDecimal parseQuantityOrDefault(String value, String fieldName, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback != null ? fallback : BigDecimal.ZERO;
        }
        return parseDecimal(value, fieldName, 3, fallback);
    }

    private static BigDecimal parseGstRate(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return parseDecimal(value.replace("%", ""), "GST %", 2, BigDecimal.ZERO);
    }

    private static BigDecimal parseDecimal(String value, String fieldName, int scale, BigDecimal fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String cleaned = value.trim().replace(",", "");
        try {
            BigDecimal parsed = new BigDecimal(cleaned).setScale(scale, RoundingMode.HALF_UP);
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative");
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a number");
        }
    }
}
