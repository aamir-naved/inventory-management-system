package com.inventory.customer.controller;

import com.inventory.common.api.PagedResponse;
import com.inventory.customer.dto.CustomerRequest;
import com.inventory.customer.dto.CustomerResponse;
import com.inventory.customer.dto.CustomerSummaryResponse;
import com.inventory.customer.service.CustomerService;
import com.inventory.sales.dto.SaleResponse;
import com.inventory.sales.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final SaleService saleService;

    public CustomerController(CustomerService customerService, SaleService saleService) {
        this.customerService = customerService;
        this.saleService = saleService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse response = customerService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PagedResponse<CustomerResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean includeArchived,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return customerService.list(search, includeArchived, page, size);
    }

    @GetMapping("/{id}")
    public CustomerResponse getById(@PathVariable UUID id) {
        return customerService.getById(id);
    }

    @GetMapping("/{id}/summary")
    public CustomerSummaryResponse getSummary(@PathVariable UUID id) {
        return customerService.getSummary(id);
    }

    @GetMapping("/{id}/sales")
    public List<SaleResponse> listSales(@PathVariable UUID id) {
        return saleService.listByCustomer(id);
    }

    @PatchMapping("/{id}")
    public CustomerResponse update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public CustomerResponse archive(@PathVariable UUID id) {
        return customerService.archive(id);
    }

    @PatchMapping("/{id}/unarchive")
    public CustomerResponse unarchive(@PathVariable UUID id) {
        return customerService.unarchive(id);
    }
}
