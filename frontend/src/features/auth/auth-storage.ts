export type SessionUser = {
  userId: string;
  fullName: string;
  email: string;
  emailVerified: boolean;
  businessId: string | null;
  businessName: string | null;
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

export function readStoredAuthSession(): AuthSession | null {
  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    const parsed = JSON.parse(rawValue) as Partial<AuthSession> & {
      user?: Partial<SessionUser>;
    };

    if (!parsed.accessToken || !parsed.user?.userId || !parsed.user.email) {
      window.localStorage.removeItem(AUTH_STORAGE_KEY);
      return null;
    }

    return {
      accessToken: parsed.accessToken,
      tokenType: parsed.tokenType ?? "Bearer",
      expiresAt: parsed.expiresAt ?? "",
      refreshToken: parsed.refreshToken ?? null,
      refreshExpiresAt: parsed.refreshExpiresAt ?? null,
      user: {
        userId: parsed.user.userId,
        fullName: parsed.user.fullName ?? "",
        email: parsed.user.email,
        emailVerified: Boolean(parsed.user.emailVerified),
        businessId: parsed.user.businessId ?? null,
        businessName: parsed.user.businessName ?? null,
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
