import {
  createContext,
  useContext,
  useState,
  type PropsWithChildren,
} from "react";

import {
  getCurrentSession,
  login as loginRequest,
  register as registerRequest,
  type LoginPayload,
  type RegisterPayload,
} from "@/features/auth/auth-api";
import {
  clearStoredAuthSession,
  readStoredAuthSession,
  writeStoredAuthSession,
  type AuthSession,
  type SessionUser,
} from "@/features/auth/auth-storage";

type AuthContextValue = {
  isAuthenticated: boolean;
  session: SessionUser | null;
  login: (input: LoginPayload) => Promise<void>;
  register: (input: RegisterPayload) => Promise<void>;
  updateBusinessSession: (business: { id: string; name: string }) => void;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const [authSession, setAuthSession] = useState<AuthSession | null>(() => readStoredAuthSession());

  async function syncSession(nextSession: AuthSession) {
    writeStoredAuthSession(nextSession);
    setAuthSession(nextSession);

    try {
      const refreshedSession = await getCurrentSession();
      writeStoredAuthSession(refreshedSession);
      setAuthSession(refreshedSession);
    } catch {
      clearStoredAuthSession();
      setAuthSession(null);
      throw new Error("Unable to load the current session");
    }
  }

  const value: AuthContextValue = {
    isAuthenticated: authSession !== null,
    session: authSession?.user ?? null,
    login: async (input: LoginPayload) => {
      await syncSession(await loginRequest(input));
    },
    register: async (input: RegisterPayload) => {
      await syncSession(await registerRequest(input));
    },
    updateBusinessSession: ({ id, name }) => {
      setAuthSession((current) => {
        if (!current) {
          return current;
        }

        const nextSession: AuthSession = {
          ...current,
          user: {
            ...current.user,
            businessId: id,
            businessName: name,
          },
        };

        writeStoredAuthSession(nextSession);
        return nextSession;
      });
    },
    logout: () => {
      clearStoredAuthSession();
      setAuthSession(null);
    },
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
