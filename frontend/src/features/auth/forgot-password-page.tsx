import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/http-client";
import { forgotPassword } from "@/features/auth/auth-api";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);

    try {
      const response = await forgotPassword(email);
      setSuccess(true);
      setFeedback(response.message);
    } catch (error) {
      if (error instanceof ApiError) {
        setFeedback(error.message);
      } else {
        setFeedback("Unable to send reset instructions right now.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <section className="auth-panel auth-panel--compact">
      <div className="auth-panel__form">
        <span className="brand-kicker">Password recovery</span>
        <h1>Forgot your password?</h1>
        <p>Enter your account email and we will send reset instructions if it exists.</p>

        {success ? (
          <div className="form-stack">
            <div className="form-success" role="status">
              {feedback}
            </div>
            <Link to="/login" className="primary-button">
              Back to sign in
            </Link>
          </div>
        ) : (
          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>

            {feedback ? (
              <div className="form-error" role="alert">
                {feedback}
              </div>
            ) : null}

            <button type="submit" className="primary-button" disabled={isSubmitting}>
              {isSubmitting ? "Sending..." : "Send reset link"}
            </button>

            <Link to="/login" className="text-link">
              Back to sign in
            </Link>
          </form>
        )}
      </div>
    </section>
  );
}
