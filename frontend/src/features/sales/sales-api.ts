import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";

export type SaleItemPayload = {
  productId: string;
  quantity: number;
  sellingPrice: number;
  gstRate?: number;
};

export type SalePayload = {
  customerId: string;
  saleDate: string;
  amountPaid: number;
  notes: string;
  interstate?: boolean;
  items: SaleItemPayload[];
};

export type SaleUpdatePayload = {
  saleDate: string;
  notes: string;
};

export type SaleItemRecord = {
  id: string;
  productId: string;
  productName: string;
  unit: string;
  quantity: number;
  sellingPrice: number;
  lineTotal: number;
  hsnCode: string | null;
  gstRate: number;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  returnedQuantity: number;
  returnableQuantity: number;
};

export type SaleRecord = {
  id: string;
  businessId: string;
  saleNumber: string;
  customerId: string;
  customerName: string;
  saleDate: string;
  paymentStatus: "PENDING" | "PARTIAL" | "PAID";
  notes: string | null;
  totalAmount: number;
  returnedAmount: number;
  netAmount: number;
  amountPaid: number;
  outstandingAmount: number;
  cancelled: boolean;
  cancellationReason: string | null;
  hasReturns: boolean;
  interstate: boolean;
  taxableAmount: number;
  cgstAmount: number;
  sgstAmount: number;
  igstAmount: number;
  items: SaleItemRecord[];
  createdAt: string;
  updatedAt: string;
};

export type SaleReturnItemPayload = {
  saleItemId: string;
  quantity: number;
};

export type SaleReturnPayload = {
  returnDate: string;
  reason: string;
  notes: string;
  items: SaleReturnItemPayload[];
};

export type SaleReturnRecord = {
  id: string;
  businessId: string;
  saleId: string;
  saleNumber: string;
  customerName: string;
  returnNumber: string;
  returnDate: string;
  reason: string | null;
  notes: string | null;
  totalAmount: number;
  items: Array<{
    id: string;
    saleItemId: string;
    productId: string;
    productName: string;
    unit: string;
    quantity: number;
    unitPrice: number;
    lineTotal: number;
  }>;
  createdAt: string;
  updatedAt: string;
};

export async function listSales(businessId: string, search?: string, paging: PageRequest = {}) {
  const params = withPaging(new URLSearchParams(), paging);
  if (search?.trim()) {
    params.set("search", search.trim());
  }

  return httpClient<PagedResult<SaleRecord>>(`/sales?${params.toString()}`, {
    businessId,
  });
}

export async function createSale(businessId: string, payload: SalePayload) {
  return httpClient<SaleRecord>("/sales", {
    method: "POST",
    businessId,
    body: payload,
  });
}

export async function getSale(businessId: string, saleId: string) {
  return httpClient<SaleRecord>(`/sales/${saleId}`, {
    businessId,
  });
}

export async function updateSale(
  businessId: string,
  saleId: string,
  payload: SaleUpdatePayload,
) {
  return httpClient<SaleRecord>(`/sales/${saleId}`, {
    method: "PATCH",
    businessId,
    body: payload,
  });
}

export async function cancelSale(
  businessId: string,
  saleId: string,
  reason: string,
) {
  return httpClient<SaleRecord>(`/sales/${saleId}/cancel`, {
    method: "PATCH",
    businessId,
    body: { reason },
  });
}

export async function listSaleReturns(businessId: string, saleId: string) {
  return httpClient<SaleReturnRecord[]>(`/sales/${saleId}/returns`, {
    businessId,
  });
}

export async function createSaleReturn(
  businessId: string,
  saleId: string,
  payload: SaleReturnPayload,
) {
  return httpClient<SaleReturnRecord>(`/sales/${saleId}/returns`, {
    method: "POST",
    businessId,
    body: payload,
  });
}
