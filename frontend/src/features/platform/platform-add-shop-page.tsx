import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";

import { ApiError } from "@/api/http-client";
import { createPlatformShop } from "@/features/platform/platform-api";

export function PlatformAddShopPage() {
  const [shopName, setShopName] = useState("");
  const [ownerName, setOwnerName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [temporaryPassword, setTemporaryPassword] = useState("");
  const [provisionStarterCatalog, setProvisionStarterCatalog] = useState(true);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [createdShopId, setCreatedShopId] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setIsSubmitting(true);
    setFeedback(null);
    try {
      const result = await createPlatformShop({
        shopName,
        ownerName,
        email,
        phone,
        temporaryPassword: temporaryPassword.trim() || undefined,
        provisionStarterCatalog,
      });
      setCreatedShopId(result.shop.id);
      setFeedback(result.message);
    } catch (error) {
      setFeedback(error instanceof ApiError ? error.message : "Unable to create the shop.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Onboard</span>
        <h1>Add shop</h1>
        <p>Creates the tenant and its owner account. Login is email or phone, not a separate username.</p>
      </section>
      <section className="panel">
        <form className="form-stack" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="shopName">Shop name</label>
            <input
              id="shopName"
              value={shopName}
              onChange={(event) => setShopName(event.target.value)}
              placeholder="Khan General Store"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="ownerName">Owner name</label>
            <input
              id="ownerName"
              value={ownerName}
              onChange={(event) => setOwnerName(event.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="phone">Phone</label>
            <input
              id="phone"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="9876543210"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="temporaryPassword">Temporary password (optional)</label>
            <input
              id="temporaryPassword"
              value={temporaryPassword}
              onChange={(event) => setTemporaryPassword(event.target.value)}
              minLength={8}
              placeholder="Leave blank to generate"
            />
          </div>
          <label className="toggle">
            <input
              type="checkbox"
              checked={provisionStarterCatalog}
              onChange={(event) => setProvisionStarterCatalog(event.target.checked)}
            />
            <span>Add Walk-in customer and starter hardware items</span>
          </label>
          {feedback ? <p className={createdShopId ? "form-success" : "form-error"}>{feedback}</p> : null}
          {createdShopId ? (
            <Link to={`/platform/shops/${createdShopId}`} className="text-link">
              Open shop
            </Link>
          ) : null}
          <button type="submit" className="primary-button" disabled={isSubmitting}>
            {isSubmitting ? "Creating..." : "Create shop"}
          </button>
          <Link to="/platform/shops" className="text-link">
            Back to shops
          </Link>
        </form>
      </section>
    </>
  );
}
