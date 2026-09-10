package com.inventory.notification.service;

import com.inventory.common.tenant.TenantContext;
import com.inventory.common.time.BusinessClock;
import com.inventory.inventory.dto.InventoryStockResponse;
import com.inventory.inventory.service.InventoryService;
import com.inventory.notification.dto.NotificationItem;
import com.inventory.notification.dto.NotificationListResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final InventoryService inventoryService;
    private final SaleRepository saleRepository;
    private final PurchaseRepository purchaseRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final BusinessClock businessClock;

    public NotificationService(
        InventoryService inventoryService,
        SaleRepository saleRepository,
        PurchaseRepository purchaseRepository,
        SaleReturnRepository saleReturnRepository,
        PurchaseReturnRepository purchaseReturnRepository,
        BusinessClock businessClock
    ) {
        this.inventoryService = inventoryService;
        this.saleRepository = saleRepository;
        this.purchaseRepository = purchaseRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.businessClock = businessClock;
    }

    public NotificationListResponse list() {
        UUID businessId = TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
        List<NotificationItem> items = new ArrayList<>();

        List<InventoryStockResponse> lowStock = inventoryService.listStockAll("", true, false);
        lowStock.stream().limit(15).forEach(stock -> items.add(new NotificationItem(
            "LOW_STOCK",
            stock.productName() + " is low",
            "Stock " + stock.currentStock().stripTrailingZeros().toPlainString()
                + " is at or below the reorder level.",
            stock.productId()
        )));

        LocalDate cutoff = businessClock.today().minusDays(7);
        for (Sale sale : saleRepository.findByBusinessIdAndCancelledFalse(businessId)) {
            if (sale.getSaleDate().isAfter(cutoff)) {
                continue;
            }
            BigDecimal returned = saleReturnRepository.sumReturnedAmountForSale(businessId, sale.getId());
            BigDecimal net = sale.getTotalAmount().subtract(returned == null ? BigDecimal.ZERO : returned);
            BigDecimal outstanding = PaymentAmounts.outstanding(sale.getAmountPaid(), net);
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                items.add(new NotificationItem(
                    "OVERDUE_SALE",
                    "Payment due from " + sale.getCustomer().getName(),
                    sale.getSaleNumber() + " has outstanding " + outstanding.toPlainString(),
                    sale.getId()
                ));
            }
            if (items.size() >= 40) {
                break;
            }
        }

        for (Purchase purchase : purchaseRepository.findByBusinessIdAndCancelledFalse(businessId)) {
            if (purchase.getPurchaseDate().isAfter(cutoff)) {
                continue;
            }
            BigDecimal returned = purchaseReturnRepository.sumReturnedAmountForPurchase(businessId, purchase.getId());
            BigDecimal net = purchase.getTotalAmount().subtract(returned == null ? BigDecimal.ZERO : returned);
            BigDecimal outstanding = PaymentAmounts.outstanding(purchase.getAmountPaid(), net);
            if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
                items.add(new NotificationItem(
                    "OVERDUE_PURCHASE",
                    "Payment due to " + purchase.getSupplier().getName(),
                    purchase.getPurchaseNumber() + " has outstanding " + outstanding.toPlainString(),
                    purchase.getId()
                ));
            }
            if (items.size() >= 50) {
                break;
            }
        }

        return new NotificationListResponse(items.size(), List.copyOf(items));
    }
}
