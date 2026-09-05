import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import { canManageStaff } from "@/features/auth/roles";
import {
  deactivateMember,
  getStaffRoster,
  inviteStaff,
  revokeInvite,
} from "@/features/staff/staff-api";

export function TeamPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const owner = canManageStaff(session?.role);
  const [email, setEmail] = useState("");
  const [role, setRole] = useState<"CLERK" | "MANAGER">("CLERK");
  const [feedback, setFeedback] = useState<string | null>(null);

  const rosterQuery = useQuery({
    queryKey: ["staff", businessId],
    queryFn: () => getStaffRoster(businessId!),
    enabled: Boolean(businessId),
  });

  const inviteMutation = useMutation({
    mutationFn: () => inviteStaff(businessId!, email.trim(), role),
    onSuccess: () => {
      setEmail("");
      setFeedback("Invite sent.");
      void queryClient.invalidateQueries({ queryKey: ["staff", businessId] });
    },
    onError: (error) => {
      setFeedback(error instanceof ApiError ? error.message : "Unable to send invite.");
    },
  });

  if (!businessId) {
    return (
      <section className="empty-state">
        <h1>Finish business setup before inviting staff.</h1>
        <Link to="/business-setup" className="primary-button">
          Complete business setup
        </Link>
      </section>
    );
  }

  async function handleInvite(event: FormEvent) {
    event.preventDefault();
    await inviteMutation.mutateAsync();
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Team</span>
        <h1>Give the counter their own login.</h1>
        <p>
          Owners invite managers and clerks. Clerks can sell; managers can run the
          shop except staff changes.
        </p>
      </section>

      {owner ? (
        <section className="panel">
          <h3>Invite a teammate</h3>
          <form className="form-stack" onSubmit={handleInvite}>
            <div className="split-grid">
              <div className="field">
                <label htmlFor="invite-email">Email</label>
                <input
                  id="invite-email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  required
                />
              </div>
              <div className="field">
                <label htmlFor="invite-role">Role</label>
                <select
                  id="invite-role"
                  value={role}
                  onChange={(event) => setRole(event.target.value as "CLERK" | "MANAGER")}
                >
                  <option value="CLERK">Clerk — sales counter</option>
                  <option value="MANAGER">Manager — shop operations</option>
                </select>
              </div>
            </div>
            {feedback ? <p className="inline-note">{feedback}</p> : null}
            <button type="submit" className="primary-button" disabled={inviteMutation.isPending}>
              {inviteMutation.isPending ? "Sending..." : "Send invite"}
            </button>
          </form>
        </section>
      ) : null}

      <section className="panel">
        <h3>People with access</h3>
        <ul className="list">
          {(rosterQuery.data?.members ?? []).map((member) => (
            <li key={member.membershipId} className="product-card">
              <strong>{member.fullName}</strong>
              <div className="product-card__row">
                <span>
                  {member.email} · {member.role}
                  {member.active ? "" : " (removed)"}
                </span>
                {owner && member.role !== "OWNER" && member.active ? (
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => {
                      void deactivateMember(businessId, member.membershipId).then(() =>
                        queryClient.invalidateQueries({ queryKey: ["staff", businessId] }),
                      );
                    }}
                  >
                    Remove access
                  </button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      </section>

      {owner ? (
        <section className="panel">
          <h3>Pending invites</h3>
          <ul className="list">
            {(rosterQuery.data?.pendingInvites ?? []).map((invite) => (
              <li key={invite.id} className="product-card">
                <strong>{invite.email}</strong>
                <div className="product-card__row">
                  <span>{invite.role}</span>
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => {
                      void revokeInvite(businessId, invite.id).then(() =>
                        queryClient.invalidateQueries({ queryKey: ["staff", businessId] }),
                      );
                    }}
                  >
                    Revoke
                  </button>
                </div>
              </li>
            ))}
          </ul>
        </section>
      ) : null}
    </>
  );
}
