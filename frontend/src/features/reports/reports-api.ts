import { httpClient } from "@/api/http-client";
import { saveFileDownload } from "@/api/download-client";

export type InventoryReport = {
  generatedAt: string;
  totalProducts: number;
  totalStockValue: number;
  lowStockProducts: number;
  rows: Array<{
    productId: string;
    productName: string;
    sku: string | null;
    currentStock: number;
    costPrice: number;
    sellingPrice: number;
    stockValue: number;
    lowStock: boolean;
  }>;
};

export type SalesReport = {
  generatedAt: string;
  from: string | null;
  to: string | null;
  rowCount: number;
  totalNetAmount: number;
  totalAmountPaid: number;
  totalOutstanding: number;
  rows: Array<{
    saleId: string;
    saleNumber: string;
    saleDate: string;
    customerId: string;
    customerName: string;
    netAmount: number;
    amountPaid: number;
    outstandingAmount: number;
    paymentStatus: string;
  }>;
};

export type PurchaseReport = {
  generatedAt: string;
  from: string | null;
  to: string | null;
  rowCount: number;
  totalAmount: number;
  totalAmountPaid: number;
  totalOutstanding: number;
  rows: Array<{
    purchaseId: string;
    purchaseNumber: string;
    purchaseDate: string;
    supplierId: string;
    supplierName: string;
    totalAmount: number;
    amountPaid: number;
    outstandingAmount: number;
    paymentStatus: string;
  }>;
};

export type CustomerOutstandingReport = {
  generatedAt: string;
  customerCount: number;
  totalOutstanding: number;
  rows: Array<{
    customerId: string;
    customerName: string;
    invoiceCount: number;
    netBilled: number;
    amountPaid: number;
    outstandingAmount: number;
  }>;
};

export type SupplierOutstandingReport = {
  generatedAt: string;
  supplierCount: number;
  totalOutstanding: number;
  rows: Array<{
    supplierId: string;
    supplierName: string;
    billCount: number;
    billedAmount: number;
    amountPaid: number;
    outstandingAmount: number;
  }>;
};

export type ReportDateFilters = {
  from?: string;
  to?: string;
};

function withDateQuery(path: string, filters: ReportDateFilters = {}) {
  const params = new URLSearchParams();
  if (filters.from) {
    params.set("from", filters.from);
  }
  if (filters.to) {
    params.set("to", filters.to);
  }
  const query = params.toString();
  return query ? `${path}?${query}` : path;
}

export async function getInventoryReport(businessId: string, lowStockOnly = false) {
  const query = lowStockOnly ? "?lowStockOnly=true" : "";
  return httpClient<InventoryReport>(`/reports/inventory${query}`, { businessId });
}

export async function getSalesReport(businessId: string, filters: ReportDateFilters = {}) {
  return httpClient<SalesReport>(withDateQuery("/reports/sales", filters), { businessId });
}

export async function getPurchaseReport(businessId: string, filters: ReportDateFilters = {}) {
  return httpClient<PurchaseReport>(withDateQuery("/reports/purchases", filters), {
    businessId,
  });
}

export async function getCustomerOutstandingReport(businessId: string) {
  return httpClient<CustomerOutstandingReport>("/reports/outstanding/customers", {
    businessId,
  });
}

export async function getSupplierOutstandingReport(businessId: string) {
  return httpClient<SupplierOutstandingReport>(`/reports/outstanding/suppliers`, {
    businessId,
  });
}

export type GstReport = {
  generatedAt: string;
  from: string | null;
  to: string | null;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  totalTax: number;
  rows: Array<{
    documentType: string;
    documentNumber: string;
    documentDate: string;
    partyName: string;
    interstate: boolean;
    gstRate: number;
    taxableAmount: number;
    cgstAmount: number;
    sgstAmount: number;
    igstAmount: number;
  }>;
};

export async function getGstReport(businessId: string, filters: ReportDateFilters = {}) {
  return httpClient<GstReport>(withDateQuery("/reports/gst", filters), { businessId });
}

export async function downloadReportExcel(
  businessId: string,
  path: string,
  filename: string,
) {
  return saveFileDownload(
    path,
    businessId,
    filename,
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  );
}
