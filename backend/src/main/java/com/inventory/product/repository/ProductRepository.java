package com.inventory.product.repository;

import com.inventory.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndBusinessId(UUID id, UUID businessId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.businessId = :businessId")
    Optional<Product> findByIdAndBusinessIdForUpdate(
        @Param("id") UUID id,
        @Param("businessId") UUID businessId
    );

    Optional<Product> findFirstByBusinessIdAndSkuIgnoreCase(UUID businessId, String sku);

    List<Product> findByBusinessIdAndNameIgnoreCaseAndArchivedFalse(UUID businessId, String name);

    boolean existsByBusinessIdAndSkuIgnoreCaseAndIdNot(UUID businessId, String sku, UUID id);

    boolean existsByBusinessIdAndSkuIgnoreCase(UUID businessId, String sku);

    Optional<Product> findFirstByBusinessIdAndBarcodeIgnoreCase(UUID businessId, String barcode);

    boolean existsByBusinessIdAndBarcodeIgnoreCase(UUID businessId, String barcode);

    boolean existsByBusinessIdAndBarcodeIgnoreCaseAndIdNot(UUID businessId, String barcode, UUID id);

    @Query("""
        select p from Product p
        where p.businessId = :businessId
          and (:includeArchived = true or p.archived = false)
          and (
            :searchTerm = ''
            or lower(p.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.sku, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.barcode, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.category, '')) like lower(concat('%', :searchTerm, '%'))
          )
        order by p.archived asc, p.name asc
        """)
    List<Product> searchAll(
        @Param("businessId") UUID businessId,
        @Param("searchTerm") String searchTerm,
        @Param("includeArchived") boolean includeArchived
    );

    @Query("""
        select p from Product p
        where p.businessId = :businessId
          and (:includeArchived = true or p.archived = false)
          and (
            :searchTerm = ''
            or lower(p.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.sku, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.barcode, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.category, '')) like lower(concat('%', :searchTerm, '%'))
          )
          and (
            :lowStockOnly = false
            or (p.lowStockThreshold > 0 and p.currentStock <= p.lowStockThreshold)
          )
        order by p.archived asc, p.name asc
        """)
    Page<Product> search(
        @Param("businessId") UUID businessId,
        @Param("searchTerm") String searchTerm,
        @Param("includeArchived") boolean includeArchived,
        @Param("lowStockOnly") boolean lowStockOnly,
        Pageable pageable
    );
}
