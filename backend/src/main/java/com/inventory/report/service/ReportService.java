package com.inventory.report.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.inventory.dto.InventoryStockResponse;
import com.inventory.inventory.service.InventoryService;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.report.dto.CustomerOutstandingReportResponse;
import com.inventory.report.dto.GstReportResponse;
import com.inventory.report.dto.InventoryReportResponse;
import com.inventory.report.dto.PurchaseReportResponse;
import com.inventory.report.dto.SalesReportResponse;
import com.inventory.report.dto.SupplierOutstandingReportResponse;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final InventoryService inventoryService;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;

    public ReportService(
        InventoryService inventoryService,
        SaleRepository saleRepository,
        PurchaseRepository purchaseRepository,
        SaleReturnRepository saleReturnRepository,
        PurchaseReturnRepository purchaseReturnRepository
    ) {
        this.inventoryService = inventoryService;
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
    }

    public InventoryReportResponse inventoryReport(boolean lowStockOnly) {
        requireBusinessId();
        List<InventoryStockResponse> stock = inventoryService.listStockAll("", lowStockOnly, false);

        List<InventoryReportResponse.Row> rows = stock.stream()
            .map(item -> new InventoryReportResponse.Row(
                item.productId(),
                item.productName(),
                item.sku(),
                item.currentStock(),
                item.costPrice(),
                item.sellingPrice(),
                item.stockValue(),
                item.lowStock()
            ))
            .toList();

        BigDecimal totalStockValue = rows.stream()
            .map(InventoryReportResponse.Row::stockValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        long lowStockProducts = rows.stream().filter(InventoryReportResponse.Row::lowStock).count();

        return new InventoryReportResponse(
            OffsetDateTime.now(),
            rows.size(),
            totalStockValue,
            lowStockProducts,
            rows
        );
    }

    public SalesReportResponse salesReport(LocalDate from, LocalDate to) {
        UUID businessId = requireBusinessId();
        validateDateRange(from, to);

        List<Sale> sales = saleRepository.findForReport(businessId, from, to);
        Map<UUID, BigDecimal> returnedBySaleId = returnedAmountsBySale(businessId);

        List<SalesReportResponse.Row> rows = new ArrayList<>();
        BigDecimal totalNetAmount = BigDecimal.ZERO;
        BigDecimal totalAmountPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Sale sale : sales) {
            BigDecimal returned = returnedBySaleId.getOrDefault(sale.getId(), BigDecimal.ZERO);
            BigDecimal netAmount = sale.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = nullSafe(sale.getAmountPaid());
            BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, netAmount);
            String paymentStatus = PaymentAmounts.deriveStatus(amountPaid, netAmount);

            rows.add(new SalesReportResponse.Row(
                sale.getId(),
                sale.getSaleNumber(),
                sale.getSaleDate(),
                sale.getCustomer().getId(),
                sale.getCustomer().getName(),
                netAmount,
                amountPaid,
                outstanding,
                paymentStatus
            ));

            totalNetAmount = totalNetAmount.add(netAmount);
            totalAmountPaid = totalAmountPaid.add(amountPaid);
            totalOutstanding = totalOutstanding.add(outstanding);
        }

        return new SalesReportResponse(
            OffsetDateTime.now(),
            from,
            to,
            rows.size(),
            totalNetAmount,
            totalAmountPaid,
            totalOutstanding,
            rows
        );
    }

    public PurchaseReportResponse purchaseReport(LocalDate from, LocalDate to) {
        UUID businessId = requireBusinessId();
        validateDateRange(from, to);

        List<Purchase> purchases = purchaseRepository.findForReport(businessId, from, to);
        Map<UUID, BigDecimal> returnedByPurchaseId = returnedAmountsByPurchase(businessId);

        List<PurchaseReportResponse.Row> rows = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalAmountPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Purchase purchase : purchases) {
            BigDecimal returned = returnedByPurchaseId.getOrDefault(purchase.getId(), BigDecimal.ZERO);
            BigDecimal billed = purchase.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = nullSafe(purchase.getAmountPaid());
            BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, billed);
            String paymentStatus = PaymentAmounts.deriveStatus(amountPaid, billed);

            rows.add(new PurchaseReportResponse.Row(
                purchase.getId(),
                purchase.getPurchaseNumber(),
                purchase.getPurchaseDate(),
                purchase.getSupplier().getId(),
                purchase.getSupplier().getName(),
                billed,
                amountPaid,
                outstanding,
                paymentStatus
            ));

            totalAmount = totalAmount.add(billed);
            totalAmountPaid = totalAmountPaid.add(amountPaid);
            totalOutstanding = totalOutstanding.add(outstanding);
        }

        return new PurchaseReportResponse(
            OffsetDateTime.now(),
            from,
            to,
            rows.size(),
            totalAmount,
            totalAmountPaid,
            totalOutstanding,
            rows
        );
    }

    public CustomerOutstandingReportResponse customerOutstandingReport() {
        UUID businessId = requireBusinessId();
        List<Sale> sales = saleRepository.findForReport(businessId, null, null);
        Map<UUID, BigDecimal> returnedBySaleId = returnedAmountsBySale(businessId);

        Map<UUID, OutstandingAccumulator> byCustomer = new HashMap<>();

        for (Sale sale : sales) {
            BigDecimal returned = returnedBySaleId.getOrDefault(sale.getId(), BigDecimal.ZERO);
            BigDecimal netAmount = sale.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = nullSafe(sale.getAmountPaid());
            BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, netAmount);

            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            UUID customerId = sale.getCustomer().getId();
            OutstandingAccumulator accumulator = byCustomer.computeIfAbsent(
                customerId,
                id -> new OutstandingAccumulator(sale.getCustomer().getName())
            );
            accumulator.invoiceCount++;
            accumulator.billed = accumulator.billed.add(netAmount);
            accumulator.paid = accumulator.paid.add(amountPaid);
            accumulator.outstanding = accumulator.outstanding.add(outstanding);
        }

        List<CustomerOutstandingReportResponse.Row> rows = byCustomer.entrySet().stream()
            .map(entry -> new CustomerOutstandingReportResponse.Row(
                entry.getKey(),
                entry.getValue().name,
                entry.getValue().invoiceCount,
                entry.getValue().billed,
                entry.getValue().paid,
                entry.getValue().outstanding
            ))
            .sorted(Comparator.comparing(CustomerOutstandingReportResponse.Row::outstandingAmount).reversed())
            .toList();

        BigDecimal totalOutstanding = rows.stream()
            .map(CustomerOutstandingReportResponse.Row::outstandingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CustomerOutstandingReportResponse(
            OffsetDateTime.now(),
            rows.size(),
            totalOutstanding,
            rows
        );
    }

    public SupplierOutstandingReportResponse supplierOutstandingReport() {
        UUID businessId = requireBusinessId();
        List<Purchase> purchases = purchaseRepository.findForReport(businessId, null, null);
        Map<UUID, BigDecimal> returnedByPurchaseId = returnedAmountsByPurchase(businessId);

        Map<UUID, OutstandingAccumulator> bySupplier = new HashMap<>();

        for (Purchase purchase : purchases) {
            BigDecimal returned = returnedByPurchaseId.getOrDefault(purchase.getId(), BigDecimal.ZERO);
            BigDecimal billed = purchase.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = nullSafe(purchase.getAmountPaid());
            BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, billed);

            if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            UUID supplierId = purchase.getSupplier().getId();
            OutstandingAccumulator accumulator = bySupplier.computeIfAbsent(
                supplierId,
                id -> new OutstandingAccumulator(purchase.getSupplier().getName())
            );
            accumulator.invoiceCount++;
            accumulator.billed = accumulator.billed.add(billed);
            accumulator.paid = accumulator.paid.add(amountPaid);
            accumulator.outstanding = accumulator.outstanding.add(outstanding);
        }

        List<SupplierOutstandingReportResponse.Row> rows = bySupplier.entrySet().stream()
            .map(entry -> new SupplierOutstandingReportResponse.Row(
                entry.getKey(),
                entry.getValue().name,
                entry.getValue().invoiceCount,
                entry.getValue().billed,
                entry.getValue().paid,
                entry.getValue().outstanding
            ))
            .sorted(Comparator.comparing(SupplierOutstandingReportResponse.Row::outstandingAmount).reversed())
            .toList();

        BigDecimal totalOutstanding = rows.stream()
            .map(SupplierOutstandingReportResponse.Row::outstandingAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SupplierOutstandingReportResponse(
            OffsetDateTime.now(),
            rows.size(),
            totalOutstanding,
            rows
        );
    }

    public GstReportResponse gstReport(LocalDate from, LocalDate to) {
        UUID businessId = requireBusinessId();
        validateDateRange(from, to);
        List<GstReportResponse.Row> rows = new ArrayList<>();

        BigDecimal outputTaxable = BigDecimal.ZERO;
        BigDecimal outputCgst = BigDecimal.ZERO;
        BigDecimal outputSgst = BigDecimal.ZERO;
        BigDecimal outputIgst = BigDecimal.ZERO;

        BigDecimal inputTaxable = BigDecimal.ZERO;
        BigDecimal inputCgst = BigDecimal.ZERO;
        BigDecimal inputSgst = BigDecimal.ZERO;
        BigDecimal inputIgst = BigDecimal.ZERO;

        for (Sale sale : saleRepository.findForReport(businessId, from, to)) {
            Hibernate.initialize(sale.getItems());
            for (com.inventory.sales.entity.SaleItem item : sale.getItems()) {
                BigDecimal taxable = nullSafe(item.getTaxableAmount());
                BigDecimal cgst = nullSafe(item.getCgstAmount());
                BigDecimal sgst = nullSafe(item.getSgstAmount());
                BigDecimal igst = nullSafe(item.getIgstAmount());
                rows.add(new GstReportResponse.Row(
                    "SALE",
                    sale.getSaleNumber(),
                    sale.getSaleDate(),
                    sale.getCustomer().getName(),
                    sale.isInterstate(),
                    nullSafe(item.getGstRate()),
                    taxable,
                    cgst,
                    sgst,
                    igst
                ));
                outputTaxable = outputTaxable.add(taxable);
                outputCgst = outputCgst.add(cgst);
                outputSgst = outputSgst.add(sgst);
                outputIgst = outputIgst.add(igst);
            }
        }

        for (com.inventory.sales.entity.SaleReturn saleReturn : saleReturnRepository.findForReport(businessId, from, to)) {
            Hibernate.initialize(saleReturn.getItems());
            Sale sale = saleReturn.getSale();
            for (com.inventory.sales.entity.SaleReturnItem returnItem : saleReturn.getItems()) {
                com.inventory.sales.entity.SaleItem saleItem = returnItem.getSaleItem();
                TaxSlice reversed = reverseTaxShare(
                    nullSafe(saleItem.getLineTotal()),
                    nullSafe(returnItem.getLineTotal()),
                    nullSafe(saleItem.getTaxableAmount()),
                    nullSafe(saleItem.getCgstAmount()),
                    nullSafe(saleItem.getSgstAmount()),
                    nullSafe(saleItem.getIgstAmount())
                );
                rows.add(new GstReportResponse.Row(
                    "SALE_RETURN",
                    saleReturn.getReturnNumber(),
                    saleReturn.getReturnDate(),
                    sale.getCustomer().getName(),
                    sale.isInterstate(),
                    nullSafe(saleItem.getGstRate()),
                    reversed.taxable().negate(),
                    reversed.cgst().negate(),
                    reversed.sgst().negate(),
                    reversed.igst().negate()
                ));
                outputTaxable = outputTaxable.subtract(reversed.taxable());
                outputCgst = outputCgst.subtract(reversed.cgst());
                outputSgst = outputSgst.subtract(reversed.sgst());
                outputIgst = outputIgst.subtract(reversed.igst());
            }
        }

        for (Purchase purchase : purchaseRepository.findForReport(businessId, from, to)) {
            Hibernate.initialize(purchase.getItems());
            for (com.inventory.purchase.entity.PurchaseItem item : purchase.getItems()) {
                BigDecimal taxable = nullSafe(item.getTaxableAmount());
                BigDecimal cgst = nullSafe(item.getCgstAmount());
                BigDecimal sgst = nullSafe(item.getSgstAmount());
                BigDecimal igst = nullSafe(item.getIgstAmount());
                rows.add(new GstReportResponse.Row(
                    "PURCHASE",
                    purchase.getPurchaseNumber(),
                    purchase.getPurchaseDate(),
                    purchase.getSupplier().getName(),
                    purchase.isInterstate(),
                    nullSafe(item.getGstRate()),
                    taxable,
                    cgst,
                    sgst,
                    igst
                ));
                inputTaxable = inputTaxable.add(taxable);
                inputCgst = inputCgst.add(cgst);
                inputSgst = inputSgst.add(sgst);
                inputIgst = inputIgst.add(igst);
            }
        }

        for (com.inventory.purchase.entity.PurchaseReturn purchaseReturn
            : purchaseReturnRepository.findForReport(businessId, from, to)) {
            Hibernate.initialize(purchaseReturn.getItems());
            Purchase purchase = purchaseReturn.getPurchase();
            for (com.inventory.purchase.entity.PurchaseReturnItem returnItem : purchaseReturn.getItems()) {
                com.inventory.purchase.entity.PurchaseItem purchaseItem = returnItem.getPurchaseItem();
                TaxSlice reversed = reverseTaxShare(
                    nullSafe(purchaseItem.getLineTotal()),
                    nullSafe(returnItem.getLineTotal()),
                    nullSafe(purchaseItem.getTaxableAmount()),
                    nullSafe(purchaseItem.getCgstAmount()),
                    nullSafe(purchaseItem.getSgstAmount()),
                    nullSafe(purchaseItem.getIgstAmount())
                );
                rows.add(new GstReportResponse.Row(
                    "PURCHASE_RETURN",
                    purchaseReturn.getReturnNumber(),
                    purchaseReturn.getReturnDate(),
                    purchase.getSupplier().getName(),
                    purchase.isInterstate(),
                    nullSafe(purchaseItem.getGstRate()),
                    reversed.taxable().negate(),
                    reversed.cgst().negate(),
                    reversed.sgst().negate(),
                    reversed.igst().negate()
                ));
                inputTaxable = inputTaxable.subtract(reversed.taxable());
                inputCgst = inputCgst.subtract(reversed.cgst());
                inputSgst = inputSgst.subtract(reversed.sgst());
                inputIgst = inputIgst.subtract(reversed.igst());
            }
        }

        BigDecimal outputTax = outputCgst.add(outputSgst).add(outputIgst);
        BigDecimal inputTax = inputCgst.add(inputSgst).add(inputIgst);
        BigDecimal netCgst = outputCgst.subtract(inputCgst);
        BigDecimal netSgst = outputSgst.subtract(inputSgst);
        BigDecimal netIgst = outputIgst.subtract(inputIgst);

        return new GstReportResponse(
            OffsetDateTime.now(),
            from,
            to,
            outputTaxable,
            outputCgst,
            outputSgst,
            outputIgst,
            outputTax,
            inputTaxable,
            inputCgst,
            inputSgst,
            inputIgst,
            inputTax,
            netCgst,
            netSgst,
            netIgst,
            netCgst.add(netSgst).add(netIgst),
            rows
        );
    }

    private TaxSlice reverseTaxShare(
        BigDecimal originalLineTotal,
        BigDecimal returnLineTotal,
        BigDecimal taxable,
        BigDecimal cgst,
        BigDecimal sgst,
        BigDecimal igst
    ) {
        if (originalLineTotal.compareTo(BigDecimal.ZERO) <= 0 || returnLineTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return new TaxSlice(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (returnLineTotal.compareTo(originalLineTotal) == 0) {
            return new TaxSlice(taxable, cgst, sgst, igst);
        }
        java.math.RoundingMode halfUp = java.math.RoundingMode.HALF_UP;
        BigDecimal ratio = returnLineTotal.divide(originalLineTotal, 8, halfUp);
        return new TaxSlice(
            taxable.multiply(ratio).setScale(2, halfUp),
            cgst.multiply(ratio).setScale(2, halfUp),
            sgst.multiply(ratio).setScale(2, halfUp),
            igst.multiply(ratio).setScale(2, halfUp)
        );
    }

    private record TaxSlice(BigDecimal taxable, BigDecimal cgst, BigDecimal sgst, BigDecimal igst) {
    }

    private Map<UUID, BigDecimal> returnedAmountsBySale(UUID businessId) {
        Map<UUID, BigDecimal> returnedBySaleId = new HashMap<>();
        for (Object[] row : saleReturnRepository.sumReturnedAmountsGroupedBySale(businessId)) {
            UUID saleId = (UUID) row[0];
            BigDecimal amount = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            returnedBySaleId.put(saleId, amount);
        }
        return returnedBySaleId;
    }

    private Map<UUID, BigDecimal> returnedAmountsByPurchase(UUID businessId) {
        Map<UUID, BigDecimal> returnedByPurchaseId = new HashMap<>();
        for (Object[] row : purchaseReturnRepository.sumReturnedAmountsGroupedByPurchase(businessId)) {
            UUID purchaseId = (UUID) row[0];
            BigDecimal amount = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
            returnedByPurchaseId.put(purchaseId, amount);
        }
        return returnedByPurchaseId;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("'from' date must be on or before 'to' date");
        }
    }

    private UUID requireBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static final class OutstandingAccumulator {
        private final String name;
        private long invoiceCount;
        private BigDecimal billed = BigDecimal.ZERO;
        private BigDecimal paid = BigDecimal.ZERO;
        private BigDecimal outstanding = BigDecimal.ZERO;

        private OutstandingAccumulator(String name) {
            this.name = name;
        }
    }
}
