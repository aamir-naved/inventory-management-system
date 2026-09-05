import { useState } from "react";
import { NavLink, Outlet } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";

const navItems = [
  { to: "/platform", label: "Dashboard", detail: "Counts", end: true },
  { to: "/platform/shops", label: "Shops", detail: "Tenants" },
  { to: "/platform/users", label: "Users", detail: "Directory" },
  { to: "/platform/settings", label: "Settings", detail: "Flags" },
];

export function PlatformLayout() {
  const { session, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

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
          <span className="brand-kicker">Platform</span>
          <h1>Super Admin</h1>
          <p>Create shops, suspend tenants, and see who is on this host.</p>
        </div>

        <nav className="nav-group" aria-label="Platform navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) => `nav-link${isActive ? " nav-link--active" : ""}`}
            >
              <span>{item.label}</span>
              <small>{item.detail}</small>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar__footer">
          <strong>{session?.fullName ?? "Platform admin"}</strong>
          <p>Not a shop member. Shop data stays isolated.</p>
        </div>
      </aside>

      <div className="content-area">
        <header className="topbar">
          <div className="topbar__title">
            <h2>Platform console</h2>
            <p>Onboard shops. Do not share this login with a counter clerk.</p>
          </div>
          <div className="topbar__meta">
            <span className="badge">PLATFORM_ADMIN</span>
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
        <main className="page">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
