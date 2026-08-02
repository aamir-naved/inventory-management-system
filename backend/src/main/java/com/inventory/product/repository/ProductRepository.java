package com.inventory.product.repository;

import com.inventory.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndBusinessId(UUID id, UUID businessId);

    boolean existsByBusinessIdAndSkuIgnoreCaseAndIdNot(UUID businessId, String sku, UUID id);

    boolean existsByBusinessIdAndSkuIgnoreCase(UUID businessId, String sku);

    @Query("""
        select p from Product p
        where p.businessId = :businessId
          and (:includeArchived = true or p.archived = false)
          and (
            :searchTerm = ''
            or lower(p.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.sku, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(p.category, '')) like lower(concat('%', :searchTerm, '%'))
          )
        order by p.archived asc, p.name asc
        """)
    List<Product> search(UUID businessId, String searchTerm, boolean includeArchived);
}
