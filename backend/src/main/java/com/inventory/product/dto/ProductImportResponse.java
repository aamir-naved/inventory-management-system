package com.inventory.product.dto;

import java.util.List;

public record ProductImportResponse(
    int created,
    int updated,
    int failed,
    List<ProductImportRowError> errors
) {
}
