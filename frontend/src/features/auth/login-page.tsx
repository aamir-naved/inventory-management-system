import { useState, type FormEvent } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";

function EyeIcon({ open }: { open: boolean }) {
  if (open) {
    return (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M3 3l18 18M10.6 10.6a2 2 0 002.8 2.8M9.9 5.1A10.5 10.5 0 0121 12c-.7 1.2-1.6 2.3-2.6 3.2M6.1 6.1C4.5 7.4 3.2 9.1 2.3 12c1.6 4.3 5.4 7 9.7 7 1.4 0 2.8-.3 4-.8"
          fill="none"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    );
  }

  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M2.3 12C3.9 7.7 7.7 5 12 5s8.1 2.7 9.7 7c-1.6 4.3-5.4 7-9.7 7s-8.1-2.7-9.7-7z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle
        cx="12"
        cy="12"
        r="3"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
      />
    </svg>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register } = useAuth();
  const [mode, setMode] = useState<"login" | "register">("login");
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    password: "",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(
    location.state &&
      typeof location.state === "object" &&
      "resetSuccess" in location.state &&
      location.state.resetSuccess
      ? "Password reset successful. Sign in with your new password."
      : null,
  );
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function switchMode(nextMode: "login" | "register") {
    setMode(nextMode);
    setFeedback(null);
    setFieldErrors({});
    setShowPassword(false);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    setFieldErrors({});

    try {
      if (mode === "login") {
        await login({
          email: form.email,
          password: form.password,
        });
      } else {
        await register(form);
      }
      navigate("/dashboard", { replace: true });
    } catch (error) {
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };
        const nextFieldErrors = response.fieldErrors ?? {};

        setFieldErrors(nextFieldErrors);
        setFeedback(
          Object.values(nextFieldErrors).join(". ") ||
            response.message ||
            error.message ||
            "Request failed.",
        );
      } else if (error instanceof Error) {
        setFeedback(error.message);
      } else {
        setFeedback("Unable to sign in right now.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="auth-panel">
      <div className="auth-panel__hero">
        <span className="brand-kicker">Simple by default</span>
        <h1>Run stock, sales, and purchases without spreadsheet chaos.</h1>
        <p>
          This frontend foundation is wired for fast onboarding, protected
          routes, and business-scoped API requests.
        </p>

        <div className="hero-points">
          <div className="hero-point">
            <strong>Three-tap mindset</strong>
            <p>Every future workflow is being shaped around the MVP product docs.</p>
          </div>
          <div className="hero-point">
            <strong>Tenant-aware shell</strong>
            <p>The active business ID can flow straight into backend requests.</p>
          </div>
          <div className="hero-point">
            <strong>Feature-first structure</strong>
            <p>Auth, dashboard, products, and inventory all have clear homes.</p>
          </div>
        </div>
      </div>

      <div className="auth-panel__form">
        <span className="brand-kicker">Owner login</span>
        <h1>{mode === "login" ? "Sign in to your business" : "Create your owner account"}</h1>
        <p>
          Real JWT-based authentication is now live. Create an owner account or
          sign back in to continue.
        </p>

        <div className="helper-row">
          <button
            type="button"
            className={mode === "login" ? "primary-button" : "ghost-button"}
            onClick={() => switchMode("login")}
          >
            Sign in
          </button>
          <button
            type="button"
            className={mode === "register" ? "primary-button" : "ghost-button"}
            onClick={() => switchMode("register")}
          >
            Create account
          </button>
        </div>

        <form className="form-stack" onSubmit={handleSubmit}>
          {mode === "register" ? (
            <div className="field">
              <label htmlFor="fullName">Full name</label>
              <input
                id="fullName"
                autoComplete="name"
                value={form.fullName}
                onChange={(event) =>
                  setForm((current) => ({ ...current, fullName: event.target.value }))
                }
              />
              {fieldErrors.fullName ? (
                <span className="field-error">{fieldErrors.fullName}</span>
              ) : null}
            </div>
          ) : null}

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              value={form.email}
              onChange={(event) =>
                setForm((current) => ({ ...current, email: event.target.value }))
              }
            />
            {fieldErrors.email ? <span className="field-error">{fieldErrors.email}</span> : null}
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input
                id="password"
                type={showPassword ? "text" : "password"}
                minLength={mode === "register" ? 8 : undefined}
                autoComplete={mode === "register" ? "new-password" : "current-password"}
                value={form.password}
                onChange={(event) =>
                  setForm((current) => ({ ...current, password: event.target.value }))
                }
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? "Hide password" : "Show password"}
                aria-pressed={showPassword}
              >
                <EyeIcon open={showPassword} />
              </button>
            </div>
            {fieldErrors.password ? (
              <span className="field-error">{fieldErrors.password}</span>
            ) : mode === "register" ? (
              <span className="inline-note">Use at least 8 characters.</span>
            ) : null}
          </div>

          {feedback ? (
            <div
              className={
                feedback.includes("successful") ? "form-success" : "form-error"
              }
              role="alert"
            >
              {feedback}
            </div>
          ) : null}

          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting
              ? mode === "login"
                ? "Signing in..."
                : "Creating account..."
              : mode === "login"
                ? "Enter dashboard"
                : "Create account"}
          </button>

          {mode === "login" ? (
            <Link to="/forgot-password" className="text-link">
              Forgot password?
            </Link>
          ) : null}
        </form>
      </div>
    </section>
  );
}
