import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { useAuth } from "@/features/auth/auth-context";
import { canOpenPath } from "@/features/auth/roles";
import { getDashboardMetrics } from "@/features/dashboard/dashboard-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";

type MetricCardConfig = {
  title: string;
  description: string;
  value: ReactNode;
  to: string;
  hint: string;
};

function destinationPath(to: string) {
  return to.split("?")[0] ?? to;
}

function MetricCard({
  title,
  description,
  value,
  to,
  hint,
  allowed,
}: MetricCardConfig & { allowed: boolean }) {
  const body = (
    <>
      <h3>{title}</h3>
      <p>{description}</p>
      <div className="stat-value">{value}</div>
      {allowed ? <span className="stat-card__hint">{hint}</span> : null}
    </>
  );

  if (!allowed) {
    return <article className="stat-card">{body}</article>;
  }

  return (
    <Link to={to} className="stat-card stat-card--link" aria-label={`${title}. ${hint}`}>
      {body}
    </Link>
  );
}

export function DashboardPage() {
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const role = session?.role;
  const { formatMoney } = useBusinessSettings();

  const metricsQuery = useQuery({
    queryKey: ["dashboard-metrics", businessId],
    queryFn: () => getDashboardMetrics(businessId!),
    enabled: Boolean(businessId),
  });

  const metrics = metricsQuery.data;
  const cards: MetricCardConfig[] = [
    {
      title: "Today's sales",
      description: "Net sales amount for today",
      value: formatMoney(metrics?.todaysSalesAmount),
      to: "/sales",
      hint: "Open sales",
    },
    {
      title: "Today's purchases",
      description: "Purchase total for today",
      value: formatMoney(metrics?.todaysPurchasesAmount),
      to: "/purchases",
      hint: "Open purchases",
    },
    {
      title: "Total revenue",
      description: "Lifetime net sales after returns",
      value: formatMoney(metrics?.totalRevenue),
      to: "/sales",
      hint: "View all sales",
    },
    {
      title: "Total products",
      description: "Active catalog items",
      value: metrics?.totalProducts ?? 0,
      to: "/products",
      hint: "Open products",
    },
    {
      title: "Inventory value",
      description: "Cost price × current quantity",
      value: formatMoney(metrics?.inventoryValue),
      to: "/inventory",
      hint: "Open inventory",
    },
    {
      title: "Low stock",
      description: "Products at or below threshold",
      value: metrics?.lowStockProducts ?? 0,
      to: "/inventory?lowStock=true",
      hint: "View low-stock items",
    },
    {
      title: "Outstanding customers",
      description: "Pending customer payments",
      value: formatMoney(metrics?.outstandingCustomers),
      to: "/customers",
      hint: "Open customers",
    },
    {
      title: "Outstanding suppliers",
      description: "Pending supplier payments",
      value: formatMoney(metrics?.outstandingSuppliers),
      to: "/suppliers",
      hint: "Open suppliers",
    },
  ];

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Live dashboard</span>
        <h1>Daily business visibility in one screen.</h1>
        <p>
          Today&apos;s sales and purchases, lifetime revenue, stock health, and
          outstanding balances for the current business. Click a card to open that
          screen.
        </p>
      </section>

      {!businessId ? (
        <section className="panel">
          <h3>Business setup is the next required step</h3>
          <p>
            Create your business profile so inventory, products, and sales attach
            to your shop.
          </p>
          <Link to="/welcome" className="primary-button">
            Name the shop
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
        {cards.map((card) => (
          <MetricCard
            key={card.title}
            {...card}
            allowed={Boolean(businessId) && canOpenPath(role, destinationPath(card.to))}
          />
        ))}
      </section>
    </>
  );
}
