import {
  createContext,
  useContext,
  useState,
  type PropsWithChildren,
} from "react";

import {
  getCurrentSession,
  login as loginRequest,
  logoutRequest,
  register as registerRequest,
  requestPhoneOtp,
  resendVerification as resendVerificationRequest,
  updateProfile as updateProfileRequest,
  verifyPhoneOtp,
  type LoginPayload,
  type RegisterPayload,
  type UpdateProfilePayload,
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
  requestOtp: (phone: string) => Promise<string>;
  verifyOtp: (phone: string, code: string) => Promise<void>;
  applySession: (session: AuthSession) => Promise<void>;
  register: (input: RegisterPayload) => Promise<void>;
  updateBusinessSession: (business: { id: string; name: string }) => void;
  updateProfile: (input: UpdateProfilePayload) => Promise<void>;
  resendVerification: () => Promise<string>;
  markEmailVerified: () => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: PropsWithChildren) {
  const [authSession, setAuthSession] = useState<AuthSession | null>(() =>
    readStoredAuthSession(),
  );

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
    requestOtp: async (phone: string) => {
      const response = await requestPhoneOtp(phone);
      return response.message;
    },
    verifyOtp: async (phone: string, code: string) => {
      await syncSession(await verifyPhoneOtp(phone, code));
    },
    applySession: async (session: AuthSession) => {
      await syncSession(session);
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
    updateProfile: async (input: UpdateProfilePayload) => {
      const nextSession = await updateProfileRequest(input);
      writeStoredAuthSession(nextSession);
      setAuthSession(nextSession);
    },
    resendVerification: async () => {
      const response = await resendVerificationRequest();
      return response.message;
    },
    markEmailVerified: () => {
      setAuthSession((current) => {
        if (!current) {
          return current;
        }

        const nextSession: AuthSession = {
          ...current,
          user: {
            ...current.user,
            emailVerified: true,
          },
        };

        writeStoredAuthSession(nextSession);
        return nextSession;
      });
    },
    logout: async () => {
      const current = readStoredAuthSession();
      try {
        if (current) {
          await logoutRequest(current.refreshToken);
        }
      } catch {
        // Local sign-out should still succeed if the API call fails.
      } finally {
        clearStoredAuthSession();
        setAuthSession(null);
      }
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
