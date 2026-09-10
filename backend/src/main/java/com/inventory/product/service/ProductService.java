package com.inventory.product.service;

import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.common.tenant.TenantContext;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.product.dto.ProductRequest;
import com.inventory.product.dto.ProductResponse;
import com.inventory.product.entity.Product;
import com.inventory.product.repository.ProductRepository;
import com.inventory.tax.GstCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;

    public ProductService(
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
    }

    public ProductResponse create(ProductRequest request) {
        UUID businessId = requireBusinessId();
        validateSkuUniqueness(businessId, request.sku(), null);
        validateBarcodeUniqueness(businessId, request.barcode(), null);

        Product product = new Product();
        product.setBusinessId(businessId);
        applyRequest(product, request, true);

        Product savedProduct = save(product);
        recordMovement(
            savedProduct,
            InventoryMovement.TYPE_OPENING_STOCK,
            savedProduct.getOpeningStock(),
            BigDecimal.ZERO,
            savedProduct.getCurrentStock(),
            "Initial opening stock"
        );

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> list(
        String search,
        boolean includeArchived,
        Integer page,
        Integer size
    ) {
        UUID businessId = requireBusinessId();

        return Pagination.map(
            productRepository.search(
                businessId,
                normalizeSearch(search),
                includeArchived,
                false,
                Pagination.pageable(page, size)
            ),
            this::toResponse
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return toResponse(findProduct(id));
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        UUID businessId = requireBusinessId();
        Product product = findProductForUpdate(id);

        validateSkuUniqueness(businessId, request.sku(), id);
        validateBarcodeUniqueness(businessId, request.barcode(), id);
        BigDecimal quantityBefore = product.getCurrentStock();
        BigDecimal previousOpeningStock = product.getOpeningStock();
        applyRequest(product, request, false);
        Product savedProduct = save(product);

        BigDecimal stockDelta = savedProduct.getOpeningStock().subtract(previousOpeningStock);
        if (stockDelta.compareTo(BigDecimal.ZERO) != 0) {
            recordMovement(
                savedProduct,
                InventoryMovement.TYPE_OPENING_STOCK_UPDATE,
                stockDelta,
                quantityBefore,
                savedProduct.getCurrentStock(),
                "Opening stock updated"
            );
        }

        return toResponse(savedProduct);
    }

    public ProductResponse archive(UUID id) {
        Product product = findProduct(id);
        product.setArchived(true);
        return toResponse(save(product));
    }

    public ProductResponse unarchive(UUID id) {
        Product product = findProduct(id);
        product.setArchived(false);
        return toResponse(save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse getByBarcode(String barcode) {
        String normalized = normalize(barcode);
        if (normalized == null) {
            throw new IllegalArgumentException("Barcode is required");
        }
        return toResponse(
            productRepository.findFirstByBusinessIdAndBarcodeIgnoreCase(requireBusinessId(), normalized)
                .orElseThrow(() -> new EntityNotFoundException("No product matches that barcode"))
        );
    }

    private Product findProduct(UUID id) {
        UUID businessId = requireBusinessId();

        return productRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    private Product findProductForUpdate(UUID id) {
        UUID businessId = requireBusinessId();

        return productRepository.findByIdAndBusinessIdForUpdate(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    private void applyRequest(Product product, ProductRequest request, boolean creating) {
        product.setName(request.name().trim());
        product.setSku(normalize(request.sku()));
        product.setCategory(normalize(request.category()));
        product.setUnit(request.unit().trim());
        product.setCostPrice(request.costPrice());
        product.setSellingPrice(request.sellingPrice());
        product.setLowStockThreshold(request.lowStockThreshold());
        product.setBarcode(normalize(request.barcode()));
        product.setHsnCode(normalizeHsn(request.hsnCode()));
        product.setGstRate(GstCalculator.normalizeRate(request.gstRate()));

        if (creating) {
            product.setOpeningStock(request.openingStock());
            product.setCurrentStock(request.openingStock());
            return;
        }

        BigDecimal stockDelta = request.openingStock().subtract(product.getOpeningStock());
        product.setOpeningStock(request.openingStock());
        product.setCurrentStock(product.getCurrentStock().add(stockDelta));
    }

    private void validateSkuUniqueness(UUID businessId, String sku, UUID productId) {
        String normalizedSku = normalize(sku);

        if (normalizedSku == null) {
            return;
        }

        boolean exists = productId == null
            ? productRepository.existsByBusinessIdAndSkuIgnoreCase(businessId, normalizedSku)
            : productRepository.existsByBusinessIdAndSkuIgnoreCaseAndIdNot(businessId, normalizedSku, productId);

        if (exists) {
            throw new IllegalArgumentException("SKU must be unique within the business");
        }
    }

    private void validateBarcodeUniqueness(UUID businessId, String barcode, UUID productId) {
        String normalized = normalize(barcode);
        if (normalized == null) {
            return;
        }
        boolean exists = productId == null
            ? productRepository.existsByBusinessIdAndBarcodeIgnoreCase(businessId, normalized)
            : productRepository.existsByBusinessIdAndBarcodeIgnoreCaseAndIdNot(businessId, normalized, productId);
        if (exists) {
            throw new IllegalArgumentException("Barcode must be unique within the business");
        }
    }

    private Product save(Product product) {
        try {
            return productRepository.save(product);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Unable to save product with the provided values");
        }
    }

    private void recordMovement(
        Product product,
        String movementType,
        BigDecimal quantityChange,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String notes
    ) {
        InventoryMovement movement = new InventoryMovement();
        movement.setBusinessId(product.getBusinessId());
        movement.setProduct(product);
        movement.setMovementType(movementType);
        movement.setQuantityChange(quantityChange);
        movement.setQuantityBefore(quantityBefore);
        movement.setQuantityAfter(quantityAfter);
        movement.setNotes(notes);

        inventoryMovementRepository.save(movement);
    }

    private UUID requireBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeSearch(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getBusinessId(),
            product.getName(),
            product.getSku(),
            product.getCategory(),
            product.getUnit(),
            product.getCostPrice(),
            product.getSellingPrice(),
            product.getOpeningStock(),
            product.getCurrentStock(),
            product.getLowStockThreshold(),
            product.isArchived(),
            product.getBarcode(),
            product.getHsnCode(),
            product.getGstRate() == null ? java.math.BigDecimal.ZERO : product.getGstRate(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }

    private String normalizeHsn(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
