import { useEffect, useState, type FormEvent } from "react";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";

export function ProfilePage() {
  const { session, updateProfile } = useAuth();
  const [fullName, setFullName] = useState(session?.fullName ?? "");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [feedbackTone, setFeedbackTone] = useState<"error" | "success">("error");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    setFullName(session?.fullName ?? "");
  }, [session?.fullName]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    setFieldErrors({});

    try {
      await updateProfile({
        fullName,
        ...(newPassword
          ? {
              currentPassword,
              newPassword,
            }
          : {}),
      });
      setCurrentPassword("");
      setNewPassword("");
      setFeedbackTone("success");
      setFeedback(
        newPassword
          ? "Profile and password updated."
          : "Profile updated.",
      );
    } catch (error) {
      setFeedbackTone("error");
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };
        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? error.message);
      } else if (error instanceof Error) {
        setFeedback(error.message);
      } else {
        setFeedback("Unable to update profile.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Account</span>
        <h1>Manage your profile.</h1>
        <p>Update your display name or change your password for this owner account.</p>
      </section>

      <section className="panel">
        <h3>Profile details</h3>
        <p className="inline-note">Signed in as {session?.email}</p>

        <form className="form-stack" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="fullName">Full name</label>
            <input
              id="fullName"
              autoComplete="name"
              required
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
            />
            {fieldErrors.fullName ? (
              <span className="field-error">{fieldErrors.fullName}</span>
            ) : null}
          </div>

          <div className="field">
            <label htmlFor="currentPassword">Current password</label>
            <input
              id="currentPassword"
              type="password"
              autoComplete="current-password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
            />
            <span className="inline-note">Required only when changing password.</span>
          </div>

          <div className="field">
            <label htmlFor="newPassword">New password</label>
            <input
              id="newPassword"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
            {fieldErrors.newPassword ? (
              <span className="field-error">{fieldErrors.newPassword}</span>
            ) : (
              <span className="inline-note">Leave blank to keep your current password.</span>
            )}
          </div>

          {feedback ? (
            <div
              className={feedbackTone === "success" ? "form-success" : "form-error"}
              role={feedbackTone === "success" ? "status" : "alert"}
            >
              {feedback}
            </div>
          ) : null}

          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? "Saving..." : "Save profile"}
          </button>
        </form>
      </section>
    </>
  );
}
