import { httpClient } from "@/api/http-client";

export type InventorySummary = {
  totalProducts: number;
  lowStockProducts: number;
  totalStockValue: number;
  totalPotentialRevenue: number;
};

export type InventoryStockItem = {
  productId: string;
  productName: string;
  sku: string | null;
  category: string | null;
  unit: string;
  currentStock: number;
  costPrice: number;
  sellingPrice: number;
  stockValue: number;
  lowStockThreshold: number;
  lowStock: boolean;
  archived: boolean;
};

export type InventoryMovement = {
  id: string;
  productId: string;
  productName: string;
  movementType: string;
  quantityChange: number;
  quantityBefore: number;
  quantityAfter: number;
  notes: string | null;
  createdAt: string;
};

export type InventoryAdjustmentPayload = {
  productId: string;
  adjustmentQuantity: number;
  reason: string;
};

export async function getInventorySummary(businessId: string) {
  return httpClient<InventorySummary>("/inventory/summary", { businessId });
}

export async function listInventoryStock(
  businessId: string,
  options: { search?: string; lowStockOnly?: boolean; includeArchived?: boolean } = {},
) {
  const params = new URLSearchParams();

  if (options.search?.trim()) {
    params.set("search", options.search.trim());
  }

  if (options.lowStockOnly) {
    params.set("lowStockOnly", "true");
  }

  if (options.includeArchived) {
    params.set("includeArchived", "true");
  }

  const query = params.toString();

  return httpClient<InventoryStockItem[]>(`/inventory${query ? `?${query}` : ""}`, {
    businessId,
  });
}

export async function listInventoryMovements(businessId: string, productId?: string | null) {
  const query = productId ? `?productId=${productId}` : "";
  return httpClient<InventoryMovement[]>(`/inventory/movements${query}`, { businessId });
}

export async function adjustInventoryStock(
  businessId: string,
  payload: InventoryAdjustmentPayload,
) {
  return httpClient<InventoryMovement>("/inventory/adjustments", {
    method: "POST",
    businessId,
    body: payload,
  });
}
