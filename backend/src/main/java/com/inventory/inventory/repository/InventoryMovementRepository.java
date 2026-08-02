package com.inventory.inventory.repository;

import com.inventory.inventory.entity.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {

    List<InventoryMovement> findTop100ByBusinessIdOrderByCreatedAtDesc(UUID businessId);

    List<InventoryMovement> findTop100ByBusinessIdAndProduct_IdOrderByCreatedAtDesc(UUID businessId, UUID productId);
}
