package com.inventory.inventory.entity;

import com.inventory.common.model.TenantAwareEntity;
import com.inventory.product.entity.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement extends TenantAwareEntity {

    public static final String TYPE_OPENING_STOCK = "OPENING_STOCK";
    public static final String TYPE_OPENING_STOCK_UPDATE = "OPENING_STOCK_UPDATE";
    public static final String TYPE_MANUAL_ADJUSTMENT = "MANUAL_ADJUSTMENT";
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_PURCHASE_CANCELLATION = "PURCHASE_CANCELLATION";
    public static final String TYPE_SALE = "SALE";
    public static final String TYPE_SALE_CANCELLATION = "SALE_CANCELLATION";
    public static final String TYPE_SALE_RETURN = "SALE_RETURN";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, updatable = false)
    private Product product;

    @Column(name = "movement_type", nullable = false, length = 50)
    private String movementType;

    @Column(name = "quantity_change", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityChange;

    @Column(name = "quantity_before", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityBefore;

    @Column(name = "quantity_after", nullable = false, precision = 19, scale = 3)
    private BigDecimal quantityAfter;

    @Column(name = "notes", length = 255)
    private String notes;

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        this.movementType = movementType;
    }

    public BigDecimal getQuantityChange() {
        return quantityChange;
    }

    public void setQuantityChange(BigDecimal quantityChange) {
        this.quantityChange = quantityChange;
    }

    public BigDecimal getQuantityBefore() {
        return quantityBefore;
    }

    public void setQuantityBefore(BigDecimal quantityBefore) {
        this.quantityBefore = quantityBefore;
    }

    public BigDecimal getQuantityAfter() {
        return quantityAfter;
    }

    public void setQuantityAfter(BigDecimal quantityAfter) {
        this.quantityAfter = quantityAfter;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
