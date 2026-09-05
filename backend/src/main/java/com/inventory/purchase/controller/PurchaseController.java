package com.inventory.purchase.controller;

import com.inventory.common.api.PagedResponse;
import com.inventory.purchase.dto.PurchaseCancellationRequest;
import com.inventory.purchase.dto.PurchaseRequest;
import com.inventory.purchase.dto.PurchaseResponse;
import com.inventory.purchase.dto.PurchaseUpdateRequest;
import com.inventory.purchase.service.PurchaseService;
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
import java.util.UUID;

@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        PurchaseResponse response = purchaseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public PagedResponse<PurchaseResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return purchaseService.list(search, page, size);
    }

    @GetMapping("/{id}")
    public PurchaseResponse getById(@PathVariable UUID id) {
        return purchaseService.getById(id);
    }

    @PatchMapping("/{id}")
    public PurchaseResponse update(@PathVariable UUID id, @Valid @RequestBody PurchaseUpdateRequest request) {
        return purchaseService.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public PurchaseResponse cancel(@PathVariable UUID id, @Valid @RequestBody PurchaseCancellationRequest request) {
        return purchaseService.cancel(id, request);
    }
}
