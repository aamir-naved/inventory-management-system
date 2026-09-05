import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import {
  getPlatformShop,
  resetPlatformOwnerPassword,
  updatePlatformShop,
} from "@/features/platform/platform-api";

export function PlatformShopDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [reason, setReason] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [password, setPassword] = useState<string | null>(null);

  const shopQuery = useQuery({
    queryKey: ["platform-shop", id],
    queryFn: () => getPlatformShop(id!),
    enabled: Boolean(id),
  });

  const shop = shopQuery.data;

  const updateMutation = useMutation({
    mutationFn: (active: boolean) =>
      updatePlatformShop(id!, {
        active,
        suspendedReason: active ? undefined : reason,
      }),
    onSuccess: (updated) => {
      setFeedback(updated.active ? "Shop is active." : "Shop is suspended.");
      setPassword(null);
      void queryClient.invalidateQueries({ queryKey: ["platform-shop", id] });
      void queryClient.invalidateQueries({ queryKey: ["platform-shops"] });
      void queryClient.invalidateQueries({ queryKey: ["platform-stats"] });
    },
    onError: (error) => {
      setFeedback(error instanceof ApiError ? error.message : "Unable to update the shop.");
    },
  });

  const resetMutation = useMutation({
    mutationFn: () => resetPlatformOwnerPassword(id!),
    onSuccess: (result) => {
      setPassword(result.temporaryPassword);
      setFeedback("Owner password reset. Copy it now; it is not shown again.");
    },
    onError: (error) => {
      setFeedback(error instanceof ApiError ? error.message : "Unable to reset the password.");
    },
  });

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Tenant</span>
        <h1>{shop?.name ?? "Shop"}</h1>
        <p>Plan is a placeholder ({shop?.planCode ?? "standard"}). Billing is not wired.</p>
      </section>
      {shopQuery.isError ? <p className="form-error">Unable to load this shop.</p> : null}
      {shop ? (
        <section className="panel">
          <p>
            Status: <strong>{shop.active ? "Active" : "Suspended"}</strong>
            {shop.suspendedReason ? ` — ${shop.suspendedReason}` : ""}
          </p>
          <p>Mobile: {shop.mobileNumber}</p>
          <p>
            Owner: {shop.ownerName ?? "—"} · {shop.ownerEmail ?? "no email"} · {shop.ownerPhone ?? "no phone"}
          </p>
          <p>Staff seats (including owner): {shop.staffCount}</p>
          {!shop.active ? (
            <div className="field">
              <label htmlFor="reason">Suspend reason</label>
              <input
                id="reason"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="Non-payment, abuse, requested pause"
              />
            </div>
          ) : (
            <div className="field">
              <label htmlFor="reason">Suspend reason (if you suspend)</label>
              <input
                id="reason"
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                placeholder="Optional note"
              />
            </div>
          )}
          {feedback ? <p className="inline-note">{feedback}</p> : null}
          {password ? (
            <p className="inline-note">
              Temporary password: <strong>{password}</strong>
            </p>
          ) : null}
          <div className="product-card__actions">
            {shop.active ? (
              <button
                type="button"
                className="ghost-button"
                disabled={updateMutation.isPending}
                onClick={() => updateMutation.mutate(false)}
              >
                Suspend shop
              </button>
            ) : (
              <button
                type="button"
                className="primary-button"
                disabled={updateMutation.isPending}
                onClick={() => updateMutation.mutate(true)}
              >
                Activate shop
              </button>
            )}
            <button
              type="button"
              className="ghost-button"
              disabled={resetMutation.isPending}
              onClick={() => resetMutation.mutate()}
            >
              Reset owner password
            </button>
          </div>
          <Link to="/platform/shops" className="text-link">
            Back to shops
          </Link>
        </section>
      ) : null}
    </>
  );
}
