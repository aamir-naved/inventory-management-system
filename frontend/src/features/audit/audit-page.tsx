import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { PaginationBar } from "@/components/ui/pagination-bar";
import { useAuth } from "@/features/auth/auth-context";
import { listAuditEvents } from "@/features/audit/audit-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";
import { useState } from "react";

export function AuditPage() {
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatDateTime } = useBusinessSettings();
  const [page, setPage] = useState(0);

  const query = useQuery({
    queryKey: ["audit", businessId, page],
    queryFn: () => listAuditEvents(businessId!, { page, size: 25 }),
    enabled: Boolean(businessId),
  });

  if (!businessId) {
    return (
      <section className="empty-state">
        <h1>Finish business setup first.</h1>
        <Link to="/business-setup" className="primary-button">
          Complete business setup
        </Link>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Activity</span>
        <h1>Who changed what, and when.</h1>
        <p>Invites and other important shop actions are recorded here.</p>
      </section>
      <section className="panel">
        <ul className="list">
          {(query.data?.items ?? []).map((event) => (
            <li key={event.id} className="product-card">
              <strong>{event.summary}</strong>
              <div className="product-card__row">
                <span>
                  {event.action} · {formatDateTime(event.createdAt)}
                </span>
              </div>
            </li>
          ))}
        </ul>
        <PaginationBar page={query.data} onPageChange={setPage} />
      </section>
    </>
  );
}
