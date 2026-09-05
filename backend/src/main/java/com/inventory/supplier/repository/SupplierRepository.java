package com.inventory.supplier.repository;

import com.inventory.supplier.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("""
        select s from Supplier s
        where s.businessId = :businessId
          and (:includeArchived = true or s.archived = false)
          and (
            :searchTerm = ''
            or lower(s.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(s.contactPerson, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(s.mobileNumber, '')) like lower(concat('%', :searchTerm, '%'))
          )
        order by s.archived asc, s.name asc
        """)
    Page<Supplier> search(
        @Param("businessId") UUID businessId,
        @Param("searchTerm") String searchTerm,
        @Param("includeArchived") boolean includeArchived,
        Pageable pageable
    );
}
