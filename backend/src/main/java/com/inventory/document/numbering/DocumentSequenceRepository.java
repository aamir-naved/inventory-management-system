package com.inventory.document.numbering;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DocumentSequenceRepository extends JpaRepository<DocumentSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s from DocumentSequence s
        where s.businessId = :businessId
          and s.documentType = :documentType
          and s.financialYear = :financialYear
        """)
    Optional<DocumentSequence> findForUpdate(
        @Param("businessId") UUID businessId,
        @Param("documentType") DocumentType documentType,
        @Param("financialYear") String financialYear
    );
}
