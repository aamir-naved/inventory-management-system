package com.inventory.sales.service;

import com.inventory.audit.service.AuditService;
import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.common.tenant.TenantContext;
import com.inventory.common.time.BusinessClock;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.repository.CustomerRepository;
import com.inventory.document.numbering.DocumentNumberService;
import com.inventory.document.numbering.DocumentType;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.payment.service.PaymentService;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.product.entity.Product;
import com.inventory.product.repository.ProductRepository;
import com.inventory.sales.dto.SaleCancellationRequest;
import com.inventory.sales.dto.SaleItemRequest;
import com.inventory.sales.dto.SaleItemResponse;
import com.inventory.sales.dto.SaleRequest;
import com.inventory.sales.dto.SaleResponse;
import com.inventory.sales.dto.SaleUpdateRequest;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.entity.SaleItem;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
import com.inventory.settings.service.SettingsService;
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
public class SaleService {
    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final DocumentNumberService documentNumberService;
    private final BusinessClock businessClock;

    public SaleService(
        SaleRepository saleRepository,
        SaleReturnRepository saleReturnRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService,
        SettingsService settingsService,
        AuditService auditService,
        DocumentNumberService documentNumberService,
        BusinessClock businessClock
    ) {
        this.saleRepository = saleRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.documentNumberService = documentNumberService;
        this.businessClock = businessClock;
    }

