import { httpClient } from "@/api/http-client";
import {
  readStoredAuthSession,
  type AuthSession,
} from "@/features/auth/auth-storage";

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  fullName: string;
  email: string;
  password: string;
};

export type UpdateProfilePayload = {
  fullName: string;
  currentPassword?: string;
  newPassword?: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  refreshToken?: string | null;
  refreshExpiresAt?: string | null;
  user: AuthSession["user"];
};

type MessageResponse = {
  message: string;
};

export function mapAuthResponse(response: AuthResponse, preserveRefresh = false): AuthSession {
  const existing = preserveRefresh ? readStoredAuthSession() : null;
  const refreshToken =
    response.refreshToken ?? (preserveRefresh ? existing?.refreshToken ?? null : null);
  const refreshExpiresAt =
    response.refreshExpiresAt ??
    (preserveRefresh ? existing?.refreshExpiresAt ?? null : null);

  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    expiresAt: response.expiresAt,
    refreshToken,
    refreshExpiresAt,
      user: {
        ...response.user,
        email: response.user.email ?? null,
        phone: response.user.phone ?? null,
        emailVerified: Boolean(response.user.emailVerified),
        platformRole: response.user.platformRole === "PLATFORM_ADMIN" ? "PLATFORM_ADMIN" : null,
        role:
          response.user.platformRole === "PLATFORM_ADMIN"
            ? null
            : response.user.role ?? existing?.user.role ?? null,
      },
  };
}

export async function login(payload: LoginPayload) {
  const response = await httpClient<AuthResponse>("/auth/login", {
    method: "POST",
    body: payload,
    skipAuthRefresh: true,
  });

  return mapAuthResponse(response);
}

export async function register(payload: RegisterPayload) {
  const response = await httpClient<AuthResponse>("/auth/register", {
    method: "POST",
    body: payload,
    skipAuthRefresh: true,
  });

  return mapAuthResponse(response);
}

export async function getCurrentSession() {
  const response = await httpClient<AuthResponse>("/auth/me");
  return mapAuthResponse(response, true);
}

export async function forgotPassword(email: string) {
  return httpClient<MessageResponse>("/auth/forgot-password", {
    method: "POST",
    body: { email },
    skipAuthRefresh: true,
  });
}

export async function resetPassword(token: string, password: string) {
  return httpClient<MessageResponse>("/auth/reset-password", {
    method: "POST",
    body: { token, password },
    skipAuthRefresh: true,
  });
}

export async function verifyEmail(token: string) {
  return httpClient<MessageResponse>("/auth/verify-email", {
    method: "POST",
    body: { token },
    skipAuthRefresh: true,
  });
}

export async function resendVerification() {
  return httpClient<MessageResponse>("/auth/resend-verification", {
    method: "POST",
  });
}

export async function refreshSession(refreshToken: string) {
  const response = await httpClient<AuthResponse>("/auth/refresh", {
    method: "POST",
    body: { refreshToken },
    skipAuthRefresh: true,
  });

  return mapAuthResponse(response);
}

export async function logoutRequest(refreshToken: string | null) {
  return httpClient<MessageResponse>("/auth/logout", {
    method: "POST",
    body: { refreshToken },
  });
}

export async function updateProfile(payload: UpdateProfilePayload) {
  const response = await httpClient<AuthResponse>("/auth/profile", {
    method: "PATCH",
    body: payload,
  });

  return mapAuthResponse(response, !response.refreshToken);
}

export async function getPublicConfig() {
  return httpClient<{ openRegistration: boolean; desktop: boolean }>("/auth/public-config", {
    skipAuthRefresh: true,
  });
}

export async function requestPhoneOtp(phone: string) {
  return httpClient<MessageResponse>("/auth/otp/request", {
    method: "POST",
    body: { phone },
    skipAuthRefresh: true,
  });
}

export async function verifyPhoneOtp(phone: string, code: string) {
  const response = await httpClient<AuthResponse>("/auth/otp/verify", {
    method: "POST",
    body: { phone, code },
    skipAuthRefresh: true,
  });
  return mapAuthResponse(response);
}
