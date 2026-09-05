import { httpClient } from "@/api/http-client";

export type BusinessPayload = {
  name: string;
  businessType: string;
  addressLine: string;
  mobileNumber: string;
  currencyCode: string;
  timeZone: string;
  gstEnabled?: boolean;
  gstin?: string;
  stateCode?: string;
  stateName?: string;
  gstInclusivePricing?: boolean;
};

export type BusinessRecord = BusinessPayload & {
  id: string;
  gstEnabled: boolean;
  gstin: string | null;
  stateCode: string | null;
  stateName: string | null;
  gstInclusivePricing: boolean;
  hasLogo: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
};

export async function createBusiness(payload: BusinessPayload) {
  return httpClient<BusinessRecord>("/businesses", {
    method: "POST",
    body: payload,
  });
}

export async function quickStartBusiness(payload: { shopName: string; mobileNumber: string }) {
  return httpClient<BusinessRecord>("/businesses/quick-start", {
    method: "POST",
    body: payload,
  });
}

export async function getBusiness(id: string) {
  return httpClient<BusinessRecord>(`/businesses/${id}`);
}

export async function updateBusiness(id: string, payload: BusinessPayload) {
  return httpClient<BusinessRecord>(`/businesses/${id}`, {
    method: "PATCH",
    body: payload,
  });
}

export async function uploadBusinessLogo(id: string, file: File) {
  const body = new FormData();
  body.append("file", file);
  return httpClient<BusinessRecord>(`/businesses/${id}/logo`, {
    method: "POST",
    body,
  });
}

export async function removeBusinessLogo(id: string) {
  return httpClient<void>(`/businesses/${id}/logo`, {
    method: "DELETE",
  });
}
