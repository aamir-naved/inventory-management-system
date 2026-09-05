import { downloadBlob, openPdfInNewTab, savePdfDownload } from "@/api/download-client";

export type ShareInvoiceResult = "shared" | "cancelled" | "downloaded";

function triggerBlobDownload(blob: Blob, filename: string) {
  const objectUrl = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = objectUrl;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000);
}

export async function shareSaleInvoice(
  businessId: string,
  saleId: string,
  saleNumber: string,
): Promise<ShareInvoiceResult> {
  const path = `/sales/${saleId}/invoice.pdf`;
  const fallbackFilename = `${saleNumber}.pdf`;
  const { blob, filename } = await downloadBlob(path, { businessId }, fallbackFilename);
  const file = new File([blob], filename, { type: blob.type || "application/pdf" });
  const title = `Invoice ${saleNumber}`;
  const text = `${title}. Please find the bill.`;

  if (typeof navigator.share === "function") {
    const fileShare: ShareData = { title, text, files: [file] };
    try {
      if (!navigator.canShare || navigator.canShare(fileShare)) {
        await navigator.share(fileShare);
        return "shared";
      }
      await navigator.share({ title, text });
      triggerBlobDownload(blob, filename);
      return "shared";
    } catch (error) {
      if (error instanceof DOMException && error.name === "AbortError") {
        return "cancelled";
      }
      if (error instanceof Error && error.name === "AbortError") {
        return "cancelled";
      }
    }
  }

  triggerBlobDownload(blob, filename);
  const waUrl = `https://wa.me/?text=${encodeURIComponent(`${title} — attach the PDF we just saved.`)}`;
  window.open(waUrl, "_blank", "noopener,noreferrer");
  return "downloaded";
}

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
