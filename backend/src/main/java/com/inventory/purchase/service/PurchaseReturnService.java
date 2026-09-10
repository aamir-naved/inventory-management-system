package com.inventory.purchase.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.document.numbering.DocumentNumberService;
import com.inventory.document.numbering.DocumentType;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.payment.service.PaymentService;
import com.inventory.product.entity.Product;
import com.inventory.product.repository.ProductRepository;
import com.inventory.purchase.dto.PurchaseReturnItemRequest;
import com.inventory.purchase.dto.PurchaseReturnItemResponse;
import com.inventory.purchase.dto.PurchaseReturnRequest;
import com.inventory.purchase.dto.PurchaseReturnResponse;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.entity.PurchaseItem;
import com.inventory.purchase.entity.PurchaseReturn;
import com.inventory.purchase.entity.PurchaseReturnItem;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.settings.service.SettingsService;
import com.inventory.tax.GstCalculator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class PurchaseReturnService {

    private final PurchaseReturnRepository purchaseReturnRepository;
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;
    private final SettingsService settingsService;
    private final DocumentNumberService documentNumberService;

    public PurchaseReturnService(
        PurchaseReturnRepository purchaseReturnRepository,
        PurchaseRepository purchaseRepository,
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService,
        SettingsService settingsService,
        DocumentNumberService documentNumberService
    ) {
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.purchaseRepository = purchaseRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
        this.settingsService = settingsService;
        this.documentNumberService = documentNumberService;
    }

    public PurchaseReturnResponse create(UUID purchaseId, PurchaseReturnRequest request) {
        UUID businessId = requireBusinessId();
        Purchase purchase = findPurchaseForUpdate(purchaseId, businessId);

        if (purchase.isCancelled()) {
            throw new IllegalArgumentException("Cancelled purchases cannot accept returns");
        }

        Map<UUID, PurchaseItem> purchaseItemsById = new HashMap<>();
        for (PurchaseItem item : purchase.getItems()) {
            purchaseItemsById.put(item.getId(), item);
        }

        Set<UUID> requestedPurchaseItemIds = new HashSet<>();
        List<PurchaseReturnItem> returnItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<UUID, BigDecimal> removeByProduct = new HashMap<>();

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setBusinessId(businessId);
        purchaseReturn.setPurchase(purchase);
        purchaseReturn.setReturnNumber(documentNumberService.next(businessId, DocumentType.PURCHASE_RETURN));
        purchaseReturn.setReturnDate(request.returnDate());
        purchaseReturn.setReason(normalize(request.reason()));
        purchaseReturn.setNotes(normalize(request.notes()));

        for (PurchaseReturnItemRequest itemRequest : request.items()) {
            if (!requestedPurchaseItemIds.add(itemRequest.purchaseItemId())) {
                throw new IllegalArgumentException("Each purchase item can appear only once in a return");
            }

            PurchaseItem purchaseItem = purchaseItemsById.get(itemRequest.purchaseItemId());
            if (purchaseItem == null) {
                throw new IllegalArgumentException("Purchase item does not belong to this purchase");
            }

            BigDecimal alreadyReturned = purchaseReturnRepository.sumReturnedQuantityForPurchaseItem(
                businessId,
                purchaseItem.getId()
            );
            BigDecimal returnable = purchaseItem.getQuantity().subtract(alreadyReturned);
            if (itemRequest.quantity().compareTo(returnable) > 0) {
                throw new IllegalArgumentException(
                    "Return quantity exceeds returnable quantity for " + purchaseItem.getProduct().getName()
                );
            }

            removeByProduct.merge(purchaseItem.getProduct().getId(), itemRequest.quantity(), BigDecimal::add);

            BigDecimal alreadyCredited = purchaseReturnRepository.sumReturnedAmountForPurchaseItem(
                businessId,
                purchaseItem.getId()
            );
            BigDecimal lineTotal = GstCalculator.proportionalLineCredit(
                purchaseItem.getLineTotal(),
                purchaseItem.getQuantity(),
                itemRequest.quantity(),
                alreadyReturned,
                alreadyCredited
            );
            PurchaseReturnItem returnItem = new PurchaseReturnItem();
            returnItem.setPurchaseReturn(purchaseReturn);
            returnItem.setPurchaseItem(purchaseItem);
            returnItem.setProduct(purchaseItem.getProduct());
            returnItem.setProductName(purchaseItem.getProductName());
            returnItem.setUnit(purchaseItem.getUnit());
            returnItem.setQuantity(itemRequest.quantity());
            returnItem.setUnitCost(purchaseItem.getUnitCost());
            returnItem.setLineTotal(lineTotal);
            returnItems.add(returnItem);
            totalAmount = totalAmount.add(lineTotal);
        }

        Map<UUID, Product> lockedProducts = lockProductsForUpdate(businessId, removeByProduct.keySet());
        boolean allowNegativeStock = settingsService.isNegativeStockAllowed();
        for (Map.Entry<UUID, BigDecimal> removal : removeByProduct.entrySet()) {
            Product product = lockedProducts.get(removal.getKey());
            BigDecimal projectedStock = product.getCurrentStock().subtract(removal.getValue());
            if (projectedStock.compareTo(BigDecimal.ZERO) < 0 && !allowNegativeStock) {
                throw new IllegalArgumentException(
                    "Purchase return would make stock negative for " + product.getName()
                );
            }
        }

        purchaseReturn.setItems(returnItems);
        purchaseReturn.setTotalAmount(totalAmount);
        PurchaseReturn savedReturn = purchaseReturnRepository.save(purchaseReturn);

        for (PurchaseReturnItem returnItem : savedReturn.getItems()) {
            Product product = lockedProducts.get(returnItem.getProduct().getId());
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.subtract(returnItem.getQuantity());
            product.setCurrentStock(after);
            recordMovement(
                product,
                InventoryMovement.TYPE_PURCHASE_RETURN,
                returnItem.getQuantity().negate(),
                before,
                after,
                "Purchase return " + savedReturn.getReturnNumber() + " for " + purchase.getPurchaseNumber()
            );
        }

        paymentService.syncPurchasePaymentStatus(purchase);

        return toResponse(savedReturn);
    }

    @Transactional(readOnly = true)
    public List<PurchaseReturnResponse> listForPurchase(UUID purchaseId) {
        UUID businessId = requireBusinessId();
        findPurchase(purchaseId, businessId);
        return purchaseReturnRepository
            .findByBusinessIdAndPurchase_IdOrderByReturnDateDescCreatedAtDesc(businessId, purchaseId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseReturnResponse> list(String search) {
        return purchaseReturnRepository.search(requireBusinessId(), normalizeSearch(search))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseReturnResponse getById(UUID returnId) {
        return toResponse(findReturn(returnId));
    }

    private Purchase findPurchase(UUID purchaseId, UUID businessId) {
        return purchaseRepository.findByIdAndBusinessId(purchaseId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
    }

    private Purchase findPurchaseForUpdate(UUID purchaseId, UUID businessId) {
        return purchaseRepository.findByIdAndBusinessIdForUpdate(purchaseId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
    }

    private Map<UUID, Product> lockProductsForUpdate(UUID businessId, Iterable<UUID> productIds) {
        List<UUID> orderedIds = new ArrayList<>();
        for (UUID productId : productIds) {
            orderedIds.add(productId);
        }
        orderedIds.sort(Comparator.naturalOrder());

        Map<UUID, Product> locked = new HashMap<>();
        for (UUID productId : orderedIds) {
            Product product = productRepository.findByIdAndBusinessIdForUpdate(productId, businessId)
                .orElseThrow(() -> new EntityNotFoundException("Product not found"));
            locked.put(productId, product);
        }
        return locked;
    }

    private PurchaseReturn findReturn(UUID returnId) {
        return purchaseReturnRepository.findByIdAndBusinessId(returnId, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Purchase return not found"));
    }

    private void recordMovement(
        Product product,
        String type,
        BigDecimal delta,
        BigDecimal before,
        BigDecimal after,
        String notes
    ) {
        InventoryMovement movement = new InventoryMovement();
        movement.setBusinessId(product.getBusinessId());
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantityChange(delta);
        movement.setQuantityBefore(before);
        movement.setQuantityAfter(after);
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

    private PurchaseReturnResponse toResponse(PurchaseReturn purchaseReturn) {
        Purchase purchase = purchaseReturn.getPurchase();
        return new PurchaseReturnResponse(
            purchaseReturn.getId(),
            purchaseReturn.getBusinessId(),
            purchase.getId(),
            purchase.getPurchaseNumber(),
            purchase.getSupplier().getName(),
            purchaseReturn.getReturnNumber(),
            purchaseReturn.getReturnDate(),
            purchaseReturn.getReason(),
            purchaseReturn.getNotes(),
            purchaseReturn.getTotalAmount(),
            purchaseReturn.getItems().stream().map(item -> new PurchaseReturnItemResponse(
                item.getId(),
                item.getPurchaseItem().getId(),
                item.getProduct().getId(),
                item.getProductName(),
                item.getUnit(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal()
            )).toList(),
            purchaseReturn.getCreatedAt(),
            purchaseReturn.getUpdatedAt()
        );
    }
}
