import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { expireAuthSession, onAuthSessionExpired } from "@/api/auth-session";
import { AUTH_STORAGE_KEY } from "@/features/auth/auth-storage";

function installMemoryStorage() {
  const store = new Map<string, string>();
  const localStorage = {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, value);
    },
    removeItem: (key: string) => {
      store.delete(key);
    },
    clear: () => {
      store.clear();
    },
  };
  vi.stubGlobal("localStorage", localStorage);
  vi.stubGlobal("window", { localStorage });
}

describe("expireAuthSession", () => {
  beforeEach(() => {
    installMemoryStorage();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("notifies listeners after clearing storage", () => {
    const listener = vi.fn();
    const unsubscribe = onAuthSessionExpired(listener);

    window.localStorage.setItem(
      AUTH_STORAGE_KEY,
      JSON.stringify({ accessToken: "x", user: { userId: "1" } }),
    );

    expireAuthSession();

    expect(listener).toHaveBeenCalledTimes(1);
    expect(window.localStorage.getItem(AUTH_STORAGE_KEY)).toBeNull();

    unsubscribe();
    expireAuthSession();
    expect(listener).toHaveBeenCalledTimes(1);
  });
});
