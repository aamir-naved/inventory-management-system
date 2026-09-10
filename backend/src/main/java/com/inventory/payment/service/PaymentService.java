package com.inventory.payment.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.payment.dto.PaymentRequest;
import com.inventory.payment.dto.PaymentResponse;
import com.inventory.payment.entity.Payment;
import com.inventory.payment.repository.PaymentRepository;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;

    public PaymentService(
        PaymentRepository paymentRepository,
        SaleRepository saleRepository,
        PurchaseRepository purchaseRepository,
        SaleReturnRepository saleReturnRepository,
        PurchaseReturnRepository purchaseReturnRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
    }

    public PaymentResponse createForSale(UUID saleId, PaymentRequest request) {
        Sale sale = findSaleForUpdate(saleId);
        return toResponse(recordSalePayment(sale, request.amount(), request.paymentDate(), request.notes()));
    }

    public PaymentResponse createForPurchase(UUID purchaseId, PaymentRequest request) {
        Purchase purchase = findPurchaseForUpdate(purchaseId);
        return toResponse(recordPurchasePayment(
            purchase,
            request.amount(),
            request.paymentDate(),
            request.notes()
        ));
    }

    public Payment recordSalePayment(Sale sale, BigDecimal amount, LocalDate paymentDate, String notes) {
        if (sale.isCancelled()) {
            throw new IllegalArgumentException("Cancelled sales cannot accept payments");
        }

        BigDecimal paymentAmount = requirePositiveAmount(amount);
        BigDecimal billable = saleBillableAmount(sale);
        BigDecimal currentPaid = nullSafe(sale.getAmountPaid());
        BigDecimal nextPaid = currentPaid.add(paymentAmount);

        if (nextPaid.compareTo(billable) > 0) {
            throw new IllegalArgumentException("Payment exceeds outstanding amount");
        }

        Payment payment = new Payment();
        payment.setBusinessId(sale.getBusinessId());
        payment.setPartyType(Payment.PARTY_CUSTOMER);
        payment.setPartyId(sale.getCustomer().getId());
        payment.setDocumentType(Payment.DOCUMENT_SALE);
        payment.setDocumentId(sale.getId());
        payment.setPaymentDate(paymentDate);
        payment.setAmount(paymentAmount);
        payment.setPaymentKind(Payment.KIND_RECEIPT);
        payment.setNotes(normalize(notes));
        Payment saved = paymentRepository.save(payment);

        sale.setAmountPaid(nextPaid);
        sale.setPaymentStatus(PaymentAmounts.deriveStatus(nextPaid, billable));
        return saved;
    }

    public Payment recordPurchasePayment(
        Purchase purchase,
        BigDecimal amount,
        LocalDate paymentDate,
        String notes
    ) {
        if (purchase.isCancelled()) {
            throw new IllegalArgumentException("Cancelled purchases cannot accept payments");
        }

        BigDecimal paymentAmount = requirePositiveAmount(amount);
        BigDecimal billable = purchaseBillableAmount(purchase);
        BigDecimal currentPaid = nullSafe(purchase.getAmountPaid());
        BigDecimal nextPaid = currentPaid.add(paymentAmount);

        if (nextPaid.compareTo(billable) > 0) {
            throw new IllegalArgumentException("Payment exceeds outstanding amount");
        }

        Payment payment = new Payment();
        payment.setBusinessId(purchase.getBusinessId());
        payment.setPartyType(Payment.PARTY_SUPPLIER);
        payment.setPartyId(purchase.getSupplier().getId());
        payment.setDocumentType(Payment.DOCUMENT_PURCHASE);
        payment.setDocumentId(purchase.getId());
        payment.setPaymentDate(paymentDate);
        payment.setAmount(paymentAmount);
        payment.setPaymentKind(Payment.KIND_RECEIPT);
        payment.setNotes(normalize(notes));
        Payment saved = paymentRepository.save(payment);

        purchase.setAmountPaid(nextPaid);
        purchase.setPaymentStatus(PaymentAmounts.deriveStatus(nextPaid, billable));
        return saved;
    }

    /** Writes a refund trail and clears denormalized paid amount when a sale is cancelled. */
    public void reversePaymentsForCancelledSale(Sale sale, LocalDate refundDate, String notes) {
        BigDecimal paid = nullSafe(sale.getAmountPaid());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            sale.setAmountPaid(BigDecimal.ZERO);
            sale.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
            return;
        }

        Payment refund = new Payment();
        refund.setBusinessId(sale.getBusinessId());
        refund.setPartyType(Payment.PARTY_CUSTOMER);
        refund.setPartyId(sale.getCustomer().getId());
        refund.setDocumentType(Payment.DOCUMENT_SALE);
        refund.setDocumentId(sale.getId());
        refund.setPaymentDate(refundDate);
        refund.setAmount(paid);
        refund.setPaymentKind(Payment.KIND_REFUND);
        refund.setNotes(normalize(notes));
        paymentRepository.save(refund);

        sale.setAmountPaid(BigDecimal.ZERO);
        sale.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
    }

    /** Writes a refund trail and clears denormalized paid amount when a purchase is cancelled. */
    public void reversePaymentsForCancelledPurchase(Purchase purchase, LocalDate refundDate, String notes) {
        BigDecimal paid = nullSafe(purchase.getAmountPaid());
        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            purchase.setAmountPaid(BigDecimal.ZERO);
            purchase.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
            return;
        }

        Payment refund = new Payment();
        refund.setBusinessId(purchase.getBusinessId());
        refund.setPartyType(Payment.PARTY_SUPPLIER);
        refund.setPartyId(purchase.getSupplier().getId());
        refund.setDocumentType(Payment.DOCUMENT_PURCHASE);
        refund.setDocumentId(purchase.getId());
        refund.setPaymentDate(refundDate);
        refund.setAmount(paid);
        refund.setPaymentKind(Payment.KIND_REFUND);
        refund.setNotes(normalize(notes));
        paymentRepository.save(refund);

        purchase.setAmountPaid(BigDecimal.ZERO);
        purchase.setPaymentStatus(PaymentAmounts.STATUS_PENDING);
    }

    public void syncSalePaymentStatus(Sale sale) {
        BigDecimal amountPaid = nullSafe(sale.getAmountPaid());
        BigDecimal billable = saleBillableAmount(sale);
        sale.setPaymentStatus(PaymentAmounts.deriveStatus(amountPaid, billable));
    }

    public void syncPurchasePaymentStatus(Purchase purchase) {
        BigDecimal amountPaid = nullSafe(purchase.getAmountPaid());
        BigDecimal billable = purchaseBillableAmount(purchase);
        purchase.setPaymentStatus(PaymentAmounts.deriveStatus(amountPaid, billable));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForSale(UUID saleId) {
        Sale sale = findSale(saleId);
        return paymentRepository
            .findByBusinessIdAndDocumentTypeAndDocumentIdOrderByPaymentDateDescCreatedAtDesc(
                sale.getBusinessId(),
                Payment.DOCUMENT_SALE,
                sale.getId()
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> listForPurchase(UUID purchaseId) {
        Purchase purchase = findPurchase(purchaseId);
        return paymentRepository
            .findByBusinessIdAndDocumentTypeAndDocumentIdOrderByPaymentDateDescCreatedAtDesc(
                purchase.getBusinessId(),
                Payment.DOCUMENT_PURCHASE,
                purchase.getId()
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private BigDecimal saleBillableAmount(Sale sale) {
        BigDecimal returnedAmount = saleReturnRepository.sumReturnedAmountForSale(
            sale.getBusinessId(),
            sale.getId()
        );
        return sale.getTotalAmount().subtract(nullSafe(returnedAmount));
    }

    private BigDecimal purchaseBillableAmount(Purchase purchase) {
        BigDecimal returnedAmount = purchaseReturnRepository.sumReturnedAmountForPurchase(
            purchase.getBusinessId(),
            purchase.getId()
        );
        return purchase.getTotalAmount().subtract(nullSafe(returnedAmount));
    }

    private Sale findSale(UUID saleId) {
        return saleRepository.findByIdAndBusinessId(saleId, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
    }

    private Sale findSaleForUpdate(UUID saleId) {
        return saleRepository.findByIdAndBusinessIdForUpdate(saleId, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
    }

    private Purchase findPurchase(UUID purchaseId) {
        return purchaseRepository.findByIdAndBusinessId(purchaseId, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
    }

    private Purchase findPurchaseForUpdate(UUID purchaseId) {
        return purchaseRepository.findByIdAndBusinessIdForUpdate(purchaseId, requireBusinessId())
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }
        return amount;
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

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getBusinessId(),
            payment.getPartyType(),
            payment.getPartyId(),
            payment.getDocumentType(),
            payment.getDocumentId(),
            payment.getPaymentDate(),
            payment.getAmount(),
            payment.getPaymentKind() == null ? Payment.KIND_RECEIPT : payment.getPaymentKind(),
            payment.getNotes(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }
}
