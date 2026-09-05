import { useState, type FormEvent } from "react";
import { Link, useNavigate, useLocation } from "react-router-dom";

import { ApiError } from "@/api/http-client";
import { getPublicConfig } from "@/features/auth/auth-api";
import { useAuth } from "@/features/auth/auth-context";
import { afterAuthPath, readStoredAuthSession } from "@/features/auth/auth-storage";
import { useQuery } from "@tanstack/react-query";

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
      <circle cx="12" cy="12" r="3" fill="none" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login, register, requestOtp, verifyOtp } = useAuth();
  const publicConfigQuery = useQuery({
    queryKey: ["public-config"],
    queryFn: getPublicConfig,
  });
  const openRegistration = publicConfigQuery.data?.openRegistration ?? true;
  const [mode, setMode] = useState<"otp" | "login" | "register">("otp");
  const activeMode = mode === "register" && !openRegistration ? "login" : mode;
  const [phone, setPhone] = useState("");
  const [otpCode, setOtpCode] = useState("");
  const [otpSent, setOtpSent] = useState(false);
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

  function switchMode(nextMode: "otp" | "login" | "register") {
    if (nextMode === "register" && !openRegistration) {
      return;
    }
    setMode(nextMode);
    setFeedback(null);
    setFieldErrors({});
    setShowPassword(false);
    setOtpSent(false);
    setOtpCode("");
  }

  async function handleOtpRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    try {
      const message = await requestOtp(phone);
      setOtpSent(true);
      setFeedback(message);
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : "Unable to send OTP.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleOtpVerify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    try {
      await verifyOtp(phone, otpCode);
      navigate(afterAuthPath(readStoredAuthSession()?.user), { replace: true });
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : "Unable to verify OTP.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    setFieldErrors({});

    try {
      if (activeMode === "login") {
        await login({
          email: form.email,
          password: form.password,
        });
      } else {
        await register(form);
      }
      navigate(afterAuthPath(readStoredAuthSession()?.user), { replace: true });
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
        <span className="brand-kicker">Open on your phone</span>
        <h1>Start billing in a few minutes. Your data stays in the cloud — not on a shop PC.</h1>
        <p>
          Sign in with your mobile number. No software to install on the counter computer.
        </p>
        <div className="hero-points">
          <div className="hero-point">
            <strong>Phone login</strong>
            <p>OTP on SMS (or in server logs while SMS is not connected).</p>
          </div>
          <div className="hero-point">
            <strong>Sell first</strong>
            <p>Walk-in customer and sample items are ready after you name the shop.</p>
          </div>
          <div className="hero-point">
            <strong>Safe if the PC dies</strong>
            <p>Stock and bills live on the server you host — with backups.</p>
          </div>
        </div>
      </div>

      <div className="auth-panel__form">
        <span className="brand-kicker">Shop login</span>
        <h1>
          {activeMode === "otp"
            ? "Enter your mobile number"
            : activeMode === "login"
              ? "Sign in with email"
              : "Create an email account"}
        </h1>
        <p>
          {activeMode === "otp"
            ? openRegistration
              ? "We send a 6-digit code. New shops go straight to naming the shop."
              : "We send a 6-digit code to numbers the operator has already registered."
            : openRegistration
              ? "Existing email logins still work. New shops should use mobile OTP."
              : "Use the email and password the operator issued for your shop."}
        </p>

        <div className="helper-row">
          <button
            type="button"
            className={activeMode === "otp" ? "primary-button" : "ghost-button"}
            onClick={() => switchMode("otp")}
          >
            Mobile OTP
          </button>
          <button
            type="button"
            className={activeMode === "login" ? "primary-button" : "ghost-button"}
            onClick={() => switchMode("login")}
          >
            Email
          </button>
          {openRegistration ? (
          <button
            type="button"
            className={activeMode === "register" ? "primary-button" : "ghost-button"}
            onClick={() => switchMode("register")}
          >
            Email signup
          </button>
          ) : null}
        </div>

        {activeMode === "otp" ? (
          <form className="form-stack" onSubmit={otpSent ? handleOtpVerify : handleOtpRequest}>
            <div className="field">
              <label htmlFor="phone">Mobile number</label>
              <input
                id="phone"
                inputMode="numeric"
                autoComplete="tel"
                value={phone}
                onChange={(event) => setPhone(event.target.value)}
                placeholder="9876543210"
              />
            </div>
            {otpSent ? (
              <div className="field">
                <label htmlFor="otp">6-digit code</label>
                <input
                  id="otp"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  value={otpCode}
                  onChange={(event) => setOtpCode(event.target.value)}
                  placeholder="123456"
                />
              </div>
            ) : null}
            {feedback ? (
              <div className={otpSent && !feedback.toLowerCase().includes("wrong") ? "form-success" : "form-error"} role="status">
                {feedback}
              </div>
            ) : null}
            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting
                ? "Please wait..."
                : otpSent
                  ? "Verify and enter"
                  : "Send OTP"}
            </button>
            {otpSent ? (
              <button type="button" className="text-link" onClick={() => setOtpSent(false)}>
                Change number
              </button>
            ) : null}
          </form>
        ) : (
          <form className="form-stack" onSubmit={handleSubmit}>
            {activeMode === "register" ? (
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
                  minLength={activeMode === "register" ? 8 : undefined}
                  autoComplete={activeMode === "register" ? "new-password" : "current-password"}
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
              ) : activeMode === "register" ? (
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
                ? activeMode === "login"
                  ? "Signing in..."
                  : "Creating account..."
                : activeMode === "login"
                  ? "Enter shop"
                  : "Create account"}
            </button>

            {activeMode === "login" ? (
              <Link to="/forgot-password" className="text-link">
                Forgot password?
              </Link>
            ) : null}
          </form>
        )}
      </div>
    </section>
  );
}
