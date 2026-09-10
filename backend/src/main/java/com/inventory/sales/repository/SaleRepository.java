package com.inventory.sales.repository;

import com.inventory.sales.entity.Sale;
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

public interface SaleRepository extends JpaRepository<Sale, UUID> {
    Optional<Sale> findByIdAndBusinessId(UUID id, UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sale s where s.id = :id and s.businessId = :businessId")
    Optional<Sale> findByIdAndBusinessIdForUpdate(
        @Param("id") UUID id,
        @Param("businessId") UUID businessId
    );

    List<Sale> findByBusinessIdAndCancelledFalse(UUID businessId);

    @Query("""
        select s from Sale s
        join fetch s.customer
        where s.businessId = :businessId
          and s.customer.id = :customerId
        order by s.saleDate desc, s.createdAt desc
        """)
    List<Sale> findByBusinessIdAndCustomerIdOrderBySaleDateDescCreatedAtDesc(
        @Param("businessId") UUID businessId,
        @Param("customerId") UUID customerId
    );

    @Query("""
        select s from Sale s
        join fetch s.customer
        where s.businessId = :businessId
          and s.cancelled = false
          and (:fromDate is null or s.saleDate >= :fromDate)
          and (:toDate is null or s.saleDate <= :toDate)
        order by s.saleDate desc, s.createdAt desc
        """)
    List<Sale> findForReport(
        @Param("businessId") UUID businessId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    @Query(
        value = """
            select s from Sale s
            join s.customer c
            where s.businessId = :businessId
              and (
                :searchTerm = ''
                or lower(s.saleNumber) like lower(concat('%', :searchTerm, '%'))
                or lower(c.name) like lower(concat('%', :searchTerm, '%'))
                or lower(coalesce(s.notes, '')) like lower(concat('%', :searchTerm, '%'))
              )
            order by s.saleDate desc, s.createdAt desc
            """,
        countQuery = """
            select count(s) from Sale s
            join s.customer c
            where s.businessId = :businessId
              and (
                :searchTerm = ''
                or lower(s.saleNumber) like lower(concat('%', :searchTerm, '%'))
                or lower(c.name) like lower(concat('%', :searchTerm, '%'))
                or lower(coalesce(s.notes, '')) like lower(concat('%', :searchTerm, '%'))
              )
            """
    )
    Page<Sale> search(
        @Param("businessId") UUID businessId,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );

    @Query("select count(s) from Sale s where s.cancelled = false and s.saleDate = :saleDate")
    long countActiveBySaleDate(@Param("saleDate") LocalDate saleDate);

    @Query("select coalesce(sum(s.totalAmount), 0) from Sale s where s.cancelled = false and s.saleDate = :saleDate")
    java.math.BigDecimal sumTotalBySaleDate(@Param("saleDate") LocalDate saleDate);
}
