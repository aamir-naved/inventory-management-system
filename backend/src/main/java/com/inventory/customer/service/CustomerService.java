package com.inventory.customer.service;

import com.inventory.common.api.PagedResponse;
import com.inventory.common.api.Pagination;
import com.inventory.common.tenant.TenantContext;
import com.inventory.customer.dto.CustomerRequest;
import com.inventory.customer.dto.CustomerResponse;
import com.inventory.customer.dto.CustomerSummaryResponse;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.repository.CustomerRepository;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
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
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;

    public CustomerService(
        CustomerRepository customerRepository,
        SaleRepository saleRepository,
        SaleReturnRepository saleReturnRepository
    ) {
        this.customerRepository = customerRepository;
        this.saleRepository = saleRepository;
        this.saleReturnRepository = saleReturnRepository;
    }

    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setBusinessId(requireBusinessId());
        applyRequest(customer, request);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> list(
        String search,
        boolean includeArchived,
        Integer page,
        Integer size
    ) {
        return Pagination.map(
            customerRepository.search(
                requireBusinessId(),
                normalizeSearch(search),
                includeArchived,
                Pagination.pageable(page, size)
            ),
            this::toResponse
        );
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(UUID id) {
        return toResponse(findCustomer(id));
    }

    @Transactional(readOnly = true)
    public CustomerSummaryResponse getSummary(UUID id) {
        Customer customer = findCustomer(id);
        UUID businessId = customer.getBusinessId();
        List<Sale> sales = saleRepository.findByBusinessIdAndCustomerIdOrderBySaleDateDescCreatedAtDesc(
            businessId,
            customer.getId()
        );
        Map<UUID, BigDecimal> returnedBySaleId = returnedAmountsBySale(businessId);

        long invoiceCount = 0;
        BigDecimal netBilled = BigDecimal.ZERO;
        BigDecimal amountPaidTotal = BigDecimal.ZERO;
        BigDecimal outstandingTotal = BigDecimal.ZERO;

        for (Sale sale : sales) {
            if (sale.isCancelled()) {
                continue;
            }

            BigDecimal returned = returnedBySaleId.getOrDefault(sale.getId(), BigDecimal.ZERO);
            BigDecimal netAmount = sale.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = sale.getAmountPaid() == null ? BigDecimal.ZERO : sale.getAmountPaid();
            BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, netAmount);

            invoiceCount++;
            netBilled = netBilled.add(netAmount);
            amountPaidTotal = amountPaidTotal.add(amountPaid);
            outstandingTotal = outstandingTotal.add(outstanding);
        }

        return new CustomerSummaryResponse(
            customer.getId(),
            customer.getBusinessId(),
            customer.getName(),
            customer.getContactPerson(),
            customer.getMobileNumber(),
            customer.getAddressLine(),
            customer.getStateCode(),
            customer.isArchived(),
            invoiceCount,
            netBilled,
            amountPaidTotal,
            outstandingTotal,
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
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

    public CustomerResponse unarchive(UUID id) {
        Customer customer = findCustomer(id);
        customer.setArchived(false);
        return toResponse(customer);
    }

    private Map<UUID, BigDecimal> returnedAmountsBySale(UUID businessId) {
        Map<UUID, BigDecimal> returnedBySaleId = new HashMap<>();
        for (Object[] row : saleReturnRepository.sumReturnedAmountsGroupedBySale(businessId)) {
            returnedBySaleId.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return returnedBySaleId;
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
        customer.setStateCode(normalizeStateCode(request.stateCode()));
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

    private String normalizeStateCode(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
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
            customer.getStateCode(),
            customer.isArchived(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
