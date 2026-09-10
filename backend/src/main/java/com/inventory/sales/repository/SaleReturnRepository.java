package com.inventory.sales.repository;

import com.inventory.sales.entity.SaleReturn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleReturnRepository extends JpaRepository<SaleReturn, UUID> {

    Optional<SaleReturn> findByIdAndBusinessId(UUID id, UUID businessId);

    List<SaleReturn> findByBusinessIdAndSale_IdOrderByReturnDateDescCreatedAtDesc(UUID businessId, UUID saleId);

    @Query("""
        select r from SaleReturn r
        join fetch r.sale s
        join fetch s.customer
        where r.businessId = :businessId
          and (:fromDate is null or r.returnDate >= :fromDate)
          and (:toDate is null or r.returnDate <= :toDate)
        order by r.returnDate desc, r.createdAt desc
        """)
    List<SaleReturn> findForReport(
        @Param("businessId") UUID businessId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    boolean existsByBusinessIdAndSale_Id(UUID businessId, UUID saleId);

    @Query("""
        select coalesce(sum(ri.quantity), 0)
        from SaleReturnItem ri
        join ri.saleReturn r
        where r.businessId = :businessId
          and ri.saleItem.id = :saleItemId
        """)
    BigDecimal sumReturnedQuantityForSaleItem(UUID businessId, UUID saleItemId);

    @Query("""
        select coalesce(sum(ri.lineTotal), 0)
        from SaleReturnItem ri
        join ri.saleReturn r
        where r.businessId = :businessId
          and ri.saleItem.id = :saleItemId
        """)
    BigDecimal sumReturnedAmountForSaleItem(UUID businessId, UUID saleItemId);

    @Query("""
        select coalesce(sum(r.totalAmount), 0)
        from SaleReturn r
        where r.businessId = :businessId
          and r.sale.id = :saleId
        """)
    BigDecimal sumReturnedAmountForSale(UUID businessId, UUID saleId);

    @Query("""
        select r.sale.id, coalesce(sum(r.totalAmount), 0)
        from SaleReturn r
        where r.businessId = :businessId
        group by r.sale.id
        """)
    List<Object[]> sumReturnedAmountsGroupedBySale(UUID businessId);

    @Query("""
        select r from SaleReturn r
        join r.sale s
        join s.customer c
        where r.businessId = :businessId
          and (
            :searchTerm = ''
            or lower(r.returnNumber) like lower(concat('%', :searchTerm, '%'))
            or lower(s.saleNumber) like lower(concat('%', :searchTerm, '%'))
            or lower(c.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(r.reason, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(r.notes, '')) like lower(concat('%', :searchTerm, '%'))
          )
        order by r.returnDate desc, r.createdAt desc
        """)
    List<SaleReturn> search(UUID businessId, String searchTerm);
}
