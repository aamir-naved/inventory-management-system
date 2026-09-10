package com.inventory.purchase.repository;

import com.inventory.purchase.entity.Purchase;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    Optional<Purchase> findByIdAndBusinessId(UUID id, UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Purchase p where p.id = :id and p.businessId = :businessId")
    Optional<Purchase> findByIdAndBusinessIdForUpdate(
        @Param("id") UUID id,
        @Param("businessId") UUID businessId
    );

    List<Purchase> findByBusinessIdAndCancelledFalse(UUID businessId);

    @Query("""
        select p from Purchase p
        join fetch p.supplier
        where p.businessId = :businessId
          and p.supplier.id = :supplierId
        order by p.purchaseDate desc, p.createdAt desc
        """)
    List<Purchase> findByBusinessIdAndSupplierIdOrderByPurchaseDateDescCreatedAtDesc(
        @Param("businessId") UUID businessId,
        @Param("supplierId") UUID supplierId
    );

    @Query("""
        select p from Purchase p
        join fetch p.supplier
        where p.businessId = :businessId
          and p.cancelled = false
          and (:fromDate is null or p.purchaseDate >= :fromDate)
          and (:toDate is null or p.purchaseDate <= :toDate)
        order by p.purchaseDate desc, p.createdAt desc
        """)
    List<Purchase> findForReport(
        @Param("businessId") UUID businessId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    @Query(
        value = """
            select p from Purchase p
            join p.supplier s
            where p.businessId = :businessId
              and (
                :searchTerm = ''
                or lower(p.purchaseNumber) like lower(concat('%', :searchTerm, '%'))
                or lower(s.name) like lower(concat('%', :searchTerm, '%'))
                or lower(coalesce(p.notes, '')) like lower(concat('%', :searchTerm, '%'))
              )
            order by p.purchaseDate desc, p.createdAt desc
            """,
        countQuery = """
            select count(p) from Purchase p
            join p.supplier s
            where p.businessId = :businessId
              and (
                :searchTerm = ''
                or lower(p.purchaseNumber) like lower(concat('%', :searchTerm, '%'))
                or lower(s.name) like lower(concat('%', :searchTerm, '%'))
                or lower(coalesce(p.notes, '')) like lower(concat('%', :searchTerm, '%'))
              )
            """
    )
    Page<Purchase> search(
        @Param("businessId") UUID businessId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );
}
