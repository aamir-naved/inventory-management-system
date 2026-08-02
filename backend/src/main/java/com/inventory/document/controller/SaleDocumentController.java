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
@RequestMapping("/sales")
public class SaleDocumentController {

    private final DocumentPdfService documentPdfService;

    public SaleDocumentController(DocumentPdfService documentPdfService) {
        this.documentPdfService = documentPdfService;
    }

    @GetMapping(value = "/{id}/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID id) {
        PdfDocument pdf = documentPdfService.buildSaleInvoice(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + pdf.filename() + "\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf.content());
    }
}
