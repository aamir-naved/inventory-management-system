package com.inventory.sales.controller;

import com.inventory.common.api.PagedResponse;
import com.inventory.common.idempotency.IdempotencyService;
import com.inventory.sales.dto.*;
import com.inventory.sales.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/sales")
public class SaleController {
    private final SaleService saleService;
    private final IdempotencyService idempotencyService;

    public SaleController(SaleService saleService, IdempotencyService idempotencyService) {
        this.saleService = saleService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping
    public ResponseEntity<SaleResponse> create(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody SaleRequest request
    ) {
        return idempotencyService.execute(
            idempotencyKey,
            "POST",
            "/sales",
            request,
            SaleResponse.class,
            () -> saleService.create(request),
            response -> ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri()
        );
    }

    @GetMapping
    public PagedResponse<SaleResponse> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return saleService.list(search, page, size);
    }

    @GetMapping("/{id}")
    public SaleResponse getById(@PathVariable UUID id) {
        return saleService.getById(id);
    }

    @PatchMapping("/{id}")
    public SaleResponse update(@PathVariable UUID id, @Valid @RequestBody SaleUpdateRequest request) {
        return saleService.update(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public SaleResponse cancel(@PathVariable UUID id, @Valid @RequestBody SaleCancellationRequest request) {
        return saleService.cancel(id, request);
    }
}
