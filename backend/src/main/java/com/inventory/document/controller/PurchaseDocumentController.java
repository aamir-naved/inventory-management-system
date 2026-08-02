package com.inventory.document.controller;

import com.inventory.document.dto.PdfDocument;
import com.inventory.document.service.DocumentPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/purchases")
public class PurchaseDocumentController {

    private final DocumentPdfService documentPdfService;

    public PurchaseDocumentController(DocumentPdfService documentPdfService) {
        this.documentPdfService = documentPdfService;
    }

    @GetMapping(value = "/{id}/bill.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadBill(@PathVariable UUID id) {
        PdfDocument pdf = documentPdfService.buildPurchaseBill(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf.content());
    }
}
