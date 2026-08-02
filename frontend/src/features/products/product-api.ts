import { httpClient } from "@/api/http-client";

export type ProductPayload = {
  name: string;
  sku: string;
  category: string;
  unit: string;
  costPrice: number;
  sellingPrice: number;
  openingStock: number;
  lowStockThreshold: number;
};

export type ProductRecord = {
  id: string;
  businessId: string;
  name: string;
  sku: string | null;
  category: string | null;
  unit: string;
  costPrice: number;
  sellingPrice: number;
  openingStock: number;
  currentStock: number;
  lowStockThreshold: number;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

type ProductListOptions = {
  businessId: string;
  search?: string;
  includeArchived?: boolean;
};

export async function listProducts({
  businessId,
  search,
  includeArchived = false,
}: ProductListOptions) {
  const params = new URLSearchParams();

  if (search?.trim()) {
    params.set("search", search.trim());
  }

  if (includeArchived) {
    params.set("includeArchived", "true");
  }

  const query = params.toString();

  return httpClient<ProductRecord[]>(`/products${query ? `?${query}` : ""}`, {
    businessId,
  });
}

export async function createProduct(businessId: string, payload: ProductPayload) {
  return httpClient<ProductRecord>("/products", {
    method: "POST",
    businessId,
    body: payload,
  });
}

export async function updateProduct(
  businessId: string,
  productId: string,
  payload: ProductPayload,
) {
  return httpClient<ProductRecord>(`/products/${productId}`, {
    method: "PATCH",
    businessId,
    body: payload,
  });
}

export async function archiveProduct(businessId: string, productId: string) {
  return httpClient<ProductRecord>(`/products/${productId}/archive`, {
    method: "PATCH",
    businessId,
  });
}
