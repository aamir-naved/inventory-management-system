import { httpClient } from "@/api/http-client";

export type SupplierPayload = {
  name: string;
  contactPerson: string;
  mobileNumber: string;
  addressLine: string;
};

export type SupplierRecord = {
  id: string;
  businessId: string;
  name: string;
  contactPerson: string | null;
  mobileNumber: string | null;
  addressLine: string | null;
  archived: boolean;
  createdAt: string;
  updatedAt: string;
};

export async function listSuppliers(
  businessId: string,
  options: { search?: string; includeArchived?: boolean } = {},
) {
  const params = new URLSearchParams();

  if (options.search?.trim()) {
    params.set("search", options.search.trim());
  }

  if (options.includeArchived) {
    params.set("includeArchived", "true");
  }

  const query = params.toString();

  return httpClient<SupplierRecord[]>(`/suppliers${query ? `?${query}` : ""}`, {
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
