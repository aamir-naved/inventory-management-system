import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";
import type { PurchaseRecord } from "@/features/purchases/purchase-api";

export type SupplierPayload = {
  name: string;
  contactPerson: string;
  mobileNumber: string;
  addressLine: string;
  stateCode: string;
};

export type SupplierRecord = {
  id: string;
  businessId: string;
  name: string;
  contactPerson: string | null;
  mobileNumber: string | null;
  addressLine: string | null;
  stateCode: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export type SupplierSummary = {
  id: string;
  businessId: string;
  name: string;
  contactPerson: string | null;
  mobileNumber: string | null;
  addressLine: string | null;
  stateCode: string | null;
  archived: boolean;
  billCount: number;
  billedAmount: number;
  amountPaid: number;
  outstandingAmount: number;
  createdAt: string;
  updatedAt: string;
};

export async function listSuppliers(
  businessId: string,
  options: { search?: string; includeArchived?: boolean } & PageRequest = {},
) {
  const params = withPaging(new URLSearchParams(), options);

  if (options.search?.trim()) {
    params.set("search", options.search.trim());
  }

  if (options.includeArchived) {
    params.set("includeArchived", "true");
  }

  return httpClient<PagedResult<SupplierRecord>>(`/suppliers?${params.toString()}`, {
    businessId,
  });
}

export async function getSupplier(businessId: string, supplierId: string) {
  return httpClient<SupplierRecord>(`/suppliers/${supplierId}`, {
    businessId,
  });
}

export async function createSupplier(businessId: string, payload: SupplierPayload) {
  return httpClient<SupplierRecord>("/suppliers", {
    method: "POST",
    businessId,
    body: payload,
  });
}

export async function updateSupplier(
  businessId: string,
  supplierId: string,
  payload: SupplierPayload,
) {
  return httpClient<SupplierRecord>(`/suppliers/${supplierId}`, {
    method: "PATCH",
    businessId,
    body: payload,
  });
}

export async function archiveSupplier(businessId: string, supplierId: string) {
  return httpClient<SupplierRecord>(`/suppliers/${supplierId}/archive`, {
    method: "PATCH",
    businessId,
  });
}

export async function unarchiveSupplier(businessId: string, supplierId: string) {
  return httpClient<SupplierRecord>(`/suppliers/${supplierId}/unarchive`, {
    method: "PATCH",
    businessId,
  });
}

export async function getSupplierSummary(businessId: string, supplierId: string) {
  return httpClient<SupplierSummary>(`/suppliers/${supplierId}/summary`, {
    businessId,
  });
}

export async function listSupplierPurchases(businessId: string, supplierId: string) {
  return httpClient<PurchaseRecord[]>(`/suppliers/${supplierId}/purchases`, {
    businessId,
  });
}
