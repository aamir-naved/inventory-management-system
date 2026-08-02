export type SessionUser = {
  userId: string;
  fullName: string;
  email: string;
  businessId: string | null;
  businessName: string | null;
};

export type AuthSession = {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: SessionUser;
};

export const AUTH_STORAGE_KEY = "ims.auth";

export function readStoredAuthSession(): AuthSession | null {
  const rawValue = window.localStorage.getItem(AUTH_STORAGE_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as AuthSession;
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
