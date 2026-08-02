package com.inventory.purchase.repository;

import com.inventory.purchase.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {

    Optional<Purchase> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("""
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
        """)
    List<Purchase> search(UUID businessId, String searchTerm);
}
