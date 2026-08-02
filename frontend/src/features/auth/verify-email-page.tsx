import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { ApiError } from "@/api/http-client";
import { verifyEmail } from "@/features/auth/auth-api";
import { useAuth } from "@/features/auth/auth-context";

export function VerifyEmailPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const { isAuthenticated, markEmailVerified } = useAuth();
  const [status, setStatus] = useState<"loading" | "success" | "error">(
    token ? "loading" : "error",
  );
  const [message, setMessage] = useState(
    token ? "Verifying your email..." : "Verification link is missing or invalid.",
  );

  useEffect(() => {
    if (!token) {
      return;
    }

    let cancelled = false;

    async function run() {
      try {
        const response = await verifyEmail(token);
        if (cancelled) {
          return;
        }
        setStatus("success");
        setMessage(response.message);
        markEmailVerified();
      } catch (error) {
        if (cancelled) {
          return;
        }
        setStatus("error");
        setMessage(
          error instanceof ApiError
            ? error.message
            : "Unable to verify this email link.",
        );
      }
    }

    void run();

    return () => {
      cancelled = true;
    };
    // Intentionally run once per token; markEmailVerified only updates local session.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);

  return (
    <section className="auth-panel auth-panel--compact">
      <div className="auth-panel__form">
        <span className="brand-kicker">Email verification</span>
        <h1>
          {status === "loading"
            ? "Verifying..."
            : status === "success"
              ? "Email verified"
              : "Verification failed"}
        </h1>
        <p>{message}</p>

        <div className="form-stack">
          {isAuthenticated ? (
            <Link to="/dashboard" className="primary-button">
              Go to dashboard
            </Link>
          ) : (
            <Link to="/login" className="primary-button">
              Sign in
            </Link>
          )}
        </div>
      </div>
    </section>
  );
}
