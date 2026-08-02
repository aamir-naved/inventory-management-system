package com.inventory.purchase.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.payment.service.PaymentService;
import com.inventory.product.entity.Product;
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
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;
    private final SettingsService settingsService;

    public PurchaseReturnService(
        PurchaseReturnRepository purchaseReturnRepository,
        PurchaseRepository purchaseRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService,
        SettingsService settingsService
    ) {
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.purchaseRepository = purchaseRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
        this.settingsService = settingsService;
    }

    public PurchaseReturnResponse create(UUID purchaseId, PurchaseReturnRequest request) {
        UUID businessId = requireBusinessId();
        Purchase purchase = findPurchase(purchaseId, businessId);

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

        PurchaseReturn purchaseReturn = new PurchaseReturn();
        purchaseReturn.setBusinessId(businessId);
        purchaseReturn.setPurchase(purchase);
        purchaseReturn.setReturnNumber(generateReturnNumber());
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

            Product product = purchaseItem.getProduct();
            BigDecimal projectedStock = product.getCurrentStock().subtract(itemRequest.quantity());
            if (projectedStock.compareTo(BigDecimal.ZERO) < 0 && !settingsService.isNegativeStockAllowed()) {
                throw new IllegalArgumentException(
                    "Purchase return would make stock negative for " + product.getName()
                );
            }

            BigDecimal lineTotal = itemRequest.quantity().multiply(purchaseItem.getUnitCost());
            PurchaseReturnItem returnItem = new PurchaseReturnItem();
            returnItem.setPurchaseReturn(purchaseReturn);
            returnItem.setPurchaseItem(purchaseItem);
            returnItem.setProduct(product);
            returnItem.setQuantity(itemRequest.quantity());
            returnItem.setUnitCost(purchaseItem.getUnitCost());
            returnItem.setLineTotal(lineTotal);
            returnItems.add(returnItem);
            totalAmount = totalAmount.add(lineTotal);
        }

        purchaseReturn.setItems(returnItems);
        purchaseReturn.setTotalAmount(totalAmount);
        PurchaseReturn savedReturn = purchaseReturnRepository.save(purchaseReturn);

        for (PurchaseReturnItem returnItem : savedReturn.getItems()) {
            Product product = returnItem.getProduct();
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

    private String generateReturnNumber() {
        return "PRT-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
                item.getProduct().getName(),
                item.getProduct().getUnit(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal()
            )).toList(),
            purchaseReturn.getCreatedAt(),
            purchaseReturn.getUpdatedAt()
        );
    }
}
