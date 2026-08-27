import {
  useDeferredValue,
  useEffect,
  useState,
  type FormEvent,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { FieldLabel } from "@/components/ui/field-label";
import { useAuth } from "@/features/auth/auth-context";
import {
  parseNumericDraft,
  resolveNumericDraft,
  type NumericDraft,
} from "@/lib/numeric-draft";
import {
  archiveProduct,
  createProduct,
  listProducts,
  updateProduct,
  type ProductPayload,
  type ProductRecord,
} from "@/features/products/product-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";

const COMMON_UNITS = [
  "Pieces",
  "Bags",
  "Kg",
  "Grams",
  "Liters",
  "Ml",
  "Boxes",
  "Packs",
  "Tons",
  "Meters",
  "Rolls",
  "Dozen",
] as const;

type ProductFormState = {
  name: string;
  sku: string;
  category: string;
  unit: string;
  costPrice: NumericDraft;
  sellingPrice: NumericDraft;
  openingStock: NumericDraft;
  lowStockThreshold: NumericDraft;
};

const initialForm: ProductFormState = {
  name: "",
  sku: "",
  category: "",
  unit: "Pieces",
  costPrice: "",
  sellingPrice: "",
  openingStock: "",
  lowStockThreshold: "",
};

function isCommonUnit(unit: string): unit is (typeof COMMON_UNITS)[number] {
  return (COMMON_UNITS as readonly string[]).includes(unit);
}

function toFormState(product: ProductRecord): ProductFormState {
  return {
    name: product.name,
    sku: product.sku ?? "",
    category: product.category ?? "",
    unit: product.unit,
    costPrice: Number(product.costPrice),
    sellingPrice: Number(product.sellingPrice),
    openingStock: Number(product.openingStock),
    lowStockThreshold: Number(product.lowStockThreshold),
  };
}

function toPayload(form: ProductFormState): ProductPayload {
  return {
    name: form.name,
    sku: form.sku,
    category: form.category,
    unit: form.unit.trim() || "Pieces",
    costPrice: resolveNumericDraft(form.costPrice),
    sellingPrice: resolveNumericDraft(form.sellingPrice),
    openingStock: resolveNumericDraft(form.openingStock),
    lowStockThreshold: resolveNumericDraft(form.lowStockThreshold),
  };
}

export function ProductsPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, defaultLowStockThreshold } = useBusinessSettings();
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<ProductRecord | null>(null);
  const [form, setForm] = useState<ProductFormState>(initialForm);
  const [unitMode, setUnitMode] = useState<"preset" | "other">("preset");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  function blankForm(): ProductFormState {
    return {
      ...initialForm,
      lowStockThreshold:
        defaultLowStockThreshold > 0 ? defaultLowStockThreshold : "",
    };
  }

  const productsQuery = useQuery({
    queryKey: ["products", businessId, deferredSearch, includeArchived],
    queryFn: () =>
      listProducts({
        businessId: businessId!,
        search: deferredSearch,
        includeArchived,
      }),
    enabled: Boolean(businessId),
  });

  useEffect(() => {
    if (!selectedProduct) {
      setForm(blankForm());
      setUnitMode("preset");
      return;
    }

    const nextForm = toFormState(selectedProduct);
    setForm(nextForm);
    setUnitMode(isCommonUnit(nextForm.unit) ? "preset" : "other");
  }, [selectedProduct, defaultLowStockThreshold]);

  const saveMutation = useMutation({
    mutationFn: async (payload: ProductPayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before products can be managed.");
      }

      if (selectedProduct) {
        return updateProduct(businessId, selectedProduct.id, payload);
      }

      return createProduct(businessId, payload);
    },
    onSuccess: async (product) => {
      setFeedback(selectedProduct ? "Product updated." : "Product created.");
      setFieldErrors({});
      setSelectedProduct(product);
      await queryClient.invalidateQueries({ queryKey: ["products", businessId] });
    },
    onError: (error) => {
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };

        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? "Unable to save product.");
        return;
      }

      setFeedback("Unable to save product.");
    },
  });

  const archiveMutation = useMutation({
    mutationFn: async (product: ProductRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before products can be managed.");
      }

      return archiveProduct(businessId, product.id);
    },
    onSuccess: async (product) => {
      setFeedback(`${product.name} archived.`);
      if (selectedProduct?.id === product.id) {
        setSelectedProduct(null);
      }
      await queryClient.invalidateQueries({ queryKey: ["products", businessId] });
    },
    onError: () => {
      setFeedback("Unable to archive product.");
    },
  });

  function updateField<K extends keyof ProductFormState>(key: K, value: ProductFormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  function handleUnitSelect(value: string) {
    if (value === "Other") {
      setUnitMode("other");
      updateField("unit", isCommonUnit(form.unit) ? "" : form.unit);
      return;
    }

    setUnitMode("preset");
    updateField("unit", value);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});

    if (!form.unit.trim()) {
      setFieldErrors({ unit: "Unit is required." });
      setFeedback("Choose a unit or enter a custom one.");
      return;
    }

    await saveMutation.mutateAsync(toPayload(form));
  }

  function resetForm() {
    setSelectedProduct(null);
    setForm(blankForm());
    setUnitMode("preset");
    setFieldErrors({});
    setFeedback(null);
  }

  const unitSelectValue = unitMode === "other" || !isCommonUnit(form.unit) ? "Other" : form.unit;

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before adding products.</h1>
        <p>
          Products are tenant-scoped, so we need the business profile saved first.
        </p>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Product management</span>
        <h1>Build the product catalog that powers inventory, purchases, and sales.</h1>
        <p>
          This flow is now live end to end: create, edit, search, and archive
          products for the active business.
        </p>
      </section>

      <section className="workspace-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>{selectedProduct ? "Edit product" : "Create product"}</h3>
              <p>Opening stock becomes the initial current stock on creation.</p>
            </div>
            {selectedProduct ? (
              <button type="button" className="ghost-button" onClick={resetForm}>
                New product
              </button>
            ) : null}
          </div>

          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <FieldLabel
                htmlFor="product-name"
                label="Product name"
                info="The display name shown in catalog, purchases, sales, and inventory."
              />
              <input
                id="product-name"
                value={form.name}
                onChange={(event) => updateField("name", event.target.value)}
                placeholder="Ultra Cement"
              />
              {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
            </div>

            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="product-sku"
                  label="SKU"
                  info="Optional stock-keeping code for quick search and identification (for example CEM-001)."
                />
                <input
                  id="product-sku"
                  value={form.sku}
                  onChange={(event) => updateField("sku", event.target.value)}
                  placeholder="CEM-001"
                />
                {fieldErrors.sku ? <span className="field-error">{fieldErrors.sku}</span> : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="product-category"
                  label="Category"
                  info="Optional group used for filtering, such as Cement, Steel, or Hardware."
                />
                <input
                  id="product-category"
                  value={form.category}
                  onChange={(event) => updateField("category", event.target.value)}
                  placeholder="Cement"
                />
                {fieldErrors.category ? (
                  <span className="field-error">{fieldErrors.category}</span>
                ) : null}
              </div>
            </div>

            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="product-unit"
                  label="Unit"
                  info="How this product is measured when stocking, buying, or selling — Bags, Kg, Pieces, and so on."
                />
                <select
                  id="product-unit"
                  value={unitSelectValue}
                  onChange={(event) => handleUnitSelect(event.target.value)}
                >
                  {COMMON_UNITS.map((unit) => (
                    <option key={unit} value={unit}>
                      {unit}
                    </option>
                  ))}
                  <option value="Other">Other</option>
                </select>
                {unitSelectValue === "Other" ? (
                  <input
                    id="product-unit-custom"
                    value={form.unit}
                    onChange={(event) => updateField("unit", event.target.value)}
                    placeholder="Enter custom unit"
                    aria-label="Custom unit"
                  />
                ) : null}
                {fieldErrors.unit ? <span className="field-error">{fieldErrors.unit}</span> : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="product-opening-stock"
                  label="Opening stock"
                  info="Quantity you already have on hand. On create, this becomes the product's starting current stock. Leave blank or 0 if you have none yet."
                />
                <input
                  id="product-opening-stock"
                  type="number"
                  min="0"
                  step="0.001"
                  inputMode="decimal"
                  value={form.openingStock}
                  onChange={(event) =>
                    updateField("openingStock", parseNumericDraft(event.target.value))
                  }
                  placeholder="0"
                />
                {fieldErrors.openingStock ? (
                  <span className="field-error">{fieldErrors.openingStock}</span>
                ) : null}
              </div>
            </div>

            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="product-low-stock-threshold"
                  label="Low stock threshold"
                  info="When current stock reaches this level, the product is treated as low stock so you can reorder in time."
                />
                <input
                  id="product-low-stock-threshold"
                  type="number"
                  min="0"
                  step="0.001"
                  inputMode="decimal"
                  value={form.lowStockThreshold}
                  onChange={(event) =>
                    updateField("lowStockThreshold", parseNumericDraft(event.target.value))
                  }
                  placeholder="0"
                />
                {fieldErrors.lowStockThreshold ? (
                  <span className="field-error">{fieldErrors.lowStockThreshold}</span>
                ) : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="product-cost-price"
                  label="Cost price"
                  info="What you typically pay per unit when purchasing this product."
                />
                <input
                  id="product-cost-price"
                  type="number"
                  min="0"
                  step="0.01"
                  inputMode="decimal"
                  value={form.costPrice}
                  onChange={(event) =>
                    updateField("costPrice", parseNumericDraft(event.target.value))
                  }
                  placeholder="0"
                />
                {fieldErrors.costPrice ? (
                  <span className="field-error">{fieldErrors.costPrice}</span>
                ) : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="product-selling-price"
                  label="Selling price"
                  info="What you typically charge customers per unit when selling this product."
                />
                <input
                  id="product-selling-price"
                  type="number"
                  min="0"
                  step="0.01"
                  inputMode="decimal"
                  value={form.sellingPrice}
                  onChange={(event) =>
                    updateField("sellingPrice", parseNumericDraft(event.target.value))
                  }
                  placeholder="0"
                />
                {fieldErrors.sellingPrice ? (
                  <span className="field-error">{fieldErrors.sellingPrice}</span>
                ) : null}
              </div>
            </div>

            {feedback ? <p className="inline-note">{feedback}</p> : null}

            <button type="submit" className="primary-button" disabled={saveMutation.isPending}>
              {saveMutation.isPending
                ? "Saving..."
                : selectedProduct
                  ? "Save product"
                  : "Create product"}
            </button>
          </form>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Product catalog</h3>
              <p>Search by product name, SKU, or category.</p>
            </div>
            <label className="toggle">
              <input
                type="checkbox"
                checked={includeArchived}
                onChange={(event) => setIncludeArchived(event.target.checked)}
              />
              <span>Show archived</span>
            </label>
          </div>

          <div className="field">
            <label htmlFor="product-search">Search products</label>
            <input
              id="product-search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by name, SKU, or category"
            />
          </div>

          {productsQuery.isLoading ? <p className="inline-note">Loading products...</p> : null}

          {!productsQuery.isLoading && (productsQuery.data?.length ?? 0) === 0 ? (
            <div className="empty-inline-state">
              <strong>No products yet</strong>
              <p>Create your first product to start inventory tracking.</p>
            </div>
          ) : null}

          <div className="product-list">
            {productsQuery.data?.map((product) => (
              <article key={product.id} className="product-card">
                <div className="product-card__row">
                  <div>
                    <h4>{product.name}</h4>
                    <p>
                      {product.category ?? "No category"} · {product.unit}
                    </p>
                  </div>
                  <span
                    className={`status-chip ${
                      product.archived ? "status-chip--warn" : "status-chip--success"
                    }`}
                  >
                    {product.archived ? "Archived" : "Active"}
                  </span>
                </div>

                <div className="product-metrics">
                  <span>SKU: {product.sku ?? "Not set"}</span>
                  <span>Cost: {formatMoney(product.costPrice)}</span>
                  <span>Selling: {formatMoney(product.sellingPrice)}</span>
                  <span>Stock: {Number(product.currentStock).toFixed(3)}</span>
                  <span>Low stock at: {Number(product.lowStockThreshold).toFixed(3)}</span>
                </div>

                <div className="product-card__actions">
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => {
                      setFeedback(null);
                      setFieldErrors({});
                      setSelectedProduct(product);
                    }}
                  >
                    Edit
                  </button>
                  {!product.archived ? (
                    <button
                      type="button"
                      className="ghost-button ghost-button--danger"
                      disabled={archiveMutation.isPending}
                      onClick={() => archiveMutation.mutate(product)}
                    >
                      Archive
                    </button>
                  ) : null}
                </div>
              </article>
            ))}
          </div>
        </article>
      </section>
    </>
  );
}
