import { httpClient } from "@/api/http-client";

export type PurchaseItemPayload = {
  productId: string;
  quantity: number;
  purchasePrice: number;
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
  amountPaid: number;
  outstandingAmount: number;
  cancelled: boolean;
  cancellationReason: string | null;
  items: Array<{
    productId: string;
    productName: string;
    unit: string;
    quantity: number;
    purchasePrice: number;
    lineTotal: number;
  }>;
  createdAt: string;
  updatedAt: string;
};

export async function listPurchases(businessId: string, search?: string) {
  const params = new URLSearchParams();
  if (search?.trim()) {
    params.set("search", search.trim());
  }

  const query = params.toString();
  return httpClient<PurchaseRecord[]>(`/purchases${query ? `?${query}` : ""}`, {
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
