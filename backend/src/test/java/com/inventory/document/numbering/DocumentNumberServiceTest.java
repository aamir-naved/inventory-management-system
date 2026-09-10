package com.inventory.document.numbering;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentNumberServiceTest {

    @Test
    void indianFinancialYearUsesAprilBoundary() {
        assertEquals("2025-26", DocumentNumberService.indianFinancialYear(LocalDate.of(2025, 4, 1)));
        assertEquals("2025-26", DocumentNumberService.indianFinancialYear(LocalDate.of(2026, 3, 31)));
        assertEquals("2026-27", DocumentNumberService.indianFinancialYear(LocalDate.of(2026, 4, 1)));
        assertEquals("2024-25", DocumentNumberService.indianFinancialYear(LocalDate.of(2025, 3, 31)));
    }

    @Test
    void formatsPrefixedSerial() {
        assertEquals(
            "SAL/2025-26/000001",
            DocumentNumberService.format(DocumentType.SALE, "2025-26", 1)
        );
        assertEquals(
            "PUR/2026-27/000042",
            DocumentNumberService.format(DocumentType.PURCHASE, "2026-27", 42)
        );
    }
}
