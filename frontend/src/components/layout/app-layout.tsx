import { useMemo, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";

import { useQuery } from "@tanstack/react-query";

import { useAuth } from "@/features/auth/auth-context";
import { getPublicConfig } from "@/features/auth/auth-api";
import { canManageCatalog, canManageStaff, canViewReports } from "@/features/auth/roles";
import { listNotifications } from "@/features/notifications/notification-api";

type NavItem = {
  to: string;
  label: string;
  detail: string;
  visible: boolean;
};

export function AppLayout() {
  const { session, logout, resendVerification } = useAuth();
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);
  const [isResending, setIsResending] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [notesOpen, setNotesOpen] = useState(false);
  const role = session?.role;
  const businessId = session?.businessId ?? null;

  const notificationsQuery = useQuery({
    queryKey: ["notifications", businessId],
    queryFn: () => listNotifications(businessId!),
    enabled: Boolean(businessId),
    refetchInterval: 60_000,
  });
  const publicConfigQuery = useQuery({
    queryKey: ["public-config"],
    queryFn: getPublicConfig,
  });
  const desktop = publicConfigQuery.data?.desktop === true;

  const navItems = useMemo<NavItem[]>(
    () => {
      if (!businessId) {
        return [
          { to: "/welcome", label: "Start", detail: "Shop name", visible: true },
          { to: "/business-setup", label: "Full setup", detail: "GST later", visible: canManageCatalog(role) },
          { to: "/profile", label: "Profile", detail: "Account", visible: true },
        ];
      }

      return [
        { to: "/pos", label: "Counter", detail: "Barcode sale", visible: true },
        { to: "/sales", label: "Sales", detail: "Invoices", visible: true },
        { to: "/customers", label: "Customers", detail: "Parties", visible: true },
        { to: "/dashboard", label: "Dashboard", detail: "Quick pulse", visible: true },
        { to: "/products", label: "Products", detail: "Catalog", visible: canManageCatalog(role) },
        { to: "/inventory", label: "Inventory", detail: "Stock", visible: true },
        { to: "/purchases", label: "Purchases", detail: "Bills", visible: canManageCatalog(role) },
        { to: "/suppliers", label: "Suppliers", detail: "Parties", visible: canManageCatalog(role) },
        { to: "/reports", label: "Reports", detail: "Insights", visible: canViewReports(role) },
        { to: "/team", label: "Team", detail: "Staff", visible: canManageStaff(role) || canViewReports(role) },
        { to: "/audit", label: "Activity", detail: "Log", visible: canViewReports(role) },
        { to: "/business-setup", label: "Business", detail: "Workspace", visible: canManageCatalog(role) },
        { to: "/settings", label: "Settings", detail: "Prefs", visible: canManageCatalog(role) },
        { to: "/profile", label: "Profile", detail: "Account", visible: true },
      ];
    },
    [role, businessId],
  );

  async function handleResendVerification() {
    setIsResending(true);
    setBannerMessage(null);
    try {
      const message = await resendVerification();
      setBannerMessage(message);
    } catch {
      setBannerMessage("Unable to resend verification email right now.");
    } finally {
      setIsResending(false);
    }
  }

  return (
    <div className={`app-shell${menuOpen ? " app-shell--menu-open" : ""}`}>
      <button
        type="button"
        className="menu-toggle"
        aria-expanded={menuOpen}
        onClick={() => setMenuOpen((open) => !open)}
      >
        {menuOpen ? "Close menu" : "Menu"}
      </button>
      {menuOpen ? (
        <button
          type="button"
          className="nav-backdrop"
          aria-label="Close menu"
          onClick={() => setMenuOpen(false)}
        />
      ) : null}

      <aside className={`sidebar${menuOpen ? " sidebar--open" : ""}`}>
        <div className="sidebar__brand">
          <span className="brand-kicker">
            {desktop ? "This PC" : "Inventory for your shop"}
          </span>
          <h1>
            {desktop
              ? "Bill from the counter. Stock stays on this computer."
              : "Bill from the counter. Stock stays on the server."}
          </h1>
          <p>
            {desktop
              ? "This window talks to a local database. Export a backup from Settings so a lost PC is not the only copy."
              : "Open this link on a phone. Sales, stock, and invoices live in the cloud — not on a shop PC that can be lost."}
          </p>
        </div>

        <nav className="nav-group" aria-label="Primary navigation">
          {navItems
            .filter((item) => item.visible)
            .map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setMenuOpen(false)}
                className={({ isActive }) =>
                  `nav-link${isActive ? " nav-link--active" : ""}`
                }
              >
                <span>{item.label}</span>
                <small>{item.detail}</small>
              </NavLink>
            ))}
        </nav>

        <div className="sidebar__footer">
          <strong>{session?.businessName ?? "No business configured yet"}</strong>
          <p>
            {session?.role ? `${session.role.toLowerCase()} access.` : "Active business for this session."}
          </p>
        </div>
      </aside>

      <div className="content-area">
        <header className="topbar">
          <div className="topbar__title">
            <h2>Welcome back, {session?.fullName?.split(" ")[0] ?? "there"}</h2>
            <p>Sell first. Numbers and GST can wait until after the bill.</p>
          </div>

          <div className="topbar__meta">
            <button
              type="button"
              className="ghost-button"
              onClick={() => setNotesOpen((open) => !open)}
            >
              Alerts ({notificationsQuery.data?.count ?? 0})
            </button>
            <span className="badge">
              {desktop
                ? "Desktop"
                : session?.businessId
                  ? "Business ready"
                  : "Setup pending"}
            </span>
            <button
              type="button"
              className="ghost-button"
              onClick={() => {
                void logout();
              }}
            >
              Sign out
            </button>
          </div>
        </header>

        {notesOpen ? (
          <section className="panel notification-drawer" aria-label="Alerts">
            {(notificationsQuery.data?.items ?? []).length === 0 ? (
              <p className="inline-note">No low-stock or overdue payment alerts.</p>
            ) : (
              <ul className="list">
                {(notificationsQuery.data?.items ?? []).slice(0, 12).map((item, index) => (
                  <li key={`${item.type}-${item.entityId}-${index}`}>
                    <strong>{item.title}</strong>
                    <p>{item.detail}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>
        ) : null}

        {session?.email && !session.emailVerified ? (
          <div className="verify-banner" role="status">
            <div>
              <strong>Please verify your email.</strong>
              <p>
                {bannerMessage ??
                  `We sent a verification link to ${session.email}. You can keep using the app meanwhile.`}
              </p>
            </div>
            <button
              type="button"
              className="ghost-button"
              disabled={isResending}
              onClick={() => {
                void handleResendVerification();
              }}
            >
              {isResending ? "Sending..." : "Resend email"}
            </button>
          </div>
        ) : null}

        <main className="page">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
