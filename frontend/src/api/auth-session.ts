import { appConfig } from "@/app/config";
import {
  clearStoredAuthSession,
  readStoredAuthSession,
  writeStoredAuthSession,
} from "@/features/auth/auth-storage";

type AuthSessionExpiredListener = () => void;

const expiredListeners = new Set<AuthSessionExpiredListener>();

let refreshPromise: Promise<boolean> | null = null;

export function onAuthSessionExpired(listener: AuthSessionExpiredListener): () => void {
  expiredListeners.add(listener);
  return () => {
    expiredListeners.delete(listener);
  };
}

/** Clears stored tokens and notifies React auth state to sign the user out. */
export function expireAuthSession(): void {
  clearStoredAuthSession();
  expiredListeners.forEach((listener) => {
    try {
      listener();
    } catch {
      // Listener failures must not block other listeners.
    }
  });
}

async function tryRefreshSession(): Promise<boolean> {
  const session = readStoredAuthSession();
  if (!session?.refreshToken) {
    return false;
  }

  try {
    const response = await fetch(`${appConfig.apiBaseUrl}/auth/refresh`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ refreshToken: session.refreshToken }),
    });

    if (!response.ok) {
      return false;
    }

    const authPayload = (await response.json()) as {
      accessToken: string;
      tokenType: string;
      expiresAt: string;
      refreshToken?: string | null;
      refreshExpiresAt?: string | null;
      user: typeof session.user;
    };

    writeStoredAuthSession({
      accessToken: authPayload.accessToken,
      tokenType: authPayload.tokenType,
      expiresAt: authPayload.expiresAt,
      refreshToken: authPayload.refreshToken ?? null,
      refreshExpiresAt: authPayload.refreshExpiresAt ?? null,
      user: {
        ...authPayload.user,
        emailVerified: Boolean(authPayload.user.emailVerified),
        platformRole: authPayload.user.platformRole === "PLATFORM_ADMIN" ? "PLATFORM_ADMIN" : null,
        role:
          authPayload.user.platformRole === "PLATFORM_ADMIN"
            ? null
            : authPayload.user.role ?? session.user.role ?? null,
      },
    });

    return true;
  } catch {
    return false;
  }
}

/** Deduplicates concurrent refresh attempts across http and download clients. */
export async function refreshSessionOnce(): Promise<boolean> {
  if (!refreshPromise) {
    refreshPromise = tryRefreshSession().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}
