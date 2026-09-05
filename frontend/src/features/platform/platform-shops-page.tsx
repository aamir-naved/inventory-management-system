import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { PaginationBar } from "@/components/ui/pagination-bar";
import { listPlatformShops } from "@/features/platform/platform-api";

export function PlatformShopsPage() {
  const [status, setStatus] = useState("all");
  const [search, setSearch] = useState("");
  const [page, setPage] = useState(0);

  const shopsQuery = useQuery({
    queryKey: ["platform-shops", status, search, page],
    queryFn: () => listPlatformShops({ status, search, page }),
  });

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Tenants</span>
        <h1>Shops</h1>
        <p>Each row is one isolated business. Owners never see another shop’s data.</p>
      </section>
      <section className="panel">
        <div className="helper-row">
          <Link to="/platform/shops/new" className="primary-button">
            Add shop
          </Link>
          <select
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
          >
            <option value="all">All shops</option>
            <option value="active">Active shops</option>
            <option value="suspended">Suspended shops</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="shop-search">Search</label>
          <input
            id="shop-search"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder="Name or mobile"
          />
        </div>
        {shopsQuery.isError ? <p className="form-error">Unable to load shops.</p> : null}
        <ul className="list">
          {(shopsQuery.data?.items ?? []).map((shop) => (
            <li key={shop.id} className="product-card">
              <strong>{shop.name}</strong>
              <p>
                {shop.active ? "Active" : "Suspended"} · {shop.planCode} · {shop.ownerName ?? "No owner"}{" "}
                {shop.ownerEmail ? `(${shop.ownerEmail})` : ""}
              </p>
              <Link to={`/platform/shops/${shop.id}`} className="ghost-button">
                Open
              </Link>
            </li>
          ))}
        </ul>
        <PaginationBar page={shopsQuery.data} onPageChange={setPage} />
      </section>
    </>
  );
}
