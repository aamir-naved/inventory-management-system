package com.inventory.report.excel;

import com.inventory.report.dto.CustomerOutstandingReportResponse;
import com.inventory.report.dto.GstReportResponse;
import com.inventory.report.dto.InventoryReportResponse;
import com.inventory.report.dto.PurchaseReportResponse;
import com.inventory.report.dto.SalesReportResponse;
import com.inventory.report.dto.SupplierOutstandingReportResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

public final class ReportWorkbook {

    private ReportWorkbook() {
    }

    public static byte[] inventory(InventoryReportResponse report) {
        return write("Inventory", new String[]{
            "Product", "SKU", "Stock", "Cost", "Selling price", "Stock value", "Low stock"
        }, report.rows().stream().map(row -> new String[]{
            row.productName(),
            blank(row.sku()),
            number(row.currentStock()),
            number(row.costPrice()),
            number(row.sellingPrice()),
            number(row.stockValue()),
            row.lowStock() ? "Yes" : "No"
        }).toList());
    }

    public static byte[] sales(SalesReportResponse report) {
        return write("Sales", new String[]{
            "Invoice", "Date", "Customer", "Net", "Paid", "Outstanding", "Status"
        }, report.rows().stream().map(row -> new String[]{
            row.saleNumber(),
            String.valueOf(row.saleDate()),
            row.customerName(),
            number(row.netAmount()),
            number(row.amountPaid()),
            number(row.outstandingAmount()),
            row.paymentStatus()
        }).toList());
    }

    public static byte[] purchases(PurchaseReportResponse report) {
        return write("Purchases", new String[]{
            "Bill", "Date", "Supplier", "Amount", "Paid", "Outstanding", "Status"
        }, report.rows().stream().map(row -> new String[]{
            row.purchaseNumber(),
            String.valueOf(row.purchaseDate()),
            row.supplierName(),
            number(row.totalAmount()),
            number(row.amountPaid()),
            number(row.outstandingAmount()),
            row.paymentStatus()
        }).toList());
    }

    public static byte[] customerOutstanding(CustomerOutstandingReportResponse report) {
        return write("Customer dues", new String[]{
            "Customer", "Invoices", "Billed", "Paid", "Outstanding"
        }, report.rows().stream().map(row -> new String[]{
            row.customerName(),
            String.valueOf(row.invoiceCount()),
            number(row.netBilled()),
            number(row.amountPaid()),
            number(row.outstandingAmount())
        }).toList());
    }

    public static byte[] supplierOutstanding(SupplierOutstandingReportResponse report) {
        return write("Supplier dues", new String[]{
            "Supplier", "Bills", "Billed", "Paid", "Outstanding"
        }, report.rows().stream().map(row -> new String[]{
            row.supplierName(),
            String.valueOf(row.billCount()),
            number(row.billedAmount()),
            number(row.amountPaid()),
            number(row.outstandingAmount())
        }).toList());
    }

    public static byte[] gst(GstReportResponse report) {
        return write("GST", new String[]{
            "Type", "Number", "Date", "Party", "Interstate", "GST %", "Taxable", "CGST", "SGST", "IGST"
        }, report.rows().stream().map(row -> new String[]{
            row.documentType(),
            row.documentNumber(),
            String.valueOf(row.documentDate()),
            row.partyName(),
            row.interstate() ? "Yes" : "No",
            number(row.gstRate()),
            number(row.taxableAmount()),
            number(row.cgstAmount()),
            number(row.sgstAmount()),
            number(row.igstAmount())
        }).toList());
    }

    private static byte[] write(String sheetName, String[] headers, java.util.List<String[]> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            int rowIndex = 1;
            for (String[] values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int index = 0; index < values.length; index++) {
                    row.createCell(index).setCellValue(values[index]);
                }
            }
            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create the report Excel file", exception);
        }
    }

    private static String number(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private static String blank(String value) {
        return value == null ? "" : value;
    }
}
