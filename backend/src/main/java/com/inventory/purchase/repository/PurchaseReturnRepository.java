package com.inventory.purchase.repository;

import com.inventory.purchase.entity.PurchaseReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseReturnRepository extends JpaRepository<PurchaseReturn, UUID> {

    Optional<PurchaseReturn> findByIdAndBusinessId(UUID id, UUID businessId);

    List<PurchaseReturn> findByBusinessIdAndPurchase_IdOrderByReturnDateDescCreatedAtDesc(
        UUID businessId,
        UUID purchaseId
    );

    @Query("""
        select r from PurchaseReturn r
        join fetch r.purchase p
        join fetch p.supplier
        where r.businessId = :businessId
          and (:fromDate is null or r.returnDate >= :fromDate)
          and (:toDate is null or r.returnDate <= :toDate)
        order by r.returnDate desc, r.createdAt desc
        """)
    List<PurchaseReturn> findForReport(
        @Param("businessId") UUID businessId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    boolean existsByBusinessIdAndPurchase_Id(UUID businessId, UUID purchaseId);

    @Query("""
        select coalesce(sum(ri.quantity), 0)
        from PurchaseReturnItem ri
        join ri.purchaseReturn r
        where r.businessId = :businessId
          and ri.purchaseItem.id = :purchaseItemId
        """)
    BigDecimal sumReturnedQuantityForPurchaseItem(UUID businessId, UUID purchaseItemId);

    @Query("""
        select coalesce(sum(ri.lineTotal), 0)
        from PurchaseReturnItem ri
        join ri.purchaseReturn r
        where r.businessId = :businessId
          and ri.purchaseItem.id = :purchaseItemId
        """)
    BigDecimal sumReturnedAmountForPurchaseItem(UUID businessId, UUID purchaseItemId);

    @Query("""
        select coalesce(sum(r.totalAmount), 0)
        from PurchaseReturn r
        where r.businessId = :businessId
          and r.purchase.id = :purchaseId
        """)
    BigDecimal sumReturnedAmountForPurchase(UUID businessId, UUID purchaseId);

    @Query("""
        select r.purchase.id, coalesce(sum(r.totalAmount), 0)
        from PurchaseReturn r
        where r.businessId = :businessId
        group by r.purchase.id
        """)
    List<Object[]> sumReturnedAmountsGroupedByPurchase(UUID businessId);

    @Query("""
        select r from PurchaseReturn r
        join r.purchase p
        join p.supplier s
        where r.businessId = :businessId
          and (
            :searchTerm = ''
            or lower(r.returnNumber) like lower(concat('%', :searchTerm, '%'))
            or lower(p.purchaseNumber) like lower(concat('%', :searchTerm, '%'))
            or lower(s.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(r.reason, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(r.notes, '')) like lower(concat('%', :searchTerm, '%'))
          )
        order by r.returnDate desc, r.createdAt desc
        """)
    List<PurchaseReturn> search(UUID businessId, String searchTerm);
}
