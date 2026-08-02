package com.inventory.sales.repository;

import com.inventory.sales.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
    Optional<Sale> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("""
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
        """)
    List<Sale> search(UUID businessId, String searchTerm);
}
