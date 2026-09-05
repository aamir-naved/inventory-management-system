package com.inventory.product.dto;

public record ProductImportRowError(
    int rowNumber,
    String message
) {
}
