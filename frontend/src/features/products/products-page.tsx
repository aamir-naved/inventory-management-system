import {
  useDeferredValue,
  useEffect,
  useState,
  type FormEvent,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import {
  archiveProduct,
  createProduct,
  listProducts,
  updateProduct,
  type ProductPayload,
  type ProductRecord,
} from "@/features/products/product-api";

const initialForm: ProductPayload = {
  name: "",
  sku: "",
  category: "",
  unit: "Pieces",
  costPrice: 0,
  sellingPrice: 0,
  openingStock: 0,
  lowStockThreshold: 0,
};

function toPayload(product: ProductRecord): ProductPayload {
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

export function ProductsPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<ProductRecord | null>(null);
  const [form, setForm] = useState<ProductPayload>(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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
      setForm(initialForm);
      return;
    }

    setForm(toPayload(selectedProduct));
  }, [selectedProduct]);

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

  function updateField<K extends keyof ProductPayload>(key: K, value: ProductPayload[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});
    await saveMutation.mutateAsync(form);
  }

  function resetForm() {
    setSelectedProduct(null);
    setForm(initialForm);
    setFieldErrors({});
    setFeedback(null);
  }

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
              <label htmlFor="product-name">Product name</label>
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
                <label htmlFor="product-sku">SKU</label>
                <input
                  id="product-sku"
                  value={form.sku}
                  onChange={(event) => updateField("sku", event.target.value)}
                  placeholder="CEM-001"
                />
                {fieldErrors.sku ? <span className="field-error">{fieldErrors.sku}</span> : null}
              </div>

              <div className="field">
                <label htmlFor="product-category">Category</label>
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
                <label htmlFor="product-unit">Unit</label>
                <input
                  id="product-unit"
                  value={form.unit}
                  onChange={(event) => updateField("unit", event.target.value)}
                  placeholder="Bags"
                />
                {fieldErrors.unit ? <span className="field-error">{fieldErrors.unit}</span> : null}
              </div>

              <div className="field">
                <label htmlFor="product-opening-stock">Opening stock</label>
                <input
                  id="product-opening-stock"
                  type="number"
                  min="0"
                  step="0.001"
                  value={form.openingStock}
                  onChange={(event) => updateField("openingStock", Number(event.target.value))}
                />
                {fieldErrors.openingStock ? (
                  <span className="field-error">{fieldErrors.openingStock}</span>
                ) : null}
              </div>
            </div>

            <div className="split-grid">
              <div className="field">
                <label htmlFor="product-low-stock-threshold">Low stock threshold</label>
                <input
                  id="product-low-stock-threshold"
                  type="number"
                  min="0"
                  step="0.001"
                  value={form.lowStockThreshold}
                  onChange={(event) => updateField("lowStockThreshold", Number(event.target.value))}
                />
                {fieldErrors.lowStockThreshold ? (
                  <span className="field-error">{fieldErrors.lowStockThreshold}</span>
                ) : null}
              </div>

              <div className="field">
                <label htmlFor="product-cost-price">Cost price</label>
                <input
                  id="product-cost-price"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.costPrice}
                  onChange={(event) => updateField("costPrice", Number(event.target.value))}
                />
                {fieldErrors.costPrice ? (
                  <span className="field-error">{fieldErrors.costPrice}</span>
                ) : null}
              </div>

              <div className="field">
                <label htmlFor="product-selling-price">Selling price</label>
                <input
                  id="product-selling-price"
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.sellingPrice}
                  onChange={(event) => updateField("sellingPrice", Number(event.target.value))}
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
                  <span>Cost: ₹{Number(product.costPrice).toFixed(2)}</span>
                  <span>Selling: ₹{Number(product.sellingPrice).toFixed(2)}</span>
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
