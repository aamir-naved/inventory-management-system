import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { getPublicConfig } from "@/features/auth/auth-api";
import { useAuth } from "@/features/auth/auth-context";
import { isPlatformAdmin } from "@/features/auth/auth-storage";
import { quickStartBusiness } from "@/features/business/business-api";

function shopMobileFromSession(phone: string | null | undefined) {
  if (!phone) {
    return "";
  }
  const digits = phone.replace(/\D/g, "");
  if (digits.length === 12 && digits.startsWith("91")) {
    return digits.slice(2);
  }
  return digits;
}

export function WelcomePage() {
  const navigate = useNavigate();
  const { session, updateBusinessSession } = useAuth();
  const publicConfigQuery = useQuery({
    queryKey: ["public-config"],
    queryFn: getPublicConfig,
  });
  const openRegistration = publicConfigQuery.data?.openRegistration ?? true;
  const [shopName, setShopName] = useState("");
  const [mobileNumber, setMobileNumber] = useState(shopMobileFromSession(session?.phone));
  const [feedback, setFeedback] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isPlatformAdmin(session)) {
    return <Navigate to="/platform" replace />;
  }

  if (session?.businessId) {
    return <Navigate to="/pos" replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    try {
      const business = await quickStartBusiness({
        shopName: shopName.trim(),
        mobileNumber: mobileNumber.trim(),
      });
      updateBusinessSession({ id: business.id, name: business.name });
      navigate("/pos", { replace: true });
    } catch (error) {
      setFeedback(
        error instanceof ApiError ? error.message : "Unable to create the shop. Try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  if (!openRegistration) {
    return (
      <>
        <section className="page-intro">
          <span className="brand-kicker">Waiting for a shop</span>
          <h1>This account has no shop yet.</h1>
          <p>
            Public signup is closed. Ask the operator to create your shop from the platform
            console, then sign in with the email or mobile they issued.
          </p>
        </section>
      </>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">2 minutes</span>
        <h1>What is the shop called?</h1>
        <p>
          That is enough to start. GST, logo, and extra details can wait. We add a Walk-in
          customer and a few hardware items so you can bill immediately.
        </p>
      </section>
      <section className="panel">
        <form className="form-stack" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="shop-name">Shop name</label>
            <input
              id="shop-name"
              value={shopName}
              onChange={(event) => setShopName(event.target.value)}
              placeholder="Ram Hardware"
              required
              autoFocus
            />
          </div>
          <div className="field">
            <label htmlFor="shop-mobile">Shop mobile</label>
            <input
              id="shop-mobile"
              value={mobileNumber}
              onChange={(event) => setMobileNumber(event.target.value)}
              placeholder="9876543210"
              required
            />
          </div>
          {feedback ? <p className="form-error">{feedback}</p> : null}
          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? "Opening shop..." : "Open the counter"}
          </button>
        </form>
      </section>
    </>
  );
}
