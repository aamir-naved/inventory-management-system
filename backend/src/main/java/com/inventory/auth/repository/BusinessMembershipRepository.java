package com.inventory.auth.repository;

import com.inventory.auth.entity.BusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, UUID> {

    @Query("select m from BusinessMembership m join fetch m.user join fetch m.business where m.business.id = :businessId")
    List<BusinessMembership> findAllByBusiness_Id(@Param("businessId") UUID businessId);

    @Query("select m from BusinessMembership m join fetch m.business where m.user.id = :userId")
    List<BusinessMembership> findAllByUser_Id(@Param("userId") UUID userId);

    Optional<BusinessMembership> findByBusiness_IdAndUser_IdAndActiveTrue(UUID businessId, UUID userId);

    boolean existsByUser_IdAndActiveTrue(UUID userId);

    Optional<BusinessMembership> findFirstByBusiness_IdAndRoleAndActiveTrue(UUID businessId, String role);

    long countByBusiness_IdAndActiveTrue(UUID businessId);

    @Query("select m from BusinessMembership m join fetch m.user join fetch m.business where m.user.id in :userIds and m.active = true")
    List<BusinessMembership> findActiveWithBusinessByUserIds(@Param("userIds") List<UUID> userIds);
}
