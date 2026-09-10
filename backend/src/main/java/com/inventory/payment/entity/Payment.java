package com.inventory.payment.entity;

import com.inventory.common.model.TenantAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment extends TenantAwareEntity {

    public static final String PARTY_CUSTOMER = "CUSTOMER";
    public static final String PARTY_SUPPLIER = "SUPPLIER";
    public static final String DOCUMENT_SALE = "SALE";
    public static final String DOCUMENT_PURCHASE = "PURCHASE";
    public static final String KIND_RECEIPT = "RECEIPT";
    public static final String KIND_REFUND = "REFUND";

    @Column(name = "party_type", nullable = false, length = 30)
    private String partyType;

    @Column(name = "party_id", nullable = false)
    private UUID partyId;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_kind", nullable = false, length = 20)
    private String paymentKind = KIND_RECEIPT;

    @Column(name = "notes", length = 255)
    private String notes;

    public String getPartyType() {
        return partyType;
    }

    public void setPartyType(String partyType) {
        this.partyType = partyType;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public void setPartyId(UUID partyId) {
        this.partyId = partyId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getPaymentKind() {
        return paymentKind;
    }

    public void setPaymentKind(String paymentKind) {
        this.paymentKind = paymentKind;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
