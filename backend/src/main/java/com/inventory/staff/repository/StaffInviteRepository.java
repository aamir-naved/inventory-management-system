package com.inventory.staff.repository;

import com.inventory.staff.entity.StaffInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffInviteRepository extends JpaRepository<StaffInvite, UUID> {

    List<StaffInvite> findAllByBusiness_IdAndAcceptedAtIsNullOrderByCreatedAtDesc(UUID businessId);

    @Query("select i from StaffInvite i join fetch i.business where i.tokenHash = :tokenHash")
    Optional<StaffInvite> findByTokenHash(@Param("tokenHash") String tokenHash);

    Optional<StaffInvite> findByBusiness_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(
        UUID businessId,
        String email,
        OffsetDateTime now
    );
}
