package com.inventory.auth.repository;

import com.inventory.auth.entity.BusinessMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, UUID> {

    List<BusinessMembership> findAllByBusiness_Id(UUID businessId);

    List<BusinessMembership> findAllByUser_Id(UUID userId);

    Optional<BusinessMembership> findByBusiness_IdAndUser_IdAndActiveTrue(UUID businessId, UUID userId);

    boolean existsByUser_IdAndActiveTrue(UUID userId);
}
