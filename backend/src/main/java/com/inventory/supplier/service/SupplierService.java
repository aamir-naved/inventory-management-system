package com.inventory.supplier.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.supplier.dto.SupplierRequest;
import com.inventory.supplier.dto.SupplierResponse;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.repository.SupplierRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
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
