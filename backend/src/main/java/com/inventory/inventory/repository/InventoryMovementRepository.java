package com.inventory.inventory.repository;

import com.inventory.inventory.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    Page<InventoryMovement> findByBusinessIdOrderByCreatedAtDesc(UUID businessId, Pageable pageable);

    Page<InventoryMovement> findByBusinessIdAndProduct_IdOrderByCreatedAtDesc(
        UUID businessId,
        UUID productId,
        Pageable pageable
    );
}
