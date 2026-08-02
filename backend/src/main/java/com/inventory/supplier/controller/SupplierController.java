package com.inventory.supplier.controller;

import com.inventory.purchase.dto.PurchaseResponse;
import com.inventory.purchase.service.PurchaseService;
import com.inventory.supplier.dto.SupplierRequest;
import com.inventory.supplier.dto.SupplierResponse;
import com.inventory.supplier.dto.SupplierSummaryResponse;
import com.inventory.supplier.service.SupplierService;
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
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;
    private final PurchaseService purchaseService;

    public SupplierController(SupplierService supplierService, PurchaseService purchaseService) {
        this.supplierService = supplierService;
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public List<SupplierResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return supplierService.list(search, includeArchived);
    }

    @GetMapping("/{id}")
    public SupplierResponse getById(@PathVariable UUID id) {
        return supplierService.getById(id);
    }

    @GetMapping("/{id}/summary")
    public SupplierSummaryResponse getSummary(@PathVariable UUID id) {
        return supplierService.getSummary(id);
    }

    @GetMapping("/{id}/purchases")
    public List<PurchaseResponse> listPurchases(@PathVariable UUID id) {
        return purchaseService.listBySupplier(id);
    }

    @PatchMapping("/{id}")
    public SupplierResponse update(@PathVariable UUID id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(id, request);
    }

    @PatchMapping("/{id}/archive")
    public SupplierResponse archive(@PathVariable UUID id) {
        return supplierService.archive(id);
    }
}
