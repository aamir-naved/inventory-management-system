import { useState, type FormEvent } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import { acceptInvite, previewInvite } from "@/features/staff/staff-api";

export function AcceptInvitePage() {
  const [params] = useSearchParams();
  const token = params.get("token") ?? "";
  const navigate = useNavigate();
  const { applySession } = useAuth();
  const [fullName, setFullName] = useState("");
  const [password, setPassword] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const previewQuery = useQuery({
    queryKey: ["invite", token],
    queryFn: () => previewInvite(token),
    enabled: Boolean(token),
    retry: false,
  });

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    try {
      await applySession(await acceptInvite({ token, fullName, password }));
      navigate("/pos", { replace: true });
    } catch (error) {
      setFeedback(error instanceof ApiError ? error.message : "Unable to accept this invite.");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!token) {
    return (
      <section className="panel">
        <h1>Invite link is missing.</h1>
        <Link to="/login">Back to sign in</Link>
      </section>
    );
  }

  return (
    <section className="panel">
      <span className="brand-kicker">Join the shop</span>
      <h1>
        {previewQuery.data
          ? `Join ${previewQuery.data.businessName} as ${previewQuery.data.role.toLowerCase()}.`
          : "Accept your invite"}
      </h1>
      <p>
        {previewQuery.data
          ? `Create a password for ${previewQuery.data.email}. If you already have an account, use that password.`
          : previewQuery.isError
            ? "This invite is invalid or has expired."
            : "Loading invite..."}
      </p>
      {previewQuery.data ? (
        <form className="form-stack" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="invite-name">Your name</label>
            <input
              id="invite-name"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="invite-password">Password</label>
            <input
              id="invite-password"
              type="password"
              minLength={8}
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              required
            />
          </div>
          {feedback ? <p className="form-error">{feedback}</p> : null}
          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? "Joining..." : "Join shop"}
          </button>
        </form>
      ) : (
        <Link to="/login">Back to sign in</Link>
      )}
    </section>
  );
}
