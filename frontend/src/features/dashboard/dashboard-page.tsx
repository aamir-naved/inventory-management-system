import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { useAuth } from "@/features/auth/auth-context";
import { getDashboardMetrics } from "@/features/dashboard/dashboard-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";

export function DashboardPage() {
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney } = useBusinessSettings();

  const metricsQuery = useQuery({
    queryKey: ["dashboard-metrics", businessId],
    queryFn: () => getDashboardMetrics(businessId!),
    enabled: Boolean(businessId),
  });

  const metrics = metricsQuery.data;

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Live dashboard</span>
        <h1>Daily business visibility in one screen.</h1>
        <p>
          Today&apos;s sales and purchases, lifetime revenue, stock health, and
          outstanding balances for the current business.
        </p>
      </section>

      {!businessId ? (
        <section className="panel">
          <h3>Business setup is the next required step</h3>
          <p>
            The app shell is ready, but we still need a real business profile so
            inventory, products, and sales can attach to the correct tenant.
          </p>
          <Link to="/business-setup" className="primary-button">
            Complete business setup
          </Link>
        </section>
      ) : null}

      {businessId && metricsQuery.isLoading ? (
        <p className="inline-note">Loading dashboard metrics…</p>
      ) : null}

      {businessId && metricsQuery.isError ? (
        <p className="inline-note">
          Could not load dashboard metrics. Refresh to try again.
        </p>
      ) : null}

      <section className="card-grid">
        <article className="stat-card">
          <h3>Today&apos;s sales</h3>
          <p>Net sales amount for today</p>
          <div className="stat-value">{formatMoney(metrics?.todaysSalesAmount)}</div>
        </article>

        <article className="stat-card">
          <h3>Today&apos;s purchases</h3>
          <p>Purchase total for today</p>
          <div className="stat-value">{formatMoney(metrics?.todaysPurchasesAmount)}</div>
        </article>

        <article className="stat-card">
          <h3>Total revenue</h3>
          <p>Lifetime net sales after returns</p>
          <div className="stat-value">{formatMoney(metrics?.totalRevenue)}</div>
        </article>

        <article className="stat-card">
          <h3>Total products</h3>
          <p>Active catalog items</p>
          <div className="stat-value">{metrics?.totalProducts ?? 0}</div>
        </article>

        <article className="stat-card">
          <h3>Inventory value</h3>
          <p>Cost price × current quantity</p>
          <div className="stat-value">{formatMoney(metrics?.inventoryValue)}</div>
        </article>

        <article className="stat-card">
          <h3>Low stock</h3>
          <p>Products at or below threshold</p>
          <div className="stat-value">{metrics?.lowStockProducts ?? 0}</div>
        </article>

        <article className="stat-card">
          <h3>Outstanding customers</h3>
          <p>Pending customer payments</p>
          <div className="stat-value">{formatMoney(metrics?.outstandingCustomers)}</div>
        </article>

        <article className="stat-card">
          <h3>Outstanding suppliers</h3>
          <p>Pending supplier payments</p>
          <div className="stat-value">{formatMoney(metrics?.outstandingSuppliers)}</div>
        </article>
      </section>
    </>
  );
}
