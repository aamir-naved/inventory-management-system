package com.inventory.report.controller;

import com.inventory.report.dto.CustomerOutstandingReportResponse;
import com.inventory.report.dto.GstReportResponse;
import com.inventory.report.dto.InventoryReportResponse;
import com.inventory.report.dto.PurchaseReportResponse;
import com.inventory.report.dto.SalesReportResponse;
import com.inventory.report.dto.SupplierOutstandingReportResponse;
import com.inventory.report.excel.ReportWorkbook;
import com.inventory.report.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
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

    @GetMapping("/gst")
    public GstReportResponse gst(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reportService.gstReport(from, to);
    }

    @GetMapping("/inventory.xlsx")
    public ResponseEntity<byte[]> inventoryExcel(
        @RequestParam(defaultValue = "false") boolean lowStockOnly
    ) {
        return excel("inventory-report.xlsx", ReportWorkbook.inventory(reportService.inventoryReport(lowStockOnly)));
    }

    @GetMapping("/sales.xlsx")
    public ResponseEntity<byte[]> salesExcel(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return excel("sales-report.xlsx", ReportWorkbook.sales(reportService.salesReport(from, to)));
    }

    @GetMapping("/purchases.xlsx")
    public ResponseEntity<byte[]> purchasesExcel(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return excel("purchases-report.xlsx", ReportWorkbook.purchases(reportService.purchaseReport(from, to)));
    }

    @GetMapping("/outstanding/customers.xlsx")
    public ResponseEntity<byte[]> customerOutstandingExcel() {
        return excel("customer-dues.xlsx", ReportWorkbook.customerOutstanding(reportService.customerOutstandingReport()));
    }

    @GetMapping("/outstanding/suppliers.xlsx")
    public ResponseEntity<byte[]> supplierOutstandingExcel() {
        return excel("supplier-dues.xlsx", ReportWorkbook.supplierOutstanding(reportService.supplierOutstandingReport()));
    }

    @GetMapping("/gst.xlsx")
    public ResponseEntity<byte[]> gstExcel(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return excel("gst-report.xlsx", ReportWorkbook.gst(reportService.gstReport(from, to)));
    }

    private ResponseEntity<byte[]> excel(String filename, byte[] content) {
        return ResponseEntity.ok()
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString()
            )
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(content);
    }
}
