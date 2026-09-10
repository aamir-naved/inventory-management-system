import { useMemo, useState } from "react";
import { NavLink, Outlet } from "react-router-dom";

import { useQuery } from "@tanstack/react-query";

import { useAuth } from "@/features/auth/auth-context";
import { canManageCatalog, canManageStaff, canViewReports } from "@/features/auth/roles";
import { listNotifications } from "@/features/notifications/notification-api";
import { useLocale } from "@/i18n/locale-context";
import type { MessageKey } from "@/i18n/messages";

type NavItem = {
  to: string;
  labelKey: MessageKey;
  detailKey: MessageKey;
  visible: boolean;
};

export function AppLayout() {
  const { session, logout, resendVerification } = useAuth();
  const { locale, setLocale, t } = useLocale();
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);
  const [isResending, setIsResending] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [notesOpen, setNotesOpen] = useState(false);
  const role = session?.role;
  const businessId = session?.businessId ?? null;
  const firstName = session?.fullName?.split(" ")[0] ?? "there";

  const notificationsQuery = useQuery({
    queryKey: ["notifications", businessId],
    queryFn: () => listNotifications(businessId!),
    enabled: Boolean(businessId),
    refetchInterval: 60_000,
  });

  const navItems = useMemo<NavItem[]>(
    () => {
      if (!businessId) {
        return [
          { to: "/welcome", labelKey: "nav.start", detailKey: "nav.detail.shopName", visible: true },
          {
            to: "/business-setup",
            labelKey: "nav.fullSetup",
            detailKey: "nav.detail.gstLater",
            visible: canManageCatalog(role),
          },
          { to: "/profile", labelKey: "nav.profile", detailKey: "nav.detail.account", visible: true },
        ];
      }

      return [
        { to: "/pos", labelKey: "nav.counter", detailKey: "nav.detail.barcodeSale", visible: true },
        { to: "/sales", labelKey: "nav.sales", detailKey: "nav.detail.invoices", visible: true },
        { to: "/customers", labelKey: "nav.customers", detailKey: "nav.detail.parties", visible: true },
        { to: "/dashboard", labelKey: "nav.dashboard", detailKey: "nav.detail.pulse", visible: true },
        {
          to: "/products",
          labelKey: "nav.products",
          detailKey: "nav.detail.catalog",
          visible: canManageCatalog(role),
        },
        { to: "/inventory", labelKey: "nav.inventory", detailKey: "nav.detail.stock", visible: true },
        {
          to: "/purchases",
          labelKey: "nav.purchases",
          detailKey: "nav.detail.bills",
          visible: canManageCatalog(role),
        },
        {
          to: "/suppliers",
          labelKey: "nav.suppliers",
          detailKey: "nav.detail.parties",
          visible: canManageCatalog(role),
        },
        {
          to: "/reports",
          labelKey: "nav.reports",
          detailKey: "nav.detail.insights",
          visible: canViewReports(role),
        },
        {
          to: "/team",
          labelKey: "nav.team",
          detailKey: "nav.detail.staff",
          visible: canManageStaff(role) || canViewReports(role),
        },
        {
          to: "/audit",
          labelKey: "nav.audit",
          detailKey: "nav.detail.log",
          visible: canViewReports(role),
        },
        {
          to: "/business-setup",
          labelKey: "nav.business",
          detailKey: "nav.detail.workspace",
          visible: canManageCatalog(role),
        },
        {
          to: "/settings",
          labelKey: "nav.settings",
          detailKey: "nav.detail.prefs",
          visible: canManageCatalog(role),
        },
        { to: "/profile", labelKey: "nav.profile", detailKey: "nav.detail.account", visible: true },
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
        {menuOpen ? t("shell.closeMenu") : t("shell.menu")}
      </button>
      {menuOpen ? (
        <button
          type="button"
          className="nav-backdrop"
          aria-label={t("shell.closeMenu")}
          onClick={() => setMenuOpen(false)}
        />
      ) : null}

      <aside className={`sidebar${menuOpen ? " sidebar--open" : ""}`}>
        <div className="sidebar__brand">
          <span className="brand-kicker">{t("shell.brandKicker")}</span>
          <h1>{t("shell.brandTitle")}</h1>
          <p>{t("shell.brandBody")}</p>
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
                <span>{t(item.labelKey)}</span>
                <small>{t(item.detailKey)}</small>
              </NavLink>
            ))}
        </nav>

        <div className="sidebar__footer">
          <label className="field" htmlFor="app-locale">
            <span className="inline-note">{t("shell.language")}</span>
            <select
              id="app-locale"
              value={locale}
              onChange={(event) => setLocale(event.target.value === "hi" ? "hi" : "en")}
            >
              <option value="en">English</option>
              <option value="hi">हिन्दी</option>
            </select>
          </label>
          <strong>{session?.businessName ?? t("shell.noBusiness")}</strong>
          <p>
            {session?.role ? `${session.role.toLowerCase()} access.` : "Active business for this session."}
          </p>
        </div>
      </aside>

      <div className="content-area">
        <header className="topbar">
          <div className="topbar__title">
            <h2>{t("shell.welcome", { name: firstName })}</h2>
            <p>{t("shell.tagline")}</p>
          </div>

          <div className="topbar__meta">
            <button
              type="button"
              className="ghost-button"
              onClick={() => setNotesOpen((open) => !open)}
            >
              {t("shell.alerts", { count: notificationsQuery.data?.count ?? 0 })}
            </button>
            <span className="badge">
              {session?.businessId ? t("shell.businessReady") : t("shell.setupPending")}
            </span>
            <button
              type="button"
              className="ghost-button"
              onClick={() => {
                void logout();
              }}
            >
              {t("shell.signOut")}
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
