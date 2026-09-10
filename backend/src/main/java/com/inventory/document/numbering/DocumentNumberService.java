package com.inventory.document.numbering;

import com.inventory.business.repository.BusinessRepository;
import com.inventory.common.time.BusinessClock;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class DocumentNumberService {

    private static final int MAX_ALLOCATE_ATTEMPTS = 5;

    private final DocumentSequenceRepository documentSequenceRepository;
    private final BusinessRepository businessRepository;
    private final BusinessClock businessClock;

    public DocumentNumberService(
        DocumentSequenceRepository documentSequenceRepository,
        BusinessRepository businessRepository,
        BusinessClock businessClock
    ) {
        this.documentSequenceRepository = documentSequenceRepository;
        this.businessRepository = businessRepository;
        this.businessClock = businessClock;
    }

    @Transactional
    public String next(UUID businessId, DocumentType documentType) {
        if (businessRepository.findById(businessId).isEmpty()) {
            throw new EntityNotFoundException("Business not found");
        }
        String financialYear = indianFinancialYear(businessClock.today());

        for (int attempt = 0; attempt < MAX_ALLOCATE_ATTEMPTS; attempt++) {
            var locked = documentSequenceRepository.findForUpdate(businessId, documentType, financialYear);
            if (locked.isPresent()) {
                DocumentSequence sequence = locked.get();
                long nextValue = sequence.getLastValue() + 1;
                sequence.setLastValue(nextValue);
                return format(documentType, financialYear, nextValue);
            }

            try {
                DocumentSequence created = new DocumentSequence();
                created.setBusinessId(businessId);
                created.setDocumentType(documentType);
                created.setFinancialYear(financialYear);
                created.setLastValue(0L);
                documentSequenceRepository.saveAndFlush(created);
            } catch (DataIntegrityViolationException ignored) {
                // Another transaction created the row; retry with a lock.
            }
        }

        throw new IllegalStateException(
            "Could not allocate " + documentType.name() + " document number for business " + businessId
        );
    }

    /**
     * Indian financial year label for CGST Rule 46(b) serials: 1 April – 31 March.
     * Example: 15 May 2026 → {@code 2026-27}; 10 Feb 2026 → {@code 2025-26}.
     */
    static String indianFinancialYear(LocalDate date) {
        int startYear = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        int endYearShort = (startYear + 1) % 100;
        return startYear + "-" + String.format("%02d", endYearShort);
    }

    static String format(DocumentType documentType, String financialYear, long serial) {
        return documentType.prefix() + "/" + financialYear + "/" + String.format("%06d", serial);
    }
}
