import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { ApiError } from "@/api/http-client";
import { resetPassword } from "@/features/auth/auth-api";

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get("token") ?? "";
  const [password, setPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      await resetPassword(token, password);
      navigate("/login", { replace: true, state: { resetSuccess: true } });
    } catch (error) {
      if (error instanceof ApiError) {
        setFeedback(error.message);
      } else {
        setFeedback("Unable to reset password right now.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <section className="auth-panel auth-panel--compact">
        <div className="auth-panel__form">
          <span className="brand-kicker">Password recovery</span>
          <h1>Reset link is missing</h1>
          <p>Request a new password reset email and try again.</p>
          <Link to="/forgot-password" className="primary-button">
            Forgot password
          </Link>
        </div>
      </section>
    );
  }

  return (
    <section className="auth-panel auth-panel--compact">
      <div className="auth-panel__form">
        <span className="brand-kicker">Password recovery</span>
        <h1>Choose a new password</h1>
        <p>Use at least 8 characters for your new password.</p>

        <form className="form-stack" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="password">New password</label>
            <input
              id="password"
              type="password"
              autoComplete="new-password"
              minLength={8}
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
            />
          </div>

          {feedback ? (
            <div className="form-error" role="alert">
              {feedback}
            </div>
          ) : null}

          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? "Saving..." : "Reset password"}
          </button>

          <Link to="/login" className="text-link">
            Back to sign in
          </Link>
        </form>
      </div>
    </section>
  );
}
