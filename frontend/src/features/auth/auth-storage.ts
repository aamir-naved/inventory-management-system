export type SessionUser = {
  userId: string;
  fullName: string;
  email: string | null;
  phone: string | null;
  emailVerified: boolean;
  businessId: string | null;
  businessName: string | null;
  role: string | null;
  platformRole: "PLATFORM_ADMIN" | null;
};

export type AuthSession = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  refreshToken: string | null;
  refreshExpiresAt: string | null;
  user: SessionUser;
};

export const AUTH_STORAGE_KEY = "ims.auth";

function platformRoleFrom(value: unknown): SessionUser["platformRole"] {
  return value === "PLATFORM_ADMIN" ? "PLATFORM_ADMIN" : null;
}

export function readStoredAuthSession(): AuthSession | null {
  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<AuthSession> & {
      user?: Partial<SessionUser>;
    };

    if (!parsed.accessToken || !parsed.user?.userId) {
      window.localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }

    const platformRole = platformRoleFrom(parsed.user.platformRole);

    return {
      accessToken: parsed.accessToken,
      tokenType: parsed.tokenType ?? "Bearer",
      expiresAt: parsed.expiresAt ?? "",
      refreshToken: parsed.refreshToken ?? null,
      refreshExpiresAt: parsed.refreshExpiresAt ?? null,
      user: {
        userId: parsed.user.userId,
        fullName: parsed.user.fullName ?? "",
        email: parsed.user.email ?? null,
        phone: parsed.user.phone ?? null,
        emailVerified: Boolean(parsed.user.emailVerified),
        businessId: parsed.user.businessId ?? null,
        businessName: parsed.user.businessName ?? null,
        role: platformRole ? null : parsed.user.role ?? null,
        platformRole,
      },
    };
  } catch {
    window.localStorage.removeItem(AUTH_STORAGE_KEY);
    return null;
  }
}

export function writeStoredAuthSession(session: AuthSession) {
  window.localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(session));
}

export function clearStoredAuthSession() {
  window.localStorage.removeItem(AUTH_STORAGE_KEY);
}

export function isPlatformAdmin(session: SessionUser | null | undefined) {
  return session?.platformRole === "PLATFORM_ADMIN";
}

export function afterAuthPath(session: SessionUser | null | undefined) {
  if (isPlatformAdmin(session)) {
    return "/platform";
  }
  if (session?.businessId) {
    return "/pos";
  }
  return "/welcome";
}
