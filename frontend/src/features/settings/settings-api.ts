import { httpClient } from "@/api/http-client";

export type BusinessSettings = {
  businessId: string;
  currencyCode: string;
  dateFormat: string;
  allowNegativeStock: boolean;
  defaultLowStockThreshold: number;
};

export type SettingsPayload = {
  currencyCode: string;
  dateFormat: string;
  allowNegativeStock: boolean;
  defaultLowStockThreshold: number;
};

export const DATE_FORMAT_OPTIONS = [
  { value: "dd/MM/yyyy", label: "31/12/2026" },
  { value: "MM/dd/yyyy", label: "12/31/2026" },
  { value: "yyyy-MM-dd", label: "2026-12-31" },
  { value: "dd-MMM-yyyy", label: "31-Dec-2026" },
] as const;

export async function getSettings(businessId: string) {
  return httpClient<BusinessSettings>("/settings", { businessId });
}

export async function updateSettings(businessId: string, payload: SettingsPayload) {
  return httpClient<BusinessSettings>("/settings", {
    method: "PUT",
    businessId,
    body: payload,
  });
}
