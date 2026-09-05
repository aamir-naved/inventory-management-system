import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { getPlatformStats } from "@/features/platform/platform-api";

export function PlatformDashboardPage() {
  const statsQuery = useQuery({
    queryKey: ["platform-stats"],
    queryFn: getPlatformStats,
  });
  const stats = statsQuery.data;

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Overview</span>
        <h1>All shops on this host.</h1>
        <p>These numbers are platform-wide. They are not a shop dashboard.</p>
      </section>
      {statsQuery.isLoading ? <p className="inline-note">Loading…</p> : null}
      {statsQuery.isError ? <p className="form-error">Unable to load platform stats.</p> : null}
      <section className="card-grid">
        <article className="stat-card">
          <h3>Shops</h3>
          <p>Total tenants</p>
          <div className="stat-value">{stats?.shopCount ?? 0}</div>
        </article>
        <article className="stat-card">
          <h3>Active</h3>
          <p>Can sign in and bill</p>
          <div className="stat-value">{stats?.activeShopCount ?? 0}</div>
        </article>
        <article className="stat-card">
          <h3>Suspended</h3>
          <p>Blocked at the tenant filter</p>
          <div className="stat-value">{stats?.suspendedShopCount ?? 0}</div>
        </article>
        <article className="stat-card">
          <h3>Users</h3>
          <p>Owners, staff, and admins</p>
          <div className="stat-value">{stats?.userCount ?? 0}</div>
        </article>
        <article className="stat-card">
          <h3>Sales today</h3>
          <p>Non-cancelled invoices (India date)</p>
          <div className="stat-value">{stats?.todaysSaleCount ?? 0}</div>
        </article>
        <article className="stat-card">
          <h3>Today amount</h3>
          <p>Sum of invoice totals</p>
          <div className="stat-value">{stats?.todaysSaleAmount ?? 0}</div>
        </article>
      </section>
      <p>
        <Link to="/platform/shops/new" className="primary-button">
          Add shop
        </Link>
      </p>
    </>
  );
}
