package com.inventory.purchase.controller;

import com.inventory.purchase.dto.PurchaseReturnRequest;
import com.inventory.purchase.dto.PurchaseReturnResponse;
import com.inventory.purchase.service.PurchaseReturnService;
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
@RequestMapping("/purchases")
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    public PurchaseReturnController(PurchaseReturnService purchaseReturnService) {
        this.purchaseReturnService = purchaseReturnService;
    }

    @GetMapping("/returns")
    public List<PurchaseReturnResponse> list(@RequestParam(required = false) String search) {
        return purchaseReturnService.list(search);
    }

    @GetMapping("/returns/{returnId}")
    public PurchaseReturnResponse getById(@PathVariable UUID returnId) {
        return purchaseReturnService.getById(returnId);
    }

    @GetMapping("/{purchaseId}/returns")
    public List<PurchaseReturnResponse> listForPurchase(@PathVariable UUID purchaseId) {
        return purchaseReturnService.listForPurchase(purchaseId);
    }

    @PostMapping("/{purchaseId}/returns")
    public ResponseEntity<PurchaseReturnResponse> create(
        @PathVariable UUID purchaseId,
        @Valid @RequestBody PurchaseReturnRequest request
    ) {
        PurchaseReturnResponse response = purchaseReturnService.create(purchaseId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/purchases/returns/{returnId}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
