import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";
import type { SaleRecord } from "@/features/sales/sales-api";

export type CustomerPayload = {
  name: string;
  contactPerson: string;
  mobileNumber: string;
  addressLine: string;
  stateCode: string;
};

export type CustomerRecord = {
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

export type CustomerSummary = {
  id: string;
  businessId: string;
  name: string;
  contactPerson: string | null;
  mobileNumber: string | null;
  addressLine: string | null;
  stateCode: string | null;
  archived: boolean;
  invoiceCount: number;
  netBilled: number;
  amountPaid: number;
  outstandingAmount: number;
  createdAt: string;
  updatedAt: string;
};

export async function listCustomers(
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

  return httpClient<PagedResult<CustomerRecord>>(`/customers?${params.toString()}`, {
    businessId,
  });
}

export async function getCustomer(businessId: string, customerId: string) {
  return httpClient<CustomerRecord>(`/customers/${customerId}`, {
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

export async function updateCustomer(
  businessId: string,
  customerId: string,
  payload: CustomerPayload,
) {
  return httpClient<CustomerRecord>(`/customers/${customerId}`, {
    method: "PATCH",
    businessId,
    body: payload,
  });
}

export async function archiveCustomer(businessId: string, customerId: string) {
  return httpClient<CustomerRecord>(`/customers/${customerId}/archive`, {
    method: "PATCH",
    businessId,
  });
}

export async function unarchiveCustomer(businessId: string, customerId: string) {
  return httpClient<CustomerRecord>(`/customers/${customerId}/unarchive`, {
    method: "PATCH",
    businessId,
  });
}

export async function getCustomerSummary(businessId: string, customerId: string) {
  return httpClient<CustomerSummary>(`/customers/${customerId}/summary`, {
    businessId,
  });
}

export async function listCustomerSales(businessId: string, customerId: string) {
  return httpClient<SaleRecord[]>(`/customers/${customerId}/sales`, {
    businessId,
  });
}
