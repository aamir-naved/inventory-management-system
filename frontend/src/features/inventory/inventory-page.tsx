import {
  useDeferredValue,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import {
  adjustInventoryStock,
  getInventorySummary,
  listInventoryMovements,
  listInventoryStock,
  type InventoryAdjustmentPayload,
  type InventoryStockItem,
} from "@/features/inventory/inventory-api";

const initialAdjustment: InventoryAdjustmentPayload = {
  productId: "",
  adjustmentQuantity: 0,
  reason: "",
};

export function InventoryPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [selectedItem, setSelectedItem] = useState<InventoryStockItem | null>(null);
  const [adjustment, setAdjustment] = useState<InventoryAdjustmentPayload>(initialAdjustment);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const summaryQuery = useQuery({
    queryKey: ["inventory-summary", businessId],
    queryFn: () => getInventorySummary(businessId!),
    enabled: Boolean(businessId),
  });

  const stockQuery = useQuery({
    queryKey: ["inventory-stock", businessId, deferredSearch, lowStockOnly, includeArchived],
    queryFn: () =>
      listInventoryStock(businessId!, {
        search: deferredSearch,
        lowStockOnly,
        includeArchived,
      }),
    enabled: Boolean(businessId),
  });

  const movementsQuery = useQuery({
    queryKey: ["inventory-movements", businessId, selectedItem?.productId ?? null],
    queryFn: () => listInventoryMovements(businessId!, selectedItem?.productId ?? undefined),
    enabled: Boolean(businessId),
  });

  const adjustmentMutation = useMutation({
    mutationFn: async (payload: InventoryAdjustmentPayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before inventory can be managed.");
      }

      return adjustInventoryStock(businessId, payload);
    },
    onSuccess: async () => {
      setFeedback("Stock adjusted successfully.");
      setFieldErrors({});
      setAdjustment(initialAdjustment);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
      ]);
    },
    onError: (error) => {
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };

        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? "Unable to adjust stock.");
        return;
      }

      setFeedback("Unable to adjust stock.");
    },
  });

  const selectedCurrentStock = useMemo(() => {
    if (!selectedItem) {
      return null;
    }

    const latest = stockQuery.data?.find((item) => item.productId === selectedItem.productId);
    return latest ?? selectedItem;
  }, [selectedItem, stockQuery.data]);

  function chooseItem(item: InventoryStockItem) {
    setSelectedItem(item);
    setAdjustment({
      productId: item.productId,
      adjustmentQuantity: 0,
      reason: "",
    });
    setFieldErrors({});
    setFeedback(null);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFeedback(null);
    await adjustmentMutation.mutateAsync(adjustment);
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before managing inventory.</h1>
        <p>Inventory is business-scoped, so it needs the active tenant first.</p>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Inventory control</span>
        <h1>Track stock levels, value, low-stock risk, and every manual adjustment.</h1>
        <p>
          Inventory is now live across current stock, adjustment history, valuation,
          low-stock alerts, and search.
        </p>
      </section>

      <section className="card-grid">
        <article className="stat-card">
          <h3>Total products</h3>
          <p>Active catalog items in stock tracking</p>
          <div className="stat-value">{summaryQuery.data?.totalProducts ?? 0}</div>
        </article>
        <article className="stat-card">
          <h3>Current stock value</h3>
          <p>Based on cost price × current quantity</p>
          <div className="stat-value">₹{Number(summaryQuery.data?.totalStockValue ?? 0).toFixed(2)}</div>
        </article>
        <article className="stat-card">
          <h3>Low stock alerts</h3>
          <p>Products at or below their threshold</p>
          <div className="stat-value">{summaryQuery.data?.lowStockProducts ?? 0}</div>
        </article>
      </section>

      <section className="workspace-grid inventory-layout">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Stock overview</h3>
              <p>Search products and focus on low-stock items when needed.</p>
            </div>
          </div>

          <div className="split-grid">
            <div className="field">
              <label htmlFor="inventory-search">Search stock</label>
              <input
                id="inventory-search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search by product, SKU, or category"
              />
            </div>

            <div className="toggle-row">
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={lowStockOnly}
                  onChange={(event) => setLowStockOnly(event.target.checked)}
                />
                <span>Low stock only</span>
              </label>

              <label className="toggle">
                <input
                  type="checkbox"
                  checked={includeArchived}
                  onChange={(event) => setIncludeArchived(event.target.checked)}
                />
                <span>Show archived</span>
              </label>
            </div>
          </div>

          {stockQuery.isLoading ? <p className="inline-note">Loading inventory...</p> : null}

          <div className="product-list">
            {stockQuery.data?.map((item) => (
              <article
                key={item.productId}
                className={`product-card${selectedItem?.productId === item.productId ? " product-card--selected" : ""}`}
              >
                <div className="product-card__row">
                  <div>
                    <h4>{item.productName}</h4>
                    <p>
                      {item.category ?? "No category"} · {item.unit}
                    </p>
                  </div>
                  <span
                    className={`status-chip ${
                      item.lowStock ? "status-chip--warn" : "status-chip--success"
                    }`}
                  >
                    {item.lowStock ? "Low stock" : "Healthy"}
                  </span>
                </div>

                <div className="product-metrics">
                  <span>Current stock: {Number(item.currentStock).toFixed(3)}</span>
                  <span>Threshold: {Number(item.lowStockThreshold).toFixed(3)}</span>
                  <span>Value: ₹{Number(item.stockValue).toFixed(2)}</span>
                  <span>SKU: {item.sku ?? "Not set"}</span>
                </div>

                <div className="product-card__actions">
                  <button type="button" className="ghost-button" onClick={() => chooseItem(item)}>
                    Adjust stock
                  </button>
                </div>
              </article>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Manual stock adjustment</h3>
              <p>
                {selectedCurrentStock
                  ? `Adjust ${selectedCurrentStock.productName}. Positive adds stock, negative removes it.`
                  : "Select a product from the stock list to adjust its quantity."}
              </p>
            </div>
          </div>

          {selectedCurrentStock ? (
            <>
              <div className="inventory-focus">
                <strong>{selectedCurrentStock.productName}</strong>
                <span>
                  Current stock: {Number(selectedCurrentStock.currentStock).toFixed(3)} {selectedCurrentStock.unit}
                </span>
              </div>

              <form className="form-stack" onSubmit={handleSubmit}>
                <div className="field">
                  <label htmlFor="adjustment-quantity">Adjustment quantity</label>
                  <input
                    id="adjustment-quantity"
                    type="number"
                    step="0.001"
                    value={adjustment.adjustmentQuantity}
                    onChange={(event) =>
                      setAdjustment((current) => ({
                        ...current,
                        adjustmentQuantity: Number(event.target.value),
                      }))
                    }
                  />
                  {fieldErrors.adjustmentQuantity ? (
                    <span className="field-error">{fieldErrors.adjustmentQuantity}</span>
                  ) : null}
                </div>

                <div className="field">
                  <label htmlFor="adjustment-reason">Reason</label>
                  <input
                    id="adjustment-reason"
                    value={adjustment.reason}
                    onChange={(event) =>
                      setAdjustment((current) => ({
                        ...current,
                        reason: event.target.value,
                      }))
                    }
                    placeholder="Physical count correction"
                  />
                  {fieldErrors.reason ? <span className="field-error">{fieldErrors.reason}</span> : null}
                </div>

                {feedback ? <p className="inline-note">{feedback}</p> : null}

                <button
                  type="submit"
                  className="primary-button"
                  disabled={adjustmentMutation.isPending}
                >
                  {adjustmentMutation.isPending ? "Adjusting..." : "Apply adjustment"}
                </button>
              </form>

              <div className="movement-section">
                <h3>Stock history</h3>
                <div className="movement-list">
                  {movementsQuery.data?.map((movement) => (
                    <article key={movement.id} className="movement-card">
                      <div className="product-card__row">
                        <strong>{movement.movementType.split("_").join(" ")}</strong>
                        <span>{new Date(movement.createdAt).toLocaleString()}</span>
                      </div>
                      <p>{movement.notes ?? "No notes"}</p>
                      <div className="product-metrics">
                        <span>Change: {Number(movement.quantityChange).toFixed(3)}</span>
                        <span>Before: {Number(movement.quantityBefore).toFixed(3)}</span>
                        <span>After: {Number(movement.quantityAfter).toFixed(3)}</span>
                      </div>
                    </article>
                  ))}
                </div>
              </div>
            </>
          ) : (
            <div className="empty-inline-state">
              <strong>No product selected</strong>
              <p>Choose a stock item from the left to apply a manual adjustment and view its history.</p>
            </div>
          )}
        </article>
      </section>
    </>
  );
}