    public SaleResponse create(SaleRequest request) {
        UUID businessId = requireBusinessId();
        Customer customer = findCustomer(request.customerId(), businessId);

        Map<UUID, BigDecimal> demandedByProduct = aggregateRequestedQuantities(request.items());
        Map<UUID, Product> lockedProducts = lockProductsForUpdate(businessId, demandedByProduct.keySet());
        boolean allowNegativeStock = settingsService.isNegativeStockAllowed();
        for (Map.Entry<UUID, BigDecimal> demand : demandedByProduct.entrySet()) {
            Product product = lockedProducts.get(demand.getKey());
            if (product.isArchived()) {
                throw new IllegalArgumentException("Archived products cannot be sold");
            }
            BigDecimal after = product.getCurrentStock().subtract(demand.getValue());
            if (after.compareTo(BigDecimal.ZERO) < 0 && !allowNegativeStock) {
                throw new IllegalArgumentException("Stock cannot go below zero for " + product.getName());
            }
        }

        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setSaleNumber(documentNumberService.next(businessId, DocumentType.SALE));
        sale.setCustomer(customer);
        sale.setSaleDate(request.saleDate());
        sale.setAmountPaid(BigDecimal.ZERO);
        sale.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
        sale.setNotes(normalize(request.notes()));
        sale.setCancelled(false);
        boolean interstate = GstPlaceOfSupply.isInterstate(
            settingsService.getStateCode(),
            customer.getStateCode()
        );
        sale.setInterstate(interstate);
        boolean gstEnabled = settingsService.isGstEnabled();
        boolean inclusive = settingsService.isGstInclusivePricing();

        List<SaleItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal taxableTotal = BigDecimal.ZERO;
        BigDecimal cgstTotal = BigDecimal.ZERO;
        BigDecimal sgstTotal = BigDecimal.ZERO;
        BigDecimal igstTotal = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            Product product = lockedProducts.get(itemRequest.productId());

            BigDecimal rate = gstEnabled
                ? GstCalculator.normalizeRate(itemRequest.gstRate() != null ? itemRequest.gstRate() : product.getGstRate())
                : BigDecimal.ZERO;
            GstLine tax = GstCalculator.compute(
                itemRequest.quantity(),
                itemRequest.sellingPrice(),
                rate,
                inclusive,
                interstate
            );

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setUnit(product.getUnit());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.sellingPrice());
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
            throw new IllegalArgumentException("Amount paid cannot exceed sale total");
        }

        sale.setItems(items);
        sale.setTotalAmount(totalAmount);
        sale.setTaxableAmount(taxableTotal);
        sale.setCgstAmount(cgstTotal);
        sale.setSgstAmount(sgstTotal);
        sale.setIgstAmount(igstTotal);
        Sale savedSale = saleRepository.save(sale);

        for (SaleItem item : savedSale.getItems()) {
            Product product = item.getProduct();
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.subtract(item.getQuantity());
            product.setCurrentStock(after);
            recordMovement(
                product,
                InventoryMovement.TYPE_SALE,
                item.getQuantity().negate(),
                before,
                after,
                "Sale " + savedSale.getSaleNumber()
            );
        }

        if (initialPaid.compareTo(BigDecimal.ZERO) > 0) {
            paymentService.recordSalePayment(
                savedSale,
                initialPaid,
                request.saleDate(),
                "Initial payment"
            );
        }

        auditService.record(
            "SALE_CREATED",
            "SALE",
            savedSale.getId(),
            "Sale " + savedSale.getSaleNumber() + " for " + customer.getName()
        );
        return toResponse(savedSale);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SaleResponse> list(String search, Integer page, Integer size) {
        return Pagination.map(
            saleRepository.search(requireBusinessId(), normalizeSearch(search), Pagination.pageable(page, size)),
            this::toResponse
        );
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> listByCustomer(UUID customerId) {
        UUID businessId = requireBusinessId();
        customerRepository.findByIdAndBusinessId(customerId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        return saleRepository.findByBusinessIdAndCustomerIdOrderBySaleDateDescCreatedAtDesc(businessId, customerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse getById(UUID id) {
        return toResponse(findSale(id));
    }

    public SaleResponse update(UUID id, SaleUpdateRequest request) {
        Sale sale = findSale(id);
        if (sale.isCancelled()) {
            throw new IllegalArgumentException("Cancelled sales cannot be updated");
        }
        if (request.saleDate() != null) {
            sale.setSaleDate(request.saleDate());
        }
        if (request.notes() != null) {
            sale.setNotes(normalize(request.notes()));
        }
        return toResponse(sale);
    }

    public SaleResponse cancel(UUID id, SaleCancellationRequest request) {
        UUID businessId = requireBusinessId();
        Sale sale = findSaleForUpdate(id, businessId);
        if (sale.isCancelled()) {
            throw new IllegalArgumentException("Sale is already cancelled");
        }
        if (saleReturnRepository.existsByBusinessIdAndSale_Id(sale.getBusinessId(), sale.getId())) {
            throw new IllegalArgumentException("Sales with returns cannot be cancelled");
        }

        Map<UUID, BigDecimal> restoreByProduct = new LinkedHashMap<>();
        for (SaleItem item : sale.getItems()) {
            restoreByProduct.merge(item.getProduct().getId(), item.getQuantity(), BigDecimal::add);
        }
        Map<UUID, Product> lockedProducts = lockProductsForUpdate(businessId, restoreByProduct.keySet());

        for (SaleItem item : sale.getItems()) {
            Product product = lockedProducts.get(item.getProduct().getId());
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.add(item.getQuantity());
            product.setCurrentStock(after);
            recordMovement(
                product,
                InventoryMovement.TYPE_SALE_CANCELLATION,
                item.getQuantity(),
                before,
                after,
                "Sale cancellation " + sale.getSaleNumber()
            );
        }

        sale.setCancelled(true);
        sale.setCancellationReason(request.reason().trim());
        paymentService.reversePaymentsForCancelledSale(
            sale,
            businessClock.today(),
            "Refund on cancellation of " + sale.getSaleNumber()
        );
        auditService.record(
            "SALE_CANCELLED",
            "SALE",
            sale.getId(),
            "Cancelled sale " + sale.getSaleNumber()
        );
        return toResponse(sale);
    }

    private Sale findSale(UUID id) {
        return saleRepository.findByIdAndBusinessId(id, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
    }

    private Sale findSaleForUpdate(UUID id, UUID businessId) {
        return saleRepository.findByIdAndBusinessIdForUpdate(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
    }

    private Map<UUID, BigDecimal> aggregateRequestedQuantities(List<SaleItemRequest> items) {
        Map<UUID, BigDecimal> demanded = new LinkedHashMap<>();
        for (SaleItemRequest item : items) {
            demanded.merge(item.productId(), item.quantity(), BigDecimal::add);
        }
        return demanded;
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

    private Customer findCustomer(UUID id, UUID businessId) {
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        if (customer.isArchived()) {
            throw new IllegalArgumentException("Archived customers cannot be used");
        }
        return customer;
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

    private SaleResponse toResponse(Sale sale) {
        UUID businessId = sale.getBusinessId();
        BigDecimal returnedAmount = saleReturnRepository.sumReturnedAmountForSale(businessId, sale.getId());
        boolean hasReturns = returnedAmount.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal netAmount = sale.getTotalAmount().subtract(returnedAmount);
        BigDecimal amountPaid = sale.getAmountPaid() == null ? BigDecimal.ZERO : sale.getAmountPaid();
        BigDecimal outstandingAmount = PaymentAmounts.outstanding(amountPaid, netAmount);

        return new SaleResponse(
            sale.getId(),
            sale.getBusinessId(),
            sale.getSaleNumber(),
            sale.getCustomer().getId(),
            sale.getCustomer().getName(),
            sale.getSaleDate(),
            sale.getPaymentStatus(),
            sale.getNotes(),
            sale.getTotalAmount(),
            returnedAmount,
            netAmount,
            amountPaid,
            outstandingAmount,
            sale.isCancelled(),
            sale.getCancellationReason(),
            hasReturns,
            sale.isInterstate(),
            sale.getTaxableAmount(),
            sale.getCgstAmount(),
            sale.getSgstAmount(),
            sale.getIgstAmount(),
            sale.getItems().stream().map(item -> {
                BigDecimal returnedQuantity = saleReturnRepository.sumReturnedQuantityForSaleItem(
                    businessId,
                    item.getId()
                );
                return new SaleItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProductName(),
                    item.getUnit(),
                    item.getQuantity(),
                    item.getUnitPrice(),
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
            }).toList(),
            sale.getCreatedAt(),
            sale.getUpdatedAt()
        );
    }
}
