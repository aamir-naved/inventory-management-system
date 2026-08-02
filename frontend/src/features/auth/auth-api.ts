import { httpClient } from "@/api/http-client";
import type { AuthSession } from "@/features/auth/auth-storage";

export type LoginPayload = {
  email: string;
  password: string;
};

export type RegisterPayload = {
  fullName: string;
  email: string;
  password: string;
};

type AuthResponse = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: AuthSession["user"];
};

function mapAuthResponse(response: AuthResponse): AuthSession {
  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    expiresAt: response.expiresAt,
    user: response.user,
  };
}

export async function login(payload: LoginPayload) {
  const response = await httpClient<AuthResponse>("/auth/login", {
    method: "POST",
    body: payload,
  });

  return mapAuthResponse(response);
}

export async function register(payload: RegisterPayload) {
  const response = await httpClient<AuthResponse>("/auth/register", {
    method: "POST",
    body: payload,
  });

  return mapAuthResponse(response);
}

export async function getCurrentSession() {
  const response = await httpClient<AuthResponse>("/auth/me");
  return mapAuthResponse(response);
}
