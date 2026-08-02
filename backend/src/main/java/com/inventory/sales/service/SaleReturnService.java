package com.inventory.sales.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.inventory.entity.InventoryMovement;
import com.inventory.inventory.repository.InventoryMovementRepository;
import com.inventory.payment.service.PaymentService;
import com.inventory.product.entity.Product;
import com.inventory.sales.dto.SaleReturnItemRequest;
import com.inventory.sales.dto.SaleReturnItemResponse;
import com.inventory.sales.dto.SaleReturnRequest;
import com.inventory.sales.dto.SaleReturnResponse;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.entity.SaleItem;
import com.inventory.sales.entity.SaleReturn;
import com.inventory.sales.entity.SaleReturnItem;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
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
public class SaleReturnService {

    private final SaleReturnRepository saleReturnRepository;
    private final SaleRepository saleRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentService paymentService;

    public SaleReturnService(
        SaleReturnRepository saleReturnRepository,
        SaleRepository saleRepository,
        InventoryMovementRepository inventoryMovementRepository,
        PaymentService paymentService
    ) {
        this.saleReturnRepository = saleReturnRepository;
        this.saleRepository = saleRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentService = paymentService;
    }

    public SaleReturnResponse create(UUID saleId, SaleReturnRequest request) {
        UUID businessId = requireBusinessId();
        Sale sale = findSale(saleId, businessId);

        if (sale.isCancelled()) {
            throw new IllegalArgumentException("Cancelled sales cannot accept returns");
        }

        Map<UUID, SaleItem> saleItemsById = new HashMap<>();
        for (SaleItem item : sale.getItems()) {
            saleItemsById.put(item.getId(), item);
        }

        Set<UUID> requestedSaleItemIds = new HashSet<>();
        List<SaleReturnItem> returnItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        SaleReturn saleReturn = new SaleReturn();
        saleReturn.setBusinessId(businessId);
        saleReturn.setSale(sale);
        saleReturn.setReturnNumber(generateReturnNumber());
        saleReturn.setReturnDate(request.returnDate());
        saleReturn.setReason(normalize(request.reason()));
        saleReturn.setNotes(normalize(request.notes()));

        for (SaleReturnItemRequest itemRequest : request.items()) {
            if (!requestedSaleItemIds.add(itemRequest.saleItemId())) {
                throw new IllegalArgumentException("Each sale item can appear only once in a return");
            }

            SaleItem saleItem = saleItemsById.get(itemRequest.saleItemId());
            if (saleItem == null) {
                throw new IllegalArgumentException("Sale item does not belong to this sale");
            }

            BigDecimal alreadyReturned = saleReturnRepository.sumReturnedQuantityForSaleItem(
                businessId,
                saleItem.getId()
            );
            BigDecimal returnable = saleItem.getQuantity().subtract(alreadyReturned);
            if (itemRequest.quantity().compareTo(returnable) > 0) {
                throw new IllegalArgumentException(
                    "Return quantity exceeds returnable quantity for " + saleItem.getProduct().getName()
                );
            }

            BigDecimal lineTotal = itemRequest.quantity().multiply(saleItem.getUnitPrice());
            SaleReturnItem returnItem = new SaleReturnItem();
            returnItem.setSaleReturn(saleReturn);
            returnItem.setSaleItem(saleItem);
            returnItem.setProduct(saleItem.getProduct());
            returnItem.setQuantity(itemRequest.quantity());
            returnItem.setUnitPrice(saleItem.getUnitPrice());
            returnItem.setLineTotal(lineTotal);
            returnItems.add(returnItem);
            totalAmount = totalAmount.add(lineTotal);
        }

        saleReturn.setItems(returnItems);
        saleReturn.setTotalAmount(totalAmount);
        SaleReturn savedReturn = saleReturnRepository.save(saleReturn);

        for (SaleReturnItem returnItem : savedReturn.getItems()) {
            Product product = returnItem.getProduct();
            BigDecimal before = product.getCurrentStock();
            BigDecimal after = before.add(returnItem.getQuantity());
            product.setCurrentStock(after);
            recordMovement(
                product,
                InventoryMovement.TYPE_SALE_RETURN,
                returnItem.getQuantity(),
                before,
                after,
                "Sale return " + savedReturn.getReturnNumber() + " for " + sale.getSaleNumber()
            );
        }

        paymentService.syncSalePaymentStatus(sale);

        return toResponse(savedReturn);
    }

    @Transactional(readOnly = true)
    public List<SaleReturnResponse> listForSale(UUID saleId) {
        UUID businessId = requireBusinessId();
        findSale(saleId, businessId);
        return saleReturnRepository
            .findByBusinessIdAndSale_IdOrderByReturnDateDescCreatedAtDesc(businessId, saleId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<SaleReturnResponse> list(String search) {
        return saleReturnRepository.search(requireBusinessId(), normalizeSearch(search))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SaleReturnResponse getById(UUID returnId) {
        return toResponse(findReturn(returnId));
    }

    private Sale findSale(UUID saleId, UUID businessId) {
        return saleRepository.findByIdAndBusinessId(saleId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
    }

    private SaleReturn findReturn(UUID returnId) {
        return saleReturnRepository.findByIdAndBusinessId(returnId, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Sale return not found"));
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
        return "RET-" + LocalDate.now() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
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

    private SaleReturnResponse toResponse(SaleReturn saleReturn) {
        Sale sale = saleReturn.getSale();
        return new SaleReturnResponse(
            saleReturn.getId(),
            saleReturn.getBusinessId(),
            sale.getId(),
            sale.getSaleNumber(),
            sale.getCustomer().getName(),
            saleReturn.getReturnNumber(),
            saleReturn.getReturnDate(),
            saleReturn.getReason(),
            saleReturn.getNotes(),
            saleReturn.getTotalAmount(),
            saleReturn.getItems().stream().map(item -> new SaleReturnItemResponse(
                item.getId(),
                item.getSaleItem().getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getProduct().getUnit(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal()
            )).toList(),
            saleReturn.getCreatedAt(),
            saleReturn.getUpdatedAt()
        );
    }
}
