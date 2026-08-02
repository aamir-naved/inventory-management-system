package com.inventory.payment.controller;

import com.inventory.payment.dto.PaymentRequest;
import com.inventory.payment.dto.PaymentResponse;
import com.inventory.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/purchases/{purchaseId}/payments")
public class PurchasePaymentController {

    private final PaymentService paymentService;

    public PurchasePaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<PaymentResponse> list(@PathVariable UUID purchaseId) {
        return paymentService.listForPurchase(purchaseId);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
        @PathVariable UUID purchaseId,
        @Valid @RequestBody PaymentRequest request
    ) {
        PaymentResponse response = paymentService.createForPurchase(purchaseId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        return ResponseEntity.created(location).body(response);
    }
}