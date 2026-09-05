package com.inventory.business.repository;

import com.inventory.business.entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BusinessRepository extends JpaRepository<Business, UUID> {

    long countByActiveTrue();

    long countByActiveFalse();

    @Query(
        value = """
            select b from Business b
            where (:active is null or b.active = :active)
              and (
                :search = ''
                or lower(b.name) like lower(concat('%', :search, '%'))
                or lower(b.mobileNumber) like lower(concat('%', :search, '%'))
              )
            """,
        countQuery = """
            select count(b) from Business b
            where (:active is null or b.active = :active)
              and (
                :search = ''
                or lower(b.name) like lower(concat('%', :search, '%'))
                or lower(b.mobileNumber) like lower(concat('%', :search, '%'))
              )
            """
    )
    Page<Business> search(
        @Param("active") Boolean active,
        @Param("search") String search,
        Pageable pageable
    );
}
