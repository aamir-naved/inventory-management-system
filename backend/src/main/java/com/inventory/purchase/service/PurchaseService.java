package com.inventory.purchase.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.payment.service.PaymentService;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.product.entity.Product;
import com.inventory.product.repository.ProductRepository;
import com.inventory.purchase.dto.PurchaseCancellationRequest;
import com.inventory.purchase.dto.PurchaseItemRequest;
import com.inventory.purchase.dto.PurchaseItemResponse;
import com.inventory.purchase.dto.PurchaseRequest;
import com.inventory.purchase.dto.PurchaseResponse;
import com.inventory.purchase.dto.PurchaseUpdateRequest;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.entity.PurchaseItem;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;

    public PurchaseService(
        PurchaseRepository purchaseRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService
    ) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
    }

    public PurchaseResponse create(PurchaseRequest request) {
        UUID businessId = requireBusinessId();
        Supplier supplier = findSupplier(request.supplierId(), businessId);

        Purchase purchase = new Purchase();
        purchase.setBusinessId(businessId);
        purchase.setPurchaseNumber(generatePurchaseNumber());
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setAmountPaid(BigDecimal.ZERO);
        purchase.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
        purchase.setNotes(normalize(request.notes()));
        purchase.setCancelled(false);

        List<PurchaseItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (PurchaseItemRequest itemRequest : request.items()) {
            Product product = findProduct(itemRequest.productId(), businessId);

            if (product.isArchived()) {
                throw new IllegalArgumentException("Archived products cannot be purchased");
            }

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.purchasePrice());
            item.setLineTotal(itemRequest.quantity().multiply(itemRequest.purchasePrice()));
            items.add(item);
            totalAmount = totalAmount.add(item.getLineTotal());
        }

        BigDecimal initialPaid = request.amountPaid() == null ? BigDecimal.ZERO : request.amountPaid();
        if (initialPaid.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount paid cannot be negative");
        }
        if (initialPaid.compareTo(totalAmount) > 0) {
            throw new IllegalArgumentException("Amount paid cannot exceed purchase total");
        }

        purchase.setItems(items);
        purchase.setTotalAmount(totalAmount);

        Purchase savedPurchase = purchaseRepository.save(purchase);

        for (PurchaseItem item : savedPurchase.getItems()) {
            Product product = item.getProduct();
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.add(item.getQuantity());
            product.setCurrentStock(after);

            recordMovement(
                product,
                InventoryMovement.TYPE_PURCHASE,
                item.getQuantity(),
                before,
                after,
                "Purchase " + savedPurchase.getPurchaseNumber()
            );
        }

        if (initialPaid.compareTo(BigDecimal.ZERO) > 0) {
            paymentService.recordPurchasePayment(
                savedPurchase,
                initialPaid,
                request.purchaseDate(),
                "Initial payment"
            );
        }

        return toResponse(savedPurchase);
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> list(String search) {
        return purchaseRepository.search(requireBusinessId(), normalizeSearch(search))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse getById(UUID id) {
        return toResponse(findPurchase(id));
    }

    public PurchaseResponse update(UUID id, PurchaseUpdateRequest request) {
        Purchase purchase = findPurchase(id);

        if (purchase.isCancelled()) {
            throw new IllegalArgumentException("Cancelled purchases cannot be updated");
        }

        if (request.purchaseDate() != null) {
            purchase.setPurchaseDate(request.purchaseDate());
        }

        if (request.notes() != null) {
            purchase.setNotes(normalize(request.notes()));
        }

        return toResponse(purchase);
    }

    public PurchaseResponse cancel(UUID id, PurchaseCancellationRequest request) {
        Purchase purchase = findPurchase(id);

        if (purchase.isCancelled()) {
            throw new IllegalArgumentException("Purchase is already cancelled");
        }

        for (PurchaseItem item : purchase.getItems()) {
            Product product = item.getProduct();
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.subtract(item.getQuantity());

            if (after.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                    "Purchase cannot be cancelled because stock has already been consumed for " + product.getName()
                );
            }
        }

        for (PurchaseItem item : purchase.getItems()) {
            Product product = item.getProduct();
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.subtract(item.getQuantity());
            product.setCurrentStock(after);

            recordMovement(
                product,
                InventoryMovement.TYPE_PURCHASE_CANCELLATION,
                item.getQuantity().negate(),
                before,
                after,
                "Purchase cancellation " + purchase.getPurchaseNumber()
            );
        }

        purchase.setCancelled(true);
        purchase.setCancellationReason(request.reason().trim());
        return toResponse(purchase);
    }

    private Purchase findPurchase(UUID id) {
        return purchaseRepository.findByIdAndBusinessId(id, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
    }

    private Supplier findSupplier(UUID id, UUID businessId) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        if (supplier.isArchived()) {
            throw new IllegalArgumentException("Archived suppliers cannot be used");
        }

        return supplier;
    }

    private Product findProduct(UUID id, UUID businessId) {
        return productRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
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

    private String generatePurchaseNumber() {
        return "PUR-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

    private PurchaseResponse toResponse(Purchase purchase) {
        BigDecimal amountPaid = purchase.getAmountPaid() == null ? BigDecimal.ZERO : purchase.getAmountPaid();
        BigDecimal outstandingAmount = PaymentAmounts.outstanding(amountPaid, purchase.getTotalAmount());

        return new PurchaseResponse(
            purchase.getId(),
            purchase.getBusinessId(),
            purchase.getPurchaseNumber(),
            purchase.getSupplier().getId(),
            purchase.getSupplier().getName(),
            purchase.getPurchaseDate(),
            purchase.getPaymentStatus(),
            purchase.getNotes(),
            purchase.getTotalAmount(),
            amountPaid,
            outstandingAmount,
            purchase.isCancelled(),
            purchase.getCancellationReason(),
            purchase.getItems().stream()
                .map(item -> new PurchaseItemResponse(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getUnit(),
                    item.getQuantity(),
                    item.getUnitCost(),
                    item.getLineTotal()
                ))
                .toList(),
            purchase.getCreatedAt(),
            purchase.getUpdatedAt()
        );
    }
}
