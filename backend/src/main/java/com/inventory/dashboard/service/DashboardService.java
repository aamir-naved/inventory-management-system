package com.inventory.dashboard.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.dashboard.dto.DashboardMetricsResponse;
import com.inventory.inventory.dto.InventorySummaryResponse;
import com.inventory.inventory.service.InventoryService;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final InventoryService inventoryService;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;

    public DashboardService(
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

    public DashboardMetricsResponse getMetrics() {
        UUID businessId = requireBusinessId();
        LocalDate today = LocalDate.now();

        InventorySummaryResponse inventorySummary = inventoryService.getSummary();
        List<Sale> sales = saleRepository.findByBusinessIdAndCancelledFalse(businessId);
        List<Purchase> purchases = purchaseRepository.findByBusinessIdAndCancelledFalse(businessId);
        Map<UUID, BigDecimal> returnedBySaleId = returnedAmountsBySale(businessId);
        Map<UUID, BigDecimal> returnedByPurchaseId = returnedAmountsByPurchase(businessId);

        BigDecimal todaysSalesAmount = BigDecimal.ZERO;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal outstandingCustomers = BigDecimal.ZERO;

        for (Sale sale : sales) {
            BigDecimal returned = returnedBySaleId.getOrDefault(sale.getId(), BigDecimal.ZERO);
            BigDecimal netAmount = sale.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = sale.getAmountPaid() == null ? BigDecimal.ZERO : sale.getAmountPaid();

            totalRevenue = totalRevenue.add(netAmount);
            outstandingCustomers = outstandingCustomers.add(PaymentAmounts.outstanding(amountPaid, netAmount));

            if (today.equals(sale.getSaleDate())) {
                todaysSalesAmount = todaysSalesAmount.add(netAmount);
            }
        }

        BigDecimal todaysPurchasesAmount = BigDecimal.ZERO;
        BigDecimal outstandingSuppliers = BigDecimal.ZERO;

        for (Purchase purchase : purchases) {
            BigDecimal returned = returnedByPurchaseId.getOrDefault(purchase.getId(), BigDecimal.ZERO);
            BigDecimal netAmount = purchase.getTotalAmount().subtract(returned);
            BigDecimal amountPaid = purchase.getAmountPaid() == null ? BigDecimal.ZERO : purchase.getAmountPaid();
            outstandingSuppliers = outstandingSuppliers.add(
                PaymentAmounts.outstanding(amountPaid, netAmount)
            );

            if (today.equals(purchase.getPurchaseDate())) {
                todaysPurchasesAmount = todaysPurchasesAmount.add(netAmount);
            }
        }

        return new DashboardMetricsResponse(
            today,
            todaysSalesAmount,
            todaysPurchasesAmount,
            totalRevenue,
            inventorySummary.totalProducts(),
            inventorySummary.totalStockValue(),
            inventorySummary.lowStockProducts(),
            outstandingCustomers,
            outstandingSuppliers
        );
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

    private UUID requireBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalStateException("Business context is required"));
    }
}
