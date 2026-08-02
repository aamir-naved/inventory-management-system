import { Link } from "react-router-dom";

import { useAuth } from "@/features/auth/auth-context";

export function DashboardPage() {
  const { session } = useAuth();

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Foundation dashboard</span>
        <h1>Daily business visibility in one screen.</h1>
        <p>
          These are starter cards and panels so future backend metrics can plug
          in without changing the shell.
        </p>
      </section>

      {!session?.businessId ? (
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

      <section className="card-grid">
        <article className="stat-card">
          <h3>Today&apos;s sales</h3>
          <p>Ready for dashboard metrics API</p>
          <div className="stat-value">₹0</div>
        </article>

        <article className="stat-card">
          <h3>Current stock value</h3>
          <p>Inventory valuation placeholder</p>
          <div className="stat-value">₹0</div>
        </article>

        <article className="stat-card">
          <h3>Low stock alerts</h3>
          <p>Business-scoped warning count</p>
          <div className="stat-value">0</div>
        </article>
      </section>

      <section className="panel-stack">
        <article className="panel">
          <h3>MVP modules queued up</h3>
          <div className="list">
            <div className="list-row">
              <span>Business setup</span>
              <span className={`status-chip ${session?.businessId ? "status-chip--success" : "status-chip--warn"}`}>
                {session?.businessId ? "Flow live" : "Next candidate"}
              </span>
            </div>
            <div className="list-row">
              <span>Product management</span>
              <span className="status-chip status-chip--success">Flow live</span>
            </div>
            <div className="list-row">
              <span>Inventory workflows</span>
              <span className="status-chip status-chip--success">Flow live</span>
            </div>
            <div className="list-row">
              <span>Purchase management</span>
              <span className="status-chip status-chip--success">Flow live</span>
            </div>
            <div className="list-row">
              <span>Sales management</span>
              <span className="status-chip status-chip--success">Flow live</span>
            </div>
          </div>
        </article>

        <article className="panel">
          <h3>What this foundation already gives us</h3>
          <div className="list">
            <div className="list-row">
              <span>Central API client with `X-Business-Id` support</span>
            </div>
            <div className="list-row">
              <span>Protected routes for authenticated product flows</span>
            </div>
            <div className="list-row">
              <span>Feature folders that match the product docs and backend modules</span>
            </div>
          </div>
        </article>
      </section>
    </>
  );
}
