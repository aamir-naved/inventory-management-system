package com.inventory.supplier.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.supplier.dto.SupplierRequest;
import com.inventory.supplier.dto.SupplierResponse;
import com.inventory.supplier.dto.SupplierSummaryResponse;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;

    public SupplierService(
        SupplierRepository supplierRepository,
        PurchaseRepository purchaseRepository,
        PurchaseReturnRepository purchaseReturnRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
    }

    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setBusinessId(requireBusinessId());
        applyRequest(supplier, request);
        return toResponse(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> list(String search, boolean includeArchived) {
        return supplierRepository.search(requireBusinessId(), normalizeSearch(search), includeArchived)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(UUID id) {
        return toResponse(findSupplier(id));
    }

    @Transactional(readOnly = true)
    public SupplierSummaryResponse getSummary(UUID id) {
        Supplier supplier = findSupplier(id);
        List<Purchase> purchases = purchaseRepository.findByBusinessIdAndSupplierIdOrderByPurchaseDateDescCreatedAtDesc(
            supplier.getBusinessId(),
            supplier.getId()
        );
        Map<UUID, BigDecimal> returnedByPurchaseId = returnedAmountsByPurchase(supplier.getBusinessId());

        long billCount = 0;
        BigDecimal billedAmount = BigDecimal.ZERO;
        BigDecimal amountPaidTotal = BigDecimal.ZERO;
        BigDecimal outstandingTotal = BigDecimal.ZERO;

        for (Purchase purchase : purchases) {
            if (purchase.isCancelled()) {
                continue;
            }

            BigDecimal returned = returnedByPurchaseId.getOrDefault(purchase.getId(), BigDecimal.ZERO);
            BigDecimal billed = purchase.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = purchase.getAmountPaid() == null ? BigDecimal.ZERO : purchase.getAmountPaid();
            BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, billed);

            billCount++;
            billedAmount = billedAmount.add(billed);
            amountPaidTotal = amountPaidTotal.add(amountPaid);
            outstandingTotal = outstandingTotal.add(outstanding);
        }

        return new SupplierSummaryResponse(
            supplier.getId(),
            supplier.getBusinessId(),
            supplier.getName(),
            supplier.getContactPerson(),
            supplier.getMobileNumber(),
            supplier.getAddressLine(),
            supplier.isArchived(),
            billCount,
            billedAmount,
            amountPaidTotal,
            outstandingTotal,
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }

    public SupplierResponse update(UUID id, SupplierRequest request) {
        Supplier supplier = findSupplier(id);
        applyRequest(supplier, request);
        return toResponse(supplier);
    }

    public SupplierResponse archive(UUID id) {
        Supplier supplier = findSupplier(id);
        supplier.setArchived(true);
        return toResponse(supplier);
    }

    private Supplier findSupplier(UUID id) {
        return supplierRepository.findByIdAndBusinessId(id, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
    }

    private void applyRequest(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setContactPerson(normalize(request.contactPerson()));
        supplier.setMobileNumber(normalize(request.mobileNumber()));
        supplier.setAddressLine(normalize(request.addressLine()));
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

    private UUID requireBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
    }

    private Map<UUID, BigDecimal> returnedAmountsByPurchase(UUID businessId) {
        Map<UUID, BigDecimal> returnedByPurchaseId = new HashMap<>();
        for (Object[] row : purchaseReturnRepository.sumReturnedAmountsGroupedByPurchase(businessId)) {
            returnedByPurchaseId.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return returnedByPurchaseId;
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
            supplier.getId(),
            supplier.getBusinessId(),
            supplier.getName(),
            supplier.getContactPerson(),
            supplier.getMobileNumber(),
            supplier.getAddressLine(),
            supplier.isArchived(),
            supplier.getCreatedAt(),
            supplier.getUpdatedAt()
        );
    }
}
