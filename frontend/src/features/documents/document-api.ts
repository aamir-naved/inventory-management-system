import { openPdfInNewTab, savePdfDownload } from "@/api/download-client";

export async function downloadSaleInvoice(businessId: string, saleId: string) {
  return savePdfDownload(
    `/sales/${saleId}/invoice.pdf`,
    businessId,
    `invoice-${saleId}.pdf`,
  );
}

export async function printSaleInvoice(businessId: string, saleId: string) {
  return openPdfInNewTab(
    `/sales/${saleId}/invoice.pdf`,
    businessId,
    `invoice-${saleId}.pdf`,
  );
}

export async function downloadPurchaseBill(businessId: string, purchaseId: string) {
  return savePdfDownload(
    `/purchases/${purchaseId}/bill.pdf`,
    businessId,
    `purchase-bill-${purchaseId}.pdf`,
  );
}

export async function printPurchaseBill(businessId: string, purchaseId: string) {
  return openPdfInNewTab(
    `/purchases/${purchaseId}/bill.pdf`,
    businessId,
    `purchase-bill-${purchaseId}.pdf`,
  );
}
