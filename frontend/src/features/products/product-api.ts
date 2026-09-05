import { httpClient } from "@/api/http-client";
import { saveFileDownload } from "@/api/download-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";

export type ProductPayload = {
  name: string;
  sku: string;
  category: string;
  unit: string;
  costPrice: number;
  sellingPrice: number;
  openingStock: number;
  lowStockThreshold: number;
  barcode: string;
  hsnCode: string;
  gstRate: number;
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
  barcode: string | null;
  hsnCode: string | null;
  gstRate: number;
  createdAt: string;
  updatedAt: string;
};

type ProductListOptions = {
  businessId: string;
  search?: string;
  includeArchived?: boolean;
} & PageRequest;

export async function listProducts({
  businessId,
  search,
  includeArchived = false,
  page,
  size,
}: ProductListOptions) {
  const params = withPaging(new URLSearchParams(), { page, size });

  if (search?.trim()) {
    params.set("search", search.trim());
  }

  if (includeArchived) {
    params.set("includeArchived", "true");
  }

  return httpClient<PagedResult<ProductRecord>>(`/products?${params.toString()}`, {
    businessId,
  });
}

export async function getProduct(businessId: string, productId: string) {
  return httpClient<ProductRecord>(`/products/${productId}`, {
    businessId,
  });
}

export async function getProductByBarcode(businessId: string, barcode: string) {
  return httpClient<ProductRecord>(`/products/by-barcode/${encodeURIComponent(barcode)}`, {
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

export async function unarchiveProduct(businessId: string, productId: string) {
  return httpClient<ProductRecord>(`/products/${productId}/unarchive`, {
    method: "PATCH",
    businessId,
  });
}

export type ProductImportRowError = {
  rowNumber: number;
  message: string;
};

export type ProductImportResult = {
  created: number;
  updated: number;
  failed: number;
  errors: ProductImportRowError[];
};

export async function downloadProductsExcel(businessId: string) {
  return saveFileDownload(
    "/products/export.xlsx",
    businessId,
    "products.xlsx",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
  );
}

export async function importProductsExcel(businessId: string, file: File) {
  const body = new FormData();
  body.append("file", file);

  return httpClient<ProductImportResult>("/products/import", {
    method: "POST",
    businessId,
    body,
  });
}
