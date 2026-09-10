import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";

import { useAuth } from "@/features/auth/auth-context";
import {
  getCustomerOutstandingReport,
  getGstReport,
  getInventoryReport,
  getPurchaseReport,
  getSalesReport,
  getSupplierOutstandingReport,
  downloadReportExcel,
} from "@/features/reports/reports-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";

type ReportTab =
  | "inventory"
  | "sales"
  | "purchases"
  | "customer-outstanding"
  | "supplier-outstanding"
  | "gst";

const reportTabs: Array<{ id: ReportTab; label: string }> = [
  { id: "inventory", label: "Inventory" },
  { id: "sales", label: "Sales" },
  { id: "purchases", label: "Purchases" },
  { id: "customer-outstanding", label: "Customer dues" },
  { id: "supplier-outstanding", label: "Supplier dues" },
  { id: "gst", label: "GST" },
];

export function ReportsPage() {
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate } = useBusinessSettings();
  const [activeTab, setActiveTab] = useState<ReportTab>("inventory");
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [fromDate, setFromDate] = useState("");
  const [toDate, setToDate] = useState("");

  const dateFilters = {
    from: fromDate || undefined,
    to: toDate || undefined,
  };

  const inventoryQuery = useQuery({
    queryKey: ["reports-inventory", businessId, lowStockOnly],
    queryFn: () => getInventoryReport(businessId!, lowStockOnly),
    enabled: Boolean(businessId) && activeTab === "inventory",
  });

  const salesQuery = useQuery({
    queryKey: ["reports-sales", businessId, fromDate, toDate],
    queryFn: () => getSalesReport(businessId!, dateFilters),
    enabled: Boolean(businessId) && activeTab === "sales",
  });

  const purchasesQuery = useQuery({
    queryKey: ["reports-purchases", businessId, fromDate, toDate],
    queryFn: () => getPurchaseReport(businessId!, dateFilters),
    enabled: Boolean(businessId) && activeTab === "purchases",
  });

  const customerOutstandingQuery = useQuery({
    queryKey: ["reports-customer-outstanding", businessId],
    queryFn: () => getCustomerOutstandingReport(businessId!),
    enabled: Boolean(businessId) && activeTab === "customer-outstanding",
  });

  const supplierOutstandingQuery = useQuery({
    queryKey: ["reports-supplier-outstanding", businessId],
    queryFn: () => getSupplierOutstandingReport(businessId!),
    enabled: Boolean(businessId) && activeTab === "supplier-outstanding",
  });

  const gstQuery = useQuery({
    queryKey: ["reports-gst", businessId, fromDate, toDate],
    queryFn: () => getGstReport(businessId!, dateFilters),
    enabled: Boolean(businessId) && activeTab === "gst",
  });

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before viewing reports.</h1>
        <p>Set up your business first so reports use the right shop data.</p>
        <Link to="/business-setup" className="primary-button">
          Complete business setup
        </Link>
      </section>
    );
  }

  const showDateFilters =
    activeTab === "sales" || activeTab === "purchases" || activeTab === "gst";
  const activeQuery =
    activeTab === "inventory"
      ? inventoryQuery
      : activeTab === "sales"
        ? salesQuery
        : activeTab === "purchases"
          ? purchasesQuery
          : activeTab === "customer-outstanding"
            ? customerOutstandingQuery
            : activeTab === "gst"
              ? gstQuery
              : supplierOutstandingQuery;

  const exportPath =
    activeTab === "inventory"
      ? `/reports/inventory.xlsx${lowStockOnly ? "?lowStockOnly=true" : ""}`
      : activeTab === "sales"
        ? `/reports/sales.xlsx${fromDate || toDate ? `?${new URLSearchParams({ ...(fromDate ? { from: fromDate } : {}), ...(toDate ? { to: toDate } : {}) }).toString()}` : ""}`
        : activeTab === "purchases"
          ? `/reports/purchases.xlsx${fromDate || toDate ? `?${new URLSearchParams({ ...(fromDate ? { from: fromDate } : {}), ...(toDate ? { to: toDate } : {}) }).toString()}` : ""}`
          : activeTab === "customer-outstanding"
            ? "/reports/outstanding/customers.xlsx"
            : activeTab === "gst"
              ? `/reports/gst.xlsx${fromDate || toDate ? `?${new URLSearchParams({ ...(fromDate ? { from: fromDate } : {}), ...(toDate ? { to: toDate } : {}) }).toString()}` : ""}`
              : "/reports/outstanding/suppliers.xlsx";

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Business reports</span>
        <h1>On-screen reports for stock, trading, and outstanding balances.</h1>
        <p>
          Inventory, sales, purchases, dues, and GST for this shop. Download Excel for
          your accountant, or use browser print.
        </p>
      </section>

      <section className="panel">
        <div className="panel-heading">
          <div>
            <h3>Choose a report</h3>
            <p>Switch between report views, then download Excel if you need a file.</p>
          </div>
          <button
            type="button"
            className="ghost-button"
            onClick={() => {
              void downloadReportExcel(businessId, exportPath, `${activeTab}-report.xlsx`);
            }}
          >
            Download Excel
          </button>
        </div>

        <div className="report-tabs" role="tablist" aria-label="Report type">
          {reportTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={activeTab === tab.id}
              className={`supplier-pill${activeTab === tab.id ? " supplier-pill--selected" : ""}`}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {activeTab === "inventory" ? (
          <div className="toggle-row report-filters">
            <label className="toggle">
              <input
                type="checkbox"
                checked={lowStockOnly}
                onChange={(event) => setLowStockOnly(event.target.checked)}
              />
              <span>Low stock only</span>
            </label>
          </div>
        ) : null}

        {showDateFilters ? (
          <div className="split-grid report-filters">
            <div className="field">
              <label htmlFor="report-from">From date</label>
              <input
                id="report-from"
                type="date"
                value={fromDate}
                onChange={(event) => setFromDate(event.target.value)}
              />
            </div>
            <div className="field">
              <label htmlFor="report-to">To date</label>
              <input
                id="report-to"
                type="date"
                value={toDate}
                onChange={(event) => setToDate(event.target.value)}
              />
            </div>
          </div>
        ) : null}

        {activeQuery.isLoading ? <p className="inline-note">Loading report…</p> : null}
        {activeQuery.isError ? (
          <p className="inline-note">Could not load this report. Refresh to try again.</p>
        ) : null}

        {activeTab === "inventory" && inventoryQuery.data ? (
          <>
            <section className="card-grid report-summary">
              <article className="stat-card">
                <h3>Products</h3>
                <div className="stat-value">{inventoryQuery.data.totalProducts}</div>
              </article>
              <article className="stat-card">
                <h3>Stock value</h3>
                <div className="stat-value">
                  {formatMoney(inventoryQuery.data.totalStockValue)}
                </div>
              </article>
              <article className="stat-card">
                <h3>Low stock</h3>
                <div className="stat-value">{inventoryQuery.data.lowStockProducts}</div>
              </article>
            </section>

            <div className="product-list">
              {inventoryQuery.data.rows.length === 0 ? (
                <p className="inline-note">No inventory rows for this filter.</p>
              ) : null}
              {inventoryQuery.data.rows.map((row) => (
                <article key={row.productId} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{row.productName}</h4>
                      <p>{row.sku ?? "No SKU"}</p>
                    </div>
                    <span
                      className={`status-chip ${
                        row.lowStock ? "status-chip--warn" : "status-chip--success"
                      }`}
                    >
                      {row.lowStock ? "Low stock" : "In stock"}
                    </span>
                  </div>
                  <div className="product-metrics">
                    <span>Stock: {Number(row.currentStock).toFixed(3)}</span>
                    <span>Value: {formatMoney(row.stockValue)}</span>
                    <span>Cost: {formatMoney(row.costPrice)}</span>
                    <span>Sell: {formatMoney(row.sellingPrice)}</span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : null}

        {activeTab === "sales" && salesQuery.data ? (
          <>
            <section className="card-grid report-summary">
              <article className="stat-card">
                <h3>Sales</h3>
                <div className="stat-value">{salesQuery.data.rowCount}</div>
              </article>
              <article className="stat-card">
                <h3>Net amount</h3>
                <div className="stat-value">
                  {formatMoney(salesQuery.data.totalNetAmount)}
                </div>
              </article>
              <article className="stat-card">
                <h3>Outstanding</h3>
                <div className="stat-value">
                  {formatMoney(salesQuery.data.totalOutstanding)}
                </div>
              </article>
            </section>

            <div className="product-list">
              {salesQuery.data.rows.length === 0 ? (
                <p className="inline-note">No sales in this date range.</p>
              ) : null}
              {salesQuery.data.rows.map((row) => (
                <article key={row.saleId} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{row.saleNumber}</h4>
                      <p>
                        {row.customerName} · {formatDate(row.saleDate)}
                      </p>
                    </div>
                    <span className="status-chip">{row.paymentStatus}</span>
                  </div>
                  <div className="product-metrics">
                    <span>Net: {formatMoney(row.netAmount)}</span>
                    <span>Paid: {formatMoney(row.amountPaid)}</span>
                    <span>Due: {formatMoney(row.outstandingAmount)}</span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : null}

        {activeTab === "purchases" && purchasesQuery.data ? (
          <>
            <section className="card-grid report-summary">
              <article className="stat-card">
                <h3>Purchases</h3>
                <div className="stat-value">{purchasesQuery.data.rowCount}</div>
              </article>
              <article className="stat-card">
                <h3>Total</h3>
                <div className="stat-value">
                  {formatMoney(purchasesQuery.data.totalAmount)}
                </div>
              </article>
              <article className="stat-card">
                <h3>Outstanding</h3>
                <div className="stat-value">
                  {formatMoney(purchasesQuery.data.totalOutstanding)}
                </div>
              </article>
            </section>

            <div className="product-list">
              {purchasesQuery.data.rows.length === 0 ? (
                <p className="inline-note">No purchases in this date range.</p>
              ) : null}
              {purchasesQuery.data.rows.map((row) => (
                <article key={row.purchaseId} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{row.purchaseNumber}</h4>
                      <p>
                        {row.supplierName} · {formatDate(row.purchaseDate)}
                      </p>
                    </div>
                    <span className="status-chip">{row.paymentStatus}</span>
                  </div>
                  <div className="product-metrics">
                    <span>Total: {formatMoney(row.totalAmount)}</span>
                    <span>Paid: {formatMoney(row.amountPaid)}</span>
                    <span>Due: {formatMoney(row.outstandingAmount)}</span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : null}

        {activeTab === "customer-outstanding" && customerOutstandingQuery.data ? (
          <>
            <section className="card-grid report-summary">
              <article className="stat-card">
                <h3>Customers</h3>
                <div className="stat-value">
                  {customerOutstandingQuery.data.customerCount}
                </div>
              </article>
              <article className="stat-card">
                <h3>Total outstanding</h3>
                <div className="stat-value">
                  {formatMoney(customerOutstandingQuery.data.totalOutstanding)}
                </div>
              </article>
            </section>

            <div className="product-list">
              {customerOutstandingQuery.data.rows.length === 0 ? (
                <p className="inline-note">No customer outstanding balances.</p>
              ) : null}
              {customerOutstandingQuery.data.rows.map((row) => (
                <article key={row.customerId} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{row.customerName}</h4>
                      <p>{row.invoiceCount} open invoice(s)</p>
                    </div>
                    <strong>{formatMoney(row.outstandingAmount)}</strong>
                  </div>
                  <div className="product-metrics">
                    <span>Billed: {formatMoney(row.netBilled)}</span>
                    <span>Paid: {formatMoney(row.amountPaid)}</span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : null}

        {activeTab === "supplier-outstanding" && supplierOutstandingQuery.data ? (
          <>
            <section className="card-grid report-summary">
              <article className="stat-card">
                <h3>Suppliers</h3>
                <div className="stat-value">
                  {supplierOutstandingQuery.data.supplierCount}
                </div>
              </article>
              <article className="stat-card">
                <h3>Total outstanding</h3>
                <div className="stat-value">
                  {formatMoney(supplierOutstandingQuery.data.totalOutstanding)}
                </div>
              </article>
            </section>

            <div className="product-list">
              {supplierOutstandingQuery.data.rows.length === 0 ? (
                <p className="inline-note">No supplier outstanding balances.</p>
              ) : null}
              {supplierOutstandingQuery.data.rows.map((row) => (
                <article key={row.supplierId} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{row.supplierName}</h4>
                      <p>{row.billCount} open bill(s)</p>
                    </div>
                    <strong>{formatMoney(row.outstandingAmount)}</strong>
                  </div>
                  <div className="product-metrics">
                    <span>Billed: {formatMoney(row.billedAmount)}</span>
                    <span>Paid: {formatMoney(row.amountPaid)}</span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : null}

        {activeTab === "gst" && gstQuery.data ? (
          <>
            <section className="card-grid report-summary">
              <article className="stat-card">
                <h3>Output tax</h3>
                <div className="stat-value">{formatMoney(gstQuery.data.outputTax)}</div>
                <p className="inline-note">Sales minus sale returns</p>
              </article>
              <article className="stat-card">
                <h3>Input tax</h3>
                <div className="stat-value">{formatMoney(gstQuery.data.inputTax)}</div>
                <p className="inline-note">Purchases minus purchase returns</p>
              </article>
              <article className="stat-card">
                <h3>Net tax</h3>
                <div className="stat-value">{formatMoney(gstQuery.data.netTax)}</div>
                <p className="inline-note">Output − input</p>
              </article>
            </section>
            <div className="product-list">
              {gstQuery.data.rows.length === 0 ? (
                <p className="inline-note">No GST rows for this date range.</p>
              ) : null}
              {gstQuery.data.rows.map((row, index) => (
                <article key={`${row.documentNumber}-${index}`} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>
                        {row.documentType} {row.documentNumber}
                      </h4>
                      <p>
                        {row.partyName} · {formatDate(row.documentDate)}
                      </p>
                    </div>
                    <strong>{formatMoney(row.taxableAmount)}</strong>
                  </div>
                  <div className="product-metrics">
                    <span>GST {row.gstRate}%</span>
                    <span>CGST {formatMoney(row.cgstAmount)}</span>
                    <span>SGST {formatMoney(row.sgstAmount)}</span>
                    <span>IGST {formatMoney(row.igstAmount)}</span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : null}
      </section>
    </>
  );
}
