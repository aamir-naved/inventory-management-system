import { httpClient } from "@/api/http-client";
import { mapAuthResponse, type AuthResponse } from "@/features/auth/auth-api";

export type StaffMember = {
  membershipId: string;
  userId: string;
  fullName: string;
  email: string;
  role: string;
  active: boolean;
};

export type StaffInvite = {
  id: string;
  email: string;
  role: string;
  expiresAt: string;
  createdAt: string;
};

export type StaffRoster = {
  members: StaffMember[];
  pendingInvites: StaffInvite[];
};

export async function getStaffRoster(businessId: string) {
  return httpClient<StaffRoster>("/staff", { businessId });
}

export async function inviteStaff(businessId: string, email: string, role: "MANAGER" | "CLERK") {
  return httpClient<StaffInvite>("/staff/invites", {
    method: "POST",
    businessId,
    body: { email, role },
  });
}

export async function revokeInvite(businessId: string, inviteId: string) {
  return httpClient<{ message: string }>(`/staff/invites/${inviteId}`, {
    method: "DELETE",
    businessId,
  });
}

export async function deactivateMember(businessId: string, membershipId: string) {
  return httpClient<StaffMember>(`/staff/members/${membershipId}/deactivate`, {
    method: "PATCH",
    businessId,
  });
}

export async function previewInvite(token: string) {
  return httpClient<{
    businessName: string;
    email: string;
    role: string;
    expiresAt: string;
  }>(`/auth/invite?token=${encodeURIComponent(token)}`, {
    skipAuthRefresh: true,
  });
}

export async function acceptInvite(payload: {
  token: string;
  fullName: string;
  password: string;
}) {
  const response = await httpClient<AuthResponse>("/auth/accept-invite", {
    method: "POST",
    body: payload,
    skipAuthRefresh: true,
  });
  return mapAuthResponse(response);
}
