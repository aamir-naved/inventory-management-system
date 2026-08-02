package com.inventory.customer.repository;

import com.inventory.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("""
        select c from Customer c
        where c.businessId = :businessId
          and (:includeArchived = true or c.archived = false)
          and (
            :searchTerm = ''
            or lower(c.name) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(c.contactPerson, '')) like lower(concat('%', :searchTerm, '%'))
            or lower(coalesce(c.mobileNumber, '')) like lower(concat('%', :searchTerm, '%'))
          )
        order by c.archived asc, c.name asc
        """)
    List<Customer> search(UUID businessId, String searchTerm, boolean includeArchived);
}
