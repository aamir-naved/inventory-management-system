package com.inventory.sales.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.repository.CustomerRepository;
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
public class SaleService {
    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;
    private final SettingsService settingsService;

    public SaleService(
        SaleRepository saleRepository,
        SaleReturnRepository saleReturnRepository,
        CustomerRepository customerRepository,
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService,
        SettingsService settingsService
    ) {
        this.saleRepository = saleRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
        this.settingsService = settingsService;
    }

    public SaleResponse create(SaleRequest request) {
        UUID businessId = requireBusinessId();
        Customer customer = findCustomer(request.customerId(), businessId);

        Sale sale = new Sale();
        sale.setBusinessId(businessId);
        sale.setSaleNumber(generateSaleNumber());
        sale.setCustomer(customer);
        sale.setSaleDate(request.saleDate());
        sale.setAmountPaid(BigDecimal.ZERO);
        sale.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
        sale.setNotes(normalize(request.notes()));
        sale.setCancelled(false);

        List<SaleItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (SaleItemRequest itemRequest : request.items()) {
            Product product = findProduct(itemRequest.productId(), businessId);
            if (product.isArchived()) {
                throw new IllegalArgumentException("Archived products cannot be sold");
            }
            BigDecimal after = product.getCurrentStock().subtract(itemRequest.quantity());
            if (after.compareTo(BigDecimal.ZERO) < 0 && !settingsService.isNegativeStockAllowed()) {
                throw new IllegalArgumentException("Stock cannot go below zero for " + product.getName());
            }

            SaleItem item = new SaleItem();
            item.setSale(sale);
            item.setProduct(product);
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(itemRequest.sellingPrice());
            item.setLineTotal(itemRequest.quantity().multiply(itemRequest.sellingPrice()));
            items.add(item);
            totalAmount = totalAmount.add(item.getLineTotal());
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

        return toResponse(savedSale);
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> list(String search) {
        return saleRepository.search(requireBusinessId(), normalizeSearch(search)).stream().map(this::toResponse).toList();
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
        Sale sale = findSale(id);
        if (sale.isCancelled()) {
            throw new IllegalArgumentException("Sale is already cancelled");
        }
        if (saleReturnRepository.existsByBusinessIdAndSale_Id(sale.getBusinessId(), sale.getId())) {
            throw new IllegalArgumentException("Sales with returns cannot be cancelled");
        }

        for (SaleItem item : sale.getItems()) {
            Product product = item.getProduct();
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
        return toResponse(sale);
    }

    private Sale findSale(UUID id) {
        return saleRepository.findByIdAndBusinessId(id, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
    }

    private Customer findCustomer(UUID id, UUID businessId) {
        Customer customer = customerRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
        if (customer.isArchived()) {
            throw new IllegalArgumentException("Archived customers cannot be used");
        }
        return customer;
    }

    private Product findProduct(UUID id, UUID businessId) {
        return productRepository.findByIdAndBusinessId(id, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Product not found"));
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

    private String generateSaleNumber() {
        return "SAL-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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
            sale.getItems().stream().map(item -> {
                BigDecimal returnedQuantity = saleReturnRepository.sumReturnedQuantityForSaleItem(
                    businessId,
                    item.getId()
                );
                return new SaleItemResponse(
                    item.getId(),
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getProduct().getUnit(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal(),
                    returnedQuantity,
                    item.getQuantity().subtract(returnedQuantity)
                );
            }).toList(),
            sale.getCreatedAt(),
            sale.getUpdatedAt()
        );
    }
}
