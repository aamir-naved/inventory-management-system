package com.inventory.sales.controller;

import com.inventory.sales.dto.SaleReturnRequest;
import com.inventory.sales.dto.SaleReturnResponse;
import com.inventory.sales.service.SaleReturnService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/sales")
public class SaleReturnController {

    private final SaleReturnService saleReturnService;

    public SaleReturnController(SaleReturnService saleReturnService) {
        this.saleReturnService = saleReturnService;
    }

    @GetMapping("/returns")
    public List<SaleReturnResponse> list(@RequestParam(required = false) String search) {
        return saleReturnService.list(search);
    }

    @GetMapping("/returns/{returnId}")
    public SaleReturnResponse getById(@PathVariable UUID returnId) {
        return saleReturnService.getById(returnId);
    }

    @GetMapping("/{saleId}/returns")
    public List<SaleReturnResponse> listForSale(@PathVariable UUID saleId) {
        return saleReturnService.listForSale(saleId);
    }

    @PostMapping("/{saleId}/returns")
    public ResponseEntity<SaleReturnResponse> create(
        @PathVariable UUID saleId,
        @Valid @RequestBody SaleReturnRequest request
    ) {
        SaleReturnResponse response = saleReturnService.create(saleId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/sales/returns/{returnId}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
