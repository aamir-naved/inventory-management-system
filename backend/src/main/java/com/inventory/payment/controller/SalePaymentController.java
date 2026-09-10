package com.inventory.payment.controller;

import com.inventory.common.idempotency.IdempotencyService;
import com.inventory.payment.dto.PaymentRequest;
import com.inventory.payment.dto.PaymentResponse;
import com.inventory.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sales/{saleId}/payments")
public class SalePaymentController {

    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;

    public SalePaymentController(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    public List<PaymentResponse> list(@PathVariable UUID saleId) {
        return paymentService.listForSale(saleId);
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
        @PathVariable UUID saleId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody PaymentRequest request
    ) {
        return idempotencyService.execute(
            idempotencyKey,
            "POST",
            "/sales/" + saleId + "/payments",
            request,
            PaymentResponse.class,
            () -> paymentService.createForSale(saleId, request),
            response -> ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri()
        );
    }
}
