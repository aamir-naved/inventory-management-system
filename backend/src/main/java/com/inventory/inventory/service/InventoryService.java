package com.inventory.inventory.service;

import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.common.tenant.TenantContext;
import com.inventory.inventory.dto.InventoryAdjustmentRequest;
import com.inventory.inventory.dto.InventoryMovementResponse;
import com.inventory.inventory.dto.InventoryStockResponse;
import com.inventory.inventory.dto.InventorySummaryResponse;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.product.entity.Product;
import com.inventory.product.repository.ProductRepository;
import com.inventory.settings.service.SettingsService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final SettingsService settingsService;

    public InventoryService(
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        SettingsService settingsService
    ) {
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.settingsService = settingsService;
    }

    @Transactional(readOnly = true)
    public PagedResponse<InventoryStockResponse> listStock(
        String search,
        boolean lowStockOnly,
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
                lowStockOnly,
                Pagination.pageable(page, size)
            ),
            this::toStockResponse
        );
    }

    @Transactional(readOnly = true)
    public List<InventoryStockResponse> listStockAll(
        String search,
        boolean lowStockOnly,
        boolean includeArchived
    ) {
        UUID businessId = requireBusinessId();
        return productRepository.search(
                businessId,
                normalizeSearch(search),
                includeArchived,
                lowStockOnly,
                Pageable.unpaged()
            )
            .stream()
            .map(this::toStockResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public InventorySummaryResponse getSummary() {
        UUID businessId = requireBusinessId();
        List<Product> products = productRepository.searchAll(businessId, "", false);

        long lowStockProducts = products.stream()
            .filter(this::isLowStock)
            .count();

        BigDecimal totalStockValue = products.stream()
            .map(this::stockValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPotentialRevenue = products.stream()
            .map(product -> product.getCurrentStock().multiply(product.getSellingPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventorySummaryResponse(
            products.size(),
            lowStockProducts,
            totalStockValue,
            totalPotentialRevenue
        );
    }

    public InventoryMovementResponse adjustStock(InventoryAdjustmentRequest request) {
        Product product = findProductForUpdate(request.productId());

        if (product.isArchived()) {
            throw new IllegalArgumentException("Archived products cannot be adjusted");
        }

        BigDecimal quantityBefore = product.getCurrentStock();
        BigDecimal quantityAfter = quantityBefore.add(request.adjustmentQuantity());

        if (quantityAfter.compareTo(BigDecimal.ZERO) < 0 && !settingsService.isNegativeStockAllowed()) {
            throw new IllegalArgumentException("Stock cannot go below zero");
        }

        product.setCurrentStock(quantityAfter);
        productRepository.save(product);

        InventoryMovement movement = new InventoryMovement();
        movement.setBusinessId(product.getBusinessId());
        movement.setProduct(product);
        movement.setMovementType(InventoryMovement.TYPE_MANUAL_ADJUSTMENT);
        movement.setQuantityChange(request.adjustmentQuantity());
        movement.setQuantityBefore(quantityBefore);
        movement.setQuantityAfter(quantityAfter);
        movement.setNotes(request.reason().trim());

        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);
        return toMovementResponse(savedMovement);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InventoryMovementResponse> listMovements(UUID productId, Integer page, Integer size) {
        UUID businessId = requireBusinessId();
        var pageable = Pagination.pageable(page, size);
        var movements = productId == null
            ? inventoryMovementRepository.findByBusinessIdOrderByCreatedAtDesc(businessId, pageable)
            : inventoryMovementRepository.findByBusinessIdAndProduct_IdOrderByCreatedAtDesc(
                businessId,
                productId,
                pageable
            );

        return Pagination.map(movements, this::toMovementResponse);
    }

    private Product findProductForUpdate(UUID productId) {
        UUID businessId = requireBusinessId();

        return productRepository.findByIdAndBusinessIdForUpdate(productId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
    }

    private InventoryStockResponse toStockResponse(Product product) {
        return new InventoryStockResponse(
            product.getId(),
            product.getName(),
            product.getSku(),
            product.getCategory(),
            product.getUnit(),
            product.getCurrentStock(),
            product.getCostPrice(),
            product.getSellingPrice(),
            stockValue(product),
            product.getLowStockThreshold(),
            isLowStock(product),
            product.isArchived()
        );
    }

    private InventoryMovementResponse toMovementResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
            movement.getId(),
            movement.getProduct().getId(),
            movement.getProduct().getName(),
            movement.getMovementType(),
            movement.getQuantityChange(),
            movement.getQuantityBefore(),
            movement.getQuantityAfter(),
            movement.getNotes(),
            movement.getCreatedAt()
        );
    }

    private boolean isLowStock(Product product) {
        return product.getLowStockThreshold().compareTo(BigDecimal.ZERO) > 0
            && product.getCurrentStock().compareTo(product.getLowStockThreshold()) <= 0;
    }

    private BigDecimal stockValue(Product product) {
        return product.getCurrentStock().multiply(product.getCostPrice());
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
}
