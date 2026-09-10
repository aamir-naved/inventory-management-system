import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";

export type PurchaseItemPayload = {
  productId: string;
  quantity: number;
  purchasePrice: number;
  gstRate?: number;
};

export type PurchasePayload = {
  supplierId: string;
  purchaseDate: string;
  amountPaid: number;
  notes: string;
  items: PurchaseItemPayload[];
};

export type PurchaseUpdatePayload = {
  purchaseDate: string;
  notes: string;
};

export type PurchaseItemRecord = {
  id: string;
  productId: string;
  productName: string;
  unit: string;
  quantity: number;
  purchasePrice: number;
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

export type PurchaseRecord = {
  id: string;
  businessId: string;
  purchaseNumber: string;
  supplierId: string;
  supplierName: string;
  purchaseDate: string;
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
  items: PurchaseItemRecord[];
  createdAt: string;
  updatedAt: string;
};

export type PurchaseReturnItemPayload = {
  purchaseItemId: string;
  quantity: number;
};

export type PurchaseReturnPayload = {
  returnDate: string;
  reason: string;
  notes: string;
  items: PurchaseReturnItemPayload[];
};

export type PurchaseReturnRecord = {
  id: string;
  businessId: string;
  purchaseId: string;
  purchaseNumber: string;
  supplierName: string;
  returnNumber: string;
  returnDate: string;
  reason: string | null;
  notes: string | null;
  totalAmount: number;
  items: Array<{
    id: string;
    purchaseItemId: string;
    productId: string;
    productName: string;
    unit: string;
    quantity: number;
    unitCost: number;
    lineTotal: number;
  }>;
  createdAt: string;
  updatedAt: string;
};

export async function listPurchases(
  businessId: string,
  search?: string,
  paging: PageRequest = {},
) {
  const params = withPaging(new URLSearchParams(), paging);
  if (search?.trim()) {
    params.set("search", search.trim());
  }

  return httpClient<PagedResult<PurchaseRecord>>(`/purchases?${params.toString()}`, {
    businessId,
  });
}

export async function createPurchase(businessId: string, payload: PurchasePayload) {
  return httpClient<PurchaseRecord>("/purchases", {
    method: "POST",
    businessId,
    body: payload,
  });
}

export async function getPurchase(businessId: string, purchaseId: string) {
  return httpClient<PurchaseRecord>(`/purchases/${purchaseId}`, {
    businessId,
  });
}

export async function updatePurchase(
  businessId: string,
  purchaseId: string,
  payload: PurchaseUpdatePayload,
) {
  return httpClient<PurchaseRecord>(`/purchases/${purchaseId}`, {
    method: "PATCH",
    businessId,
    body: payload,
  });
}

export async function cancelPurchase(
  businessId: string,
  purchaseId: string,
  reason: string,
) {
  return httpClient<PurchaseRecord>(`/purchases/${purchaseId}/cancel`, {
    method: "PATCH",
    businessId,
    body: { reason },
  });
}

export async function listPurchaseReturns(businessId: string, purchaseId: string) {
  return httpClient<PurchaseReturnRecord[]>(`/purchases/${purchaseId}/returns`, {
    businessId,
  });
}

export async function createPurchaseReturn(
  businessId: string,
  purchaseId: string,
  payload: PurchaseReturnPayload,
) {
  return httpClient<PurchaseReturnRecord>(`/purchases/${purchaseId}/returns`, {
    method: "POST",
    businessId,
    body: payload,
  });
}
