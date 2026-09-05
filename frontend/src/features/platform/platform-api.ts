import { httpClient } from "@/api/http-client";
import { withPaging, type PageRequest, type PagedResult } from "@/api/paging";

export type PlatformStats = {
  shopCount: number;
  activeShopCount: number;
  suspendedShopCount: number;
  userCount: number;
  todaysSaleCount: number;
  todaysSaleAmount: number;
};

export type PlatformShopSummary = {
  id: string;
  name: string;
  mobileNumber: string;
  active: boolean;
  planCode: string;
  ownerName: string | null;
  ownerEmail: string | null;
  createdAt: string;
};

export type PlatformShopDetail = {
  id: string;
  name: string;
  mobileNumber: string;
  active: boolean;
  suspendedReason: string | null;
  planCode: string;
  ownerName: string | null;
  ownerEmail: string | null;
  ownerPhone: string | null;
  ownerUserId: string | null;
  staffCount: number;
  createdAt: string;
  updatedAt: string;
};

export type PlatformUser = {
  id: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  active: boolean;
  platformRole: string | null;
  shopId: string | null;
  shopName: string | null;
  membershipRole: string | null;
};

export async function getPlatformStats() {
  return httpClient<PlatformStats>("/platform/stats");
}

export async function getPlatformSettings() {
  return httpClient<{ openRegistration: boolean }>("/platform/settings");
}

export async function listPlatformShops(
  options: { status?: string; search?: string } & PageRequest = {},
) {
  const params = withPaging(new URLSearchParams(), options);
  if (options.status) {
    params.set("status", options.status);
  }
  if (options.search?.trim()) {
    params.set("search", options.search.trim());
  }
  return httpClient<PagedResult<PlatformShopSummary>>(`/platform/shops?${params}`);
}

export async function createPlatformShop(payload: {
  shopName: string;
  ownerName: string;
  email: string;
  phone: string;
  temporaryPassword?: string;
  provisionStarterCatalog?: boolean;
}) {
  return httpClient<{ shop: PlatformShopDetail; temporaryPassword: string }>("/platform/shops", {
    method: "POST",
    body: payload,
  });
}

export async function getPlatformShop(id: string) {
  return httpClient<PlatformShopDetail>(`/platform/shops/${id}`);
}

export async function updatePlatformShop(
  id: string,
  payload: { active?: boolean; suspendedReason?: string },
) {
  return httpClient<PlatformShopDetail>(`/platform/shops/${id}`, {
    method: "PATCH",
    body: payload,
  });
}

export async function resetPlatformOwnerPassword(id: string) {
  return httpClient<{ temporaryPassword: string }>(`/platform/shops/${id}/reset-owner`, {
    method: "POST",
  });
}

export async function listPlatformUsers(options: { search?: string } & PageRequest = {}) {
  const params = withPaging(new URLSearchParams(), options);
  if (options.search?.trim()) {
    params.set("search", options.search.trim());
  }
  return httpClient<PagedResult<PlatformUser>>(`/platform/users?${params}`);
}

export async function updatePlatformUser(id: string, payload: { active: boolean }) {
  return httpClient<PlatformUser>(`/platform/users/${id}`, {
    method: "PATCH",
    body: payload,
  });
}
