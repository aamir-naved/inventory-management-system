import { httpClient } from "@/api/http-client";

export type SaleItemPayload = {
  productId: string;
  quantity: number;
  sellingPrice: number;
};

export type SalePayload = {
  customerId: string;
  saleDate: string;
  amountPaid: number;
  notes: string;
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

export async function listSales(businessId: string, search?: string) {
  const params = new URLSearchParams();
  if (search?.trim()) {
    params.set("search", search.trim());
  }

  const query = params.toString();
  return httpClient<SaleRecord[]>(`/sales${query ? `?${query}` : ""}`, {
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
