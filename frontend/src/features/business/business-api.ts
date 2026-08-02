import { httpClient } from "@/api/http-client";

export type BusinessPayload = {
  name: string;
  businessType: string;
  addressLine: string;
  mobileNumber: string;
  currencyCode: string;
  timeZone: string;
};

export type BusinessRecord = BusinessPayload & {
  id: string;
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

export async function getBusiness(id: string) {
  return httpClient<BusinessRecord>(`/businesses/${id}`);
}

export async function updateBusiness(id: string, payload: BusinessPayload) {
  return httpClient<BusinessRecord>(`/businesses/${id}`, {
    method: "PATCH",
    body: payload,
  });
}
