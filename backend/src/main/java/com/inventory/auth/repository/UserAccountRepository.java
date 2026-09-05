package com.inventory.auth.repository;

import com.inventory.auth.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    Optional<UserAccount> findByPhone(String phone);

    long countByPlatformRoleAndActiveTrue(String platformRole);

    @Query(
        value = """
            select u from UserAccount u
            where :search = ''
              or lower(u.fullName) like lower(concat('%', :search, '%'))
              or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
              or lower(coalesce(u.phone, '')) like lower(concat('%', :search, '%'))
            """,
        countQuery = """
            select count(u) from UserAccount u
            where :search = ''
              or lower(u.fullName) like lower(concat('%', :search, '%'))
              or lower(coalesce(u.email, '')) like lower(concat('%', :search, '%'))
              or lower(coalesce(u.phone, '')) like lower(concat('%', :search, '%'))
            """
    )
    Page<UserAccount> search(@Param("search") String search, Pageable pageable);
}
