package com.inventory.purchase.service;

import com.inventory.audit.service.AuditService;
import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.common.tenant.TenantContext;
import com.inventory.common.time.BusinessClock;
import com.inventory.document.numbering.DocumentNumberService;
import com.inventory.document.numbering.DocumentType;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.payment.service.PaymentService;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.product.entity.Product;
import com.inventory.product.repository.ProductRepository;
import com.inventory.product.support.ProductCosting;
import com.inventory.purchase.dto.PurchaseCancellationRequest;
import com.inventory.purchase.dto.PurchaseItemRequest;
import com.inventory.purchase.dto.PurchaseItemResponse;
import com.inventory.purchase.dto.PurchaseRequest;
import com.inventory.purchase.dto.PurchaseResponse;
import com.inventory.purchase.dto.PurchaseUpdateRequest;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.entity.PurchaseItem;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.settings.service.SettingsService;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.repository.SupplierRepository;
import com.inventory.tax.GstCalculator;
import com.inventory.tax.GstLine;
import com.inventory.tax.GstPlaceOfSupply;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final DocumentNumberService documentNumberService;
    private final BusinessClock businessClock;

    public PurchaseService(
        PurchaseRepository purchaseRepository,
        PurchaseReturnRepository purchaseReturnRepository,
        SupplierRepository supplierRepository,
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService,
        SettingsService settingsService,
        AuditService auditService,
        DocumentNumberService documentNumberService,
        BusinessClock businessClock
    ) {
        this.purchaseRepository = purchaseRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.documentNumberService = documentNumberService;
        this.businessClock = businessClock;
    }

    public PurchaseResponse create(PurchaseRequest request) {
        UUID businessId = requireBusinessId();
        Supplier supplier = findSupplier(request.supplierId(), businessId);

        Map<UUID, BigDecimal> receivedByProduct = new LinkedHashMap<>();
        for (PurchaseItemRequest itemRequest : request.items()) {
            receivedByProduct.merge(itemRequest.productId(), itemRequest.quantity(), BigDecimal::add);
        }
        Map<UUID, Product> lockedProducts = lockProductsForUpdate(businessId, receivedByProduct.keySet());
        for (Product product : lockedProducts.values()) {
            if (product.isArchived()) {
                throw new IllegalArgumentException("Archived products cannot be purchased");
            }
        }

        Purchase purchase = new Purchase();
        purchase.setBusinessId(businessId);
        purchase.setPurchaseNumber(documentNumberService.next(businessId, DocumentType.PURCHASE));
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(request.purchaseDate());
        purchase.setAmountPaid(BigDecimal.ZERO);
        purchase.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
        purchase.setNotes(normalize(request.notes()));
        purchase.setCancelled(false);
        boolean interstate = GstPlaceOfSupply.isInterstate(
            settingsService.getStateCode(),
            supplier.getStateCode()
        );
        purchase.setInterstate(interstate);
        boolean gstEnabled = settingsService.isGstEnabled();
        boolean inclusive = settingsService.isGstInclusivePricing();

        List<PurchaseItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxableTotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;

        for (PurchaseItemRequest itemRequest : request.items()) {
            Product product = lockedProducts.get(itemRequest.productId());

            BigDecimal rate = gstEnabled
                ? GstCalculator.normalizeRate(itemRequest.gstRate() != null ? itemRequest.gstRate() : product.getGstRate())
                : BigDecimal.ZERO;
            GstLine tax = GstCalculator.compute(
                itemRequest.quantity(),
                itemRequest.purchasePrice(),
                rate,
                inclusive,
                interstate
            );

            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnit(product.getUnit());
            item.setQuantity(itemRequest.quantity());
            item.setUnitCost(itemRequest.purchasePrice());
            item.setHsnCode(product.getHsnCode());
            item.setGstRate(tax.gstRate());
            item.setTaxableAmount(tax.taxableAmount());
            item.setCgstAmount(tax.cgstAmount());
            item.setSgstAmount(tax.sgstAmount());
            item.setIgstAmount(tax.igstAmount());
            item.setLineTotal(tax.lineTotal());
            items.add(item);
            totalAmount = totalAmount.add(item.getLineTotal());
            taxableTotal = taxableTotal.add(tax.taxableAmount());
            cgstTotal = cgstTotal.add(tax.cgstAmount());
            sgstTotal = sgstTotal.add(tax.sgstAmount());
            igstTotal = igstTotal.add(tax.igstAmount());
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
        purchase.setTaxableAmount(taxableTotal);
        purchase.setCgstAmount(cgstTotal);
        purchase.setSgstAmount(sgstTotal);
        purchase.setIgstAmount(igstTotal);

        Purchase savedPurchase = purchaseRepository.save(purchase);

        Map<UUID, BigDecimal> stockBefore = new LinkedHashMap<>();
        Map<UUID, BigDecimal> inboundQty = new LinkedHashMap<>();
        Map<UUID, BigDecimal> inboundValue = new LinkedHashMap<>();

        for (PurchaseItem item : savedPurchase.getItems()) {
            Product product = item.getProduct();
            stockBefore.putIfAbsent(product.getId(), product.getCurrentStock());
            inboundQty.merge(product.getId(), item.getQuantity(), BigDecimal::add);
            inboundValue.merge(
                product.getId(),
                item.getQuantity().multiply(item.getUnitCost()),
                BigDecimal::add
            );

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

        for (Map.Entry<UUID, BigDecimal> inbound : inboundQty.entrySet()) {
            Product product = lockedProducts.get(inbound.getKey());
            product.setCostPrice(ProductCosting.weightedAverage(
                stockBefore.get(inbound.getKey()),
                product.getCostPrice(),
                inbound.getValue(),
                inboundValue.get(inbound.getKey())
            ));
        }

        if (initialPaid.compareTo(BigDecimal.ZERO) > 0) {
            paymentService.recordPurchasePayment(
                savedPurchase,
                initialPaid,
                request.purchaseDate(),
                "Initial payment"
            );
        }

        auditService.record(
            "PURCHASE_CREATED",
            "PURCHASE",
            savedPurchase.getId(),
            "Purchase " + savedPurchase.getPurchaseNumber() + " from " + supplier.getName()
        );
        return toResponse(savedPurchase);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PurchaseResponse> list(String search, Integer page, Integer size) {
        return Pagination.map(
            purchaseRepository.search(
                requireBusinessId(),
                normalizeSearch(search),
                Pagination.pageable(page, size)
            ),
            this::toResponse
        );
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> listBySupplier(UUID supplierId) {
        UUID businessId = requireBusinessId();
        supplierRepository.findByIdAndBusinessId(supplierId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        return purchaseRepository.findByBusinessIdAndSupplierIdOrderByPurchaseDateDescCreatedAtDesc(businessId, supplierId)
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
        UUID businessId = requireBusinessId();
        Purchase purchase = findPurchaseForUpdate(id, businessId);

        if (purchase.isCancelled()) {
            throw new IllegalArgumentException("Purchase is already cancelled");
        }

        if (purchaseReturnRepository.existsByBusinessIdAndPurchase_Id(purchase.getBusinessId(), purchase.getId())) {
            throw new IllegalArgumentException("Purchases with returns cannot be cancelled");
        }

        Map<UUID, BigDecimal> removeByProduct = new LinkedHashMap<>();
        for (PurchaseItem item : purchase.getItems()) {
            removeByProduct.merge(item.getProduct().getId(), item.getQuantity(), BigDecimal::add);
        }
        Map<UUID, Product> lockedProducts = lockProductsForUpdate(businessId, removeByProduct.keySet());
        boolean allowNegativeStock = settingsService.isNegativeStockAllowed();

        for (Map.Entry<UUID, BigDecimal> removal : removeByProduct.entrySet()) {
            Product product = lockedProducts.get(removal.getKey());
            BigDecimal after = product.getCurrentStock().subtract(removal.getValue());
            if (after.compareTo(BigDecimal.ZERO) < 0 && !allowNegativeStock) {
                throw new IllegalArgumentException(
                    "Purchase cannot be cancelled because stock has already been consumed for " + product.getName()
                );
            }
        }

        for (PurchaseItem item : purchase.getItems()) {
            Product product = lockedProducts.get(item.getProduct().getId());
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
        paymentService.reversePaymentsForCancelledPurchase(
            purchase,
            businessClock.today(),
            "Refund on cancellation of " + purchase.getPurchaseNumber()
        );
        auditService.record(
            "PURCHASE_CANCELLED",
            "PURCHASE",
            purchase.getId(),
            "Cancelled purchase " + purchase.getPurchaseNumber()
        );
        return toResponse(purchase);
    }

    private Purchase findPurchase(UUID id) {
        return purchaseRepository.findByIdAndBusinessId(id, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
    }

    private Purchase findPurchaseForUpdate(UUID id, UUID businessId) {
        return purchaseRepository.findByIdAndBusinessIdForUpdate(id, businessId)
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

    private Supplier findSupplier(UUID id, UUID businessId) {
        Supplier supplier = supplierRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));

        if (supplier.isArchived()) {
            throw new IllegalArgumentException("Archived suppliers cannot be used");
        }

        return supplier;
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

    private PurchaseResponse toResponse(Purchase purchase) {
        UUID businessId = purchase.getBusinessId();
        BigDecimal returnedAmount = purchaseReturnRepository.sumReturnedAmountForPurchase(businessId, purchase.getId());
        boolean hasReturns = returnedAmount.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal netAmount = purchase.getTotalAmount().subtract(returnedAmount);
        BigDecimal amountPaid = purchase.getAmountPaid() == null ? BigDecimal.ZERO : purchase.getAmountPaid();
        BigDecimal outstandingAmount = PaymentAmounts.outstanding(amountPaid, netAmount);

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
            returnedAmount,
            netAmount,
            amountPaid,
            outstandingAmount,
            purchase.isCancelled(),
            purchase.getCancellationReason(),
            hasReturns,
            purchase.isInterstate(),
            purchase.getTaxableAmount(),
            purchase.getCgstAmount(),
            purchase.getSgstAmount(),
            purchase.getIgstAmount(),
            purchase.getItems().stream()
                .map(item -> {
                    BigDecimal returnedQuantity = purchaseReturnRepository.sumReturnedQuantityForPurchaseItem(
                        businessId,
                        item.getId()
                    );
                    return new PurchaseItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProductName(),
                        item.getUnit(),
                        item.getQuantity(),
                        item.getUnitCost(),
                        item.getLineTotal(),
                        item.getHsnCode(),
                        item.getGstRate(),
                        item.getTaxableAmount(),
                        item.getCgstAmount(),
                        item.getSgstAmount(),
                        item.getIgstAmount(),
                        returnedQuantity,
                        item.getQuantity().subtract(returnedQuantity)
                    );
                })
                .toList(),
            purchase.getCreatedAt(),
            purchase.getUpdatedAt()
        );
    }
}
