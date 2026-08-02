import { httpClient } from "@/api/http-client";

export type CustomerPayload = {
  name: string;
  contactPerson: string;
  mobileNumber: string;
  addressLine: string;
};

export type CustomerRecord = {
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

export async function listCustomers(
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

  return httpClient<CustomerRecord[]>(`/customers${query ? `?${query}` : ""}`, {
    businessId,
  });
}

export async function createCustomer(businessId: string, payload: CustomerPayload) {
  return httpClient<CustomerRecord>("/customers", {
    method: "POST",
    businessId,
    body: payload,
  });
}
