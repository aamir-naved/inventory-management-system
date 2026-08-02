package com.inventory.customer.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.customer.dto.CustomerRequest;
import com.inventory.customer.dto.CustomerResponse;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.repository.CustomerRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setBusinessId(requireBusinessId());
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> list(String search, boolean includeArchived) {
        return customerRepository.search(requireBusinessId(), normalizeSearch(search), includeArchived)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return toResponse(findCustomer(id));
    }

    public CustomerResponse update(UUID id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        applyRequest(customer, request);
        return toResponse(customer);
    }

    public CustomerResponse archive(UUID id) {
        Customer customer = findCustomer(id);
        customer.setArchived(true);
        return toResponse(customer);
    }

    private Customer findCustomer(UUID id) {
        return customerRepository.findByIdAndBusinessId(id, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Customer not found"));
    }

    private void applyRequest(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setContactPerson(normalize(request.contactPerson()));
        customer.setMobileNumber(normalize(request.mobileNumber()));
        customer.setAddressLine(normalize(request.addressLine()));
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

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getBusinessId(),
            customer.getName(),
            customer.getContactPerson(),
            customer.getMobileNumber(),
            customer.getAddressLine(),
            customer.isArchived(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
