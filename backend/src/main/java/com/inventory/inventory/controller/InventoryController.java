package com.inventory.inventory.controller;

import com.inventory.inventory.dto.InventoryAdjustmentRequest;
import com.inventory.inventory.dto.InventoryMovementResponse;
import com.inventory.inventory.dto.InventoryStockResponse;
import com.inventory.inventory.dto.InventorySummaryResponse;
import com.inventory.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryStockResponse> listStock(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "false") boolean lowStockOnly,
        @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return inventoryService.listStock(search, lowStockOnly, includeArchived);
    }

    @GetMapping("/summary")
    public InventorySummaryResponse summary() {
        return inventoryService.getSummary();
    }

    @GetMapping("/movements")
    public List<InventoryMovementResponse> movements(@RequestParam(required = false) UUID productId) {
        return inventoryService.listMovements(productId);
    }

    @PostMapping("/adjustments")
    public InventoryMovementResponse adjust(@Valid @RequestBody InventoryAdjustmentRequest request) {
        return inventoryService.adjustStock(request);
    }
}
