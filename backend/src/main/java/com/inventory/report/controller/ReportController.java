package com.inventory.report.controller;

import com.inventory.report.dto.CustomerOutstandingReportResponse;
import com.inventory.report.dto.InventoryReportResponse;
import com.inventory.report.dto.PurchaseReportResponse;
import com.inventory.report.dto.SalesReportResponse;
import com.inventory.report.dto.SupplierOutstandingReportResponse;
import com.inventory.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/inventory")
    public InventoryReportResponse inventory(
        @RequestParam(defaultValue = "false") boolean lowStockOnly
    ) {
        return reportService.inventoryReport(lowStockOnly);
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.salesReport(from, to);
    }

    @GetMapping("/purchases")
    public PurchaseReportResponse purchases(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.purchaseReport(from, to);
    }

    @GetMapping("/outstanding/customers")
    public CustomerOutstandingReportResponse customerOutstanding() {
        return reportService.customerOutstandingReport();
    }

    @GetMapping("/outstanding/suppliers")
    public SupplierOutstandingReportResponse supplierOutstanding() {
        return reportService.supplierOutstandingReport();
    }
}
