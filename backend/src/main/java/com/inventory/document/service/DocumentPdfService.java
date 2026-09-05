package com.inventory.document.service;

import com.inventory.business.entity.Business;
import com.inventory.business.repository.BusinessRepository;
import com.inventory.common.tenant.TenantContext;
import com.inventory.document.dto.PdfDocument;
import com.inventory.payment.support.PaymentAmounts;
import com.inventory.purchase.entity.Purchase;
import com.inventory.purchase.entity.PurchaseItem;
import com.inventory.purchase.repository.PurchaseRepository;
import com.inventory.purchase.repository.PurchaseReturnRepository;
import com.inventory.sales.entity.Sale;
import com.inventory.sales.entity.SaleItem;
import com.inventory.sales.repository.SaleRepository;
import com.inventory.sales.repository.SaleReturnRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class DocumentPdfService {

    private final BusinessRepository businessRepository;
    private final SaleRepository saleRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;

    public DocumentPdfService(
        BusinessRepository businessRepository,
        SaleRepository saleRepository,
        SaleReturnRepository saleReturnRepository,
        PurchaseRepository purchaseRepository,
        PurchaseReturnRepository purchaseReturnRepository
    ) {
        this.businessRepository = businessRepository;
        this.saleRepository = saleRepository;
        this.saleReturnRepository = saleReturnRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
    }

    @Transactional(readOnly = true)
    public PdfDocument buildSaleInvoice(UUID saleId) {
        UUID businessId = requireBusinessId();
        Business business = findBusiness(businessId);
        Sale sale = saleRepository.findByIdAndBusinessId(saleId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        Hibernate.initialize(sale.getCustomer());
        Hibernate.initialize(sale.getItems());
        sale.getItems().forEach(item -> Hibernate.initialize(item.getProduct()));

        BigDecimal returnedAmount = nullSafe(saleReturnRepository.sumReturnedAmountForSale(businessId, sale.getId()));
        BigDecimal netAmount = sale.getTotalAmount().subtract(returnedAmount);
        BigDecimal amountPaid = nullSafe(sale.getAmountPaid());
        BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, netAmount);
        String paymentStatus = PaymentAmounts.deriveStatus(amountPaid, netAmount);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, output);
            document.open();

            addBusinessHeader(document, business);
            addTitle(document, "INVOICE");
            if (sale.isCancelled()) {
                addBanner(document, "CANCELLED" + (sale.getCancellationReason() != null
                    ? " — " + sale.getCancellationReason()
                    : ""));
            }

            addMetaTable(
                document,
                "Invoice number",
                sale.getSaleNumber(),
                "Invoice date",
                formatDate(sale.getSaleDate(), business.getDateFormat()),
                "Bill to",
                sale.getCustomer().getName(),
                "Payment status",
                paymentStatus
            );

            PdfPTable items = business.isGstEnabled() ? newGstItemTable() : newItemTable();
            if (business.isGstEnabled()) {
                addHeaderRow(items, "Product", "HSN", "Qty", "Price", "Taxable", "Tax", "Line total");
                for (SaleItem item : sale.getItems()) {
                    addItemRow(
                        items,
                        item.getProduct().getName(),
                        blankToDash(item.getHsnCode()),
                        formatQty(item.getQuantity()),
                        formatMoney(item.getUnitPrice(), business.getCurrencyCode()),
                        formatMoney(item.getTaxableAmount(), business.getCurrencyCode()),
                        formatMoney(item.getCgstAmount().add(item.getSgstAmount()).add(item.getIgstAmount()), business.getCurrencyCode()),
                        formatMoney(item.getLineTotal(), business.getCurrencyCode())
                    );
                }
            } else {
                addHeaderRow(items, "Product", "Unit", "Qty", "Price", "Line total");
                for (SaleItem item : sale.getItems()) {
                    addItemRow(
                        items,
                        item.getProduct().getName(),
                        item.getProduct().getUnit(),
                        formatQty(item.getQuantity()),
                        formatMoney(item.getUnitPrice(), business.getCurrencyCode()),
                        formatMoney(item.getLineTotal(), business.getCurrencyCode())
                    );
                }
            }
            document.add(items);
            document.add(spacer());

            addTotals(
                document,
                business,
                sale.getTotalAmount(),
                sale.getTaxableAmount(),
                sale.getCgstAmount(),
                sale.getSgstAmount(),
                sale.getIgstAmount(),
                returnedAmount,
                netAmount,
                amountPaid,
                outstanding
            );

            if (sale.getNotes() != null && !sale.getNotes().isBlank()) {
                document.add(spacer());
                document.add(new Paragraph("Notes: " + sale.getNotes(), bodyFont()));
            }

            document.close();
            return new PdfDocument(
                "invoice-" + sanitizeFilename(sale.getSaleNumber()) + ".pdf",
                output.toByteArray()
            );
        } catch (DocumentException exception) {
            throw new IllegalStateException("Unable to generate sale invoice", exception);
        }
    }

    @Transactional(readOnly = true)
    public PdfDocument buildPurchaseBill(UUID purchaseId) {
        UUID businessId = requireBusinessId();
        Business business = findBusiness(businessId);
        Purchase purchase = purchaseRepository.findByIdAndBusinessId(purchaseId, businessId)
            .orElseThrow(() -> new EntityNotFoundException("Purchase not found"));
        Hibernate.initialize(purchase.getSupplier());
        Hibernate.initialize(purchase.getItems());
        purchase.getItems().forEach(item -> Hibernate.initialize(item.getProduct()));

        BigDecimal returnedAmount = nullSafe(
            purchaseReturnRepository.sumReturnedAmountForPurchase(businessId, purchase.getId())
        );
        BigDecimal netAmount = purchase.getTotalAmount().subtract(returnedAmount);
        BigDecimal amountPaid = nullSafe(purchase.getAmountPaid());
        BigDecimal outstanding = PaymentAmounts.outstanding(amountPaid, netAmount);
        String paymentStatus = PaymentAmounts.deriveStatus(amountPaid, netAmount);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, output);
            document.open();

            addBusinessHeader(document, business);
            addTitle(document, "PURCHASE BILL");
            if (purchase.isCancelled()) {
                addBanner(document, "CANCELLED" + (purchase.getCancellationReason() != null
                    ? " — " + purchase.getCancellationReason()
                    : ""));
            }

            addMetaTable(
                document,
                "Bill number",
                purchase.getPurchaseNumber(),
                "Bill date",
                formatDate(purchase.getPurchaseDate(), business.getDateFormat()),
                "Supplier",
                purchase.getSupplier().getName(),
                "Payment status",
                paymentStatus
            );

            PdfPTable items = business.isGstEnabled() ? newGstItemTable() : newItemTable();
            if (business.isGstEnabled()) {
                addHeaderRow(items, "Product", "HSN", "Qty", "Cost", "Taxable", "Tax", "Line total");
                for (PurchaseItem item : purchase.getItems()) {
                    addItemRow(
                        items,
                        item.getProduct().getName(),
                        blankToDash(item.getHsnCode()),
                        formatQty(item.getQuantity()),
                        formatMoney(item.getUnitCost(), business.getCurrencyCode()),
                        formatMoney(item.getTaxableAmount(), business.getCurrencyCode()),
                        formatMoney(item.getCgstAmount().add(item.getSgstAmount()).add(item.getIgstAmount()), business.getCurrencyCode()),
                        formatMoney(item.getLineTotal(), business.getCurrencyCode())
                    );
                }
            } else {
                addHeaderRow(items, "Product", "Unit", "Qty", "Cost", "Line total");
                for (PurchaseItem item : purchase.getItems()) {
                    addItemRow(
                        items,
                        item.getProduct().getName(),
                        item.getProduct().getUnit(),
                        formatQty(item.getQuantity()),
                        formatMoney(item.getUnitCost(), business.getCurrencyCode()),
                        formatMoney(item.getLineTotal(), business.getCurrencyCode())
                    );
                }
            }
            document.add(items);
            document.add(spacer());

            addTotals(
                document,
                business,
                purchase.getTotalAmount(),
                purchase.getTaxableAmount(),
                purchase.getCgstAmount(),
                purchase.getSgstAmount(),
                purchase.getIgstAmount(),
                returnedAmount,
                netAmount,
                amountPaid,
                outstanding
            );

            if (purchase.getNotes() != null && !purchase.getNotes().isBlank()) {
                document.add(spacer());
                document.add(new Paragraph("Notes: " + purchase.getNotes(), bodyFont()));
            }

            document.close();
            return new PdfDocument(
                "purchase-bill-" + sanitizeFilename(purchase.getPurchaseNumber()) + ".pdf",
                output.toByteArray()
            );
        } catch (DocumentException exception) {
            throw new IllegalStateException("Unable to generate purchase bill", exception);
        }
    }

    private void addBusinessHeader(Document document, Business business) throws DocumentException {
        if (business.getLogoBytes() != null && business.getLogoBytes().length > 0) {
            try {
                Image logo = Image.getInstance(business.getLogoBytes());
                logo.scaleToFit(120, 48);
                logo.setAlignment(Element.ALIGN_LEFT);
                document.add(logo);
            } catch (Exception ignored) {
                // Skip a corrupt logo rather than failing the invoice.
            }
        }

        Paragraph name = new Paragraph(business.getName(), titleFont());
        name.setAlignment(Element.ALIGN_LEFT);
        document.add(name);

        if (business.getAddressLine() != null && !business.getAddressLine().isBlank()) {
            document.add(new Paragraph(business.getAddressLine(), bodyFont()));
        }
        document.add(new Paragraph("Mobile: " + business.getMobileNumber(), bodyFont()));
        if (business.isGstEnabled() && business.getGstin() != null && !business.getGstin().isBlank()) {
            document.add(new Paragraph("GSTIN: " + business.getGstin(), bodyFont()));
        }
        if (business.getStateName() != null && !business.getStateName().isBlank()) {
            document.add(new Paragraph("State: " + business.getStateName(), mutedFont()));
        }
        document.add(new Paragraph("Currency: " + business.getCurrencyCode(), mutedFont()));
        document.add(spacer());
    }

    private void addTitle(Document document, String title) throws DocumentException {
        Paragraph heading = new Paragraph(title, headingFont());
        heading.setAlignment(Element.ALIGN_CENTER);
        heading.setSpacingAfter(12f);
        document.add(heading);
    }

    private void addBanner(Document document, String text) throws DocumentException {
        Paragraph banner = new Paragraph(text, bannerFont());
        banner.setAlignment(Element.ALIGN_CENTER);
        banner.setSpacingAfter(10f);
        document.add(banner);
    }

    private void addMetaTable(
        Document document,
        String label1,
        String value1,
        String label2,
        String value2,
        String label3,
        String value3,
        String label4,
        String value4
    ) throws DocumentException {
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setSpacingAfter(14f);
        meta.setWidths(new float[]{1.2f, 2.8f});
        addMetaRow(meta, label1, value1);
        addMetaRow(meta, label2, value2);
        addMetaRow(meta, label3, value3);
        addMetaRow(meta, label4, value4);
        document.add(meta);
    }

    private void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, mutedFont()));
        labelCell.setBorder(0);
        labelCell.setPadding(4f);
        PdfPCell valueCell = new PdfPCell(new Phrase(value == null ? "-" : value, bodyFont()));
        valueCell.setBorder(0);
        valueCell.setPadding(4f);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private PdfPTable newItemTable() throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3.2f, 1.2f, 1.2f, 1.5f, 1.8f});
        return table;
    }

    private void addHeaderRow(PdfPTable table, String... headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, smallBoldFont()));
            cell.setBackgroundColor(new Color(245, 245, 245));
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    private void addItemRow(PdfPTable table, String... values) {
        for (String value : values) {
            PdfPCell cell = new PdfPCell(new Phrase(value, smallFont()));
            cell.setPadding(6f);
            table.addCell(cell);
        }
    }

    private PdfPTable newGstItemTable() throws DocumentException {
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.6f, 1.0f, 0.9f, 1.2f, 1.3f, 1.1f, 1.4f});
        return table;
    }

    private void addTotals(
        Document document,
        Business business,
        BigDecimal totalAmount,
        BigDecimal taxableAmount,
        BigDecimal cgstAmount,
        BigDecimal sgstAmount,
        BigDecimal igstAmount,
        BigDecimal returnedAmount,
        BigDecimal netAmount,
        BigDecimal amountPaid,
        BigDecimal outstanding
    ) throws DocumentException {
        String currencyCode = business.getCurrencyCode();
        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(50);
        totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totals.setWidths(new float[]{1.4f, 1.4f});
        if (business.isGstEnabled()) {
            addTotalRow(totals, "Taxable", formatMoney(taxableAmount, currencyCode));
            addTotalRow(totals, "CGST", formatMoney(cgstAmount, currencyCode));
            addTotalRow(totals, "SGST", formatMoney(sgstAmount, currencyCode));
            addTotalRow(totals, "IGST", formatMoney(igstAmount, currencyCode));
        }
        addTotalRow(totals, "Total", formatMoney(totalAmount, currencyCode));
        addTotalRow(totals, "Returned", formatMoney(returnedAmount, currencyCode));
        addTotalRow(totals, "Net", formatMoney(netAmount, currencyCode));
        addTotalRow(totals, "Paid", formatMoney(amountPaid, currencyCode));
        addTotalRow(totals, "Outstanding", formatMoney(outstanding, currencyCode));
        document.add(totals);
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void addTotalRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, smallBoldFont()));
        labelCell.setBorder(0);
        labelCell.setPadding(4f);
        PdfPCell valueCell = new PdfPCell(new Phrase(value, smallFont()));
        valueCell.setBorder(0);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4f);
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private Paragraph spacer() {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private Business findBusiness(UUID businessId) {
        return businessRepository.findById(businessId)
            .orElseThrow(() -> new EntityNotFoundException("Business not found"));
    }

    private UUID requireBusinessId() {
        return TenantContext.getBusinessId()
            .orElseThrow(() -> new IllegalArgumentException("X-Business-Id header is required"));
    }

    private String formatDate(LocalDate date, String dateFormat) {
        String pattern = dateFormat == null || dateFormat.isBlank() ? "dd/MM/yyyy" : dateFormat;
        return date.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    private String formatMoney(BigDecimal amount, String currencyCode) {
        BigDecimal value = nullSafe(amount).setScale(2, RoundingMode.HALF_UP);
        return currencyCode + " " + value.toPlainString();
    }

    private String formatQty(BigDecimal quantity) {
        return nullSafe(quantity).stripTrailingZeros().toPlainString();
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String sanitizeFilename(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private Font titleFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    }

    private Font headingFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    }

    private Font bodyFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 10);
    }

    private Font mutedFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
    }

    private Font smallFont() {
        return FontFactory.getFont(FontFactory.HELVETICA, 9);
    }

    private Font smallBoldFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    }

    private Font bannerFont() {
        return FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Font.NORMAL, new Color(161, 73, 73));
    }
}
