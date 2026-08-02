import { NavLink, Outlet } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";

const navItems = [
  {
    to: "/dashboard",
    label: "Dashboard",
    detail: "Quick pulse",
  },
  {
    to: "/business-setup",
    label: "Business",
    detail: "Profile",
  },
  {
    to: "/products",
    label: "Products",
    detail: "Live",
  },
  {
    to: "/inventory",
    label: "Inventory",
    detail: "Stock",
  },
  {
    to: "/purchases",
    label: "Purchases",
    detail: "Live",
  },
  {
    to: "/sales",
    label: "Sales",
    detail: "Live",
  },
];

export function AppLayout() {
  const { session, logout } = useAuth();

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar__brand">
          <span className="brand-kicker">MVP foundation</span>
          <h1>Inventory cockpit built for speed.</h1>
          <p>
            A clean shell for products, stock, purchases, sales, and business
            setup.
          </p>
        </div>

        <nav className="nav-group" aria-label="Primary navigation">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
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
            {session?.businessId
              ? "Business-aware routing is ready for the backend tenant header."
              : "Finish business setup to anchor the tenant-aware product flows."}
          </p>
        </div>
      </aside>

      <div className="content-area">
        <header className="topbar">
          <div className="topbar__title">
            <h2>Welcome back, {session?.fullName?.split(" ")[0] ?? "Owner"}</h2>
            <p>Keep today’s inventory decisions visible and simple.</p>
          </div>

          <div className="topbar__meta">
            <span className="badge">
              {session?.businessId ? "Business ID wired" : "Setup pending"}
            </span>
            <button type="button" className="ghost-button" onClick={logout}>
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
