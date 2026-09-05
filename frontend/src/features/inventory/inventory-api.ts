import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";

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
  options: {
    search?: string;
    lowStockOnly?: boolean;
    includeArchived?: boolean;
  } & PageRequest = {},
) {
  const params = withPaging(new URLSearchParams(), options);

  if (options.search?.trim()) {
    params.set("search", options.search.trim());
  }

  if (options.lowStockOnly) {
    params.set("lowStockOnly", "true");
  }

  if (options.includeArchived) {
    params.set("includeArchived", "true");
  }

  return httpClient<PagedResult<InventoryStockItem>>(`/inventory?${params.toString()}`, {
    businessId,
  });
}

export async function listInventoryMovements(
  businessId: string,
  productId?: string | null,
  paging: PageRequest = {},
) {
  const params = withPaging(new URLSearchParams(), paging);
  if (productId) {
    params.set("productId", productId);
  }
  return httpClient<PagedResult<InventoryMovement>>(`/inventory/movements?${params.toString()}`, {
    businessId,
  });
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
