import {
  useDeferredValue,
  useEffect,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { EntityPicker } from "@/components/ui/entity-picker";
import { FieldInfo, FieldLabel } from "@/components/ui/field-label";
import { PaginationBar } from "@/components/ui/pagination-bar";
import { useAuth } from "@/features/auth/auth-context";
import { computeGstLine } from "@/lib/gst";
import { useBusinessSettings } from "@/features/settings/use-business-settings";
import {
  parseNumericDraft,
  resolveNumericDraft,
  type NumericDraft,
} from "@/lib/numeric-draft";
import {
  createPurchasePayment,
  listPurchasePayments,
  type PaymentPayload,
} from "@/features/payments/payment-api";
import {
  downloadPurchaseBill,
  printPurchaseBill,
} from "@/features/documents/document-api";
import {
  createPurchase,
  createPurchaseReturn,
  cancelPurchase,
  getPurchase,
  listPurchaseReturns,
  listPurchases,
  updatePurchase,
  type PurchasePayload,
  type PurchaseRecord,
} from "@/features/purchases/purchase-api";
import {
  createSupplier,
  getSupplier,
  listSuppliers,
  type SupplierPayload,
  type SupplierRecord,
} from "@/features/suppliers/supplier-api";
import { getProduct, listProducts, type ProductRecord } from "@/features/products/product-api";

type PurchaseItemDraft = {
  productId: string;
  quantity: NumericDraft;
  purchasePrice: NumericDraft;
  gstRate: number;
};

type PurchaseFormState = {
  supplierId: string;
  purchaseDate: string;
  amountPaid: NumericDraft;
  notes: string;
  interstate: boolean;
  items: PurchaseItemDraft[];
};

const initialSupplier: SupplierPayload = {
  name: "",
  contactPerson: "",
  mobileNumber: "",
  addressLine: "",
};

const initialPurchase: PurchaseFormState = {
  supplierId: "",
  purchaseDate: new Date().toISOString().slice(0, 10),
  amountPaid: "",
  notes: "",
  interstate: false,
  items: [],
};

type PaymentFormState = {
  paymentDate: string;
  amount: NumericDraft;
  notes: string;
};

type ReturnFormState = {
  returnDate: string;
  reason: string;
  notes: string;
  items: Array<{ purchaseItemId: string; quantity: NumericDraft }>;
};

const initialPaymentForm: PaymentFormState = {
  paymentDate: new Date().toISOString().slice(0, 10),
  amount: "",
  notes: "",
};

function emptyItem(product?: ProductRecord): PurchaseItemDraft {
  return {
    productId: product?.id ?? "",
    quantity: "",
    purchasePrice: product ? Number(product.costPrice) : "",
    gstRate: product ? Number(product.gstRate ?? 0) : 0,
  };
}

function summarizeTotal(
  items: PurchaseItemDraft[],
  gstEnabled: boolean,
  inclusive: boolean,
  interstate: boolean,
) {
  return items.reduce((total, item) => {
    const line = computeGstLine(
      resolveNumericDraft(item.quantity),
      resolveNumericDraft(item.purchasePrice),
      gstEnabled ? item.gstRate : 0,
      inclusive,
      interstate,
    );
    return total + line.lineTotal;
  }, 0);
}

function toPurchasePayload(form: PurchaseFormState): PurchasePayload {
  return {
    supplierId: form.supplierId,
    purchaseDate: form.purchaseDate,
    amountPaid: resolveNumericDraft(form.amountPaid),
    notes: form.notes,
    interstate: form.interstate,
    items: form.items.map((item) => ({
      productId: item.productId,
      quantity: resolveNumericDraft(item.quantity),
      purchasePrice: resolveNumericDraft(item.purchasePrice),
      gstRate: item.gstRate,
    })),
  };
}

export function PurchasesPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate, gstEnabled, gstInclusivePricing } = useBusinessSettings();
  const [supplierForm, setSupplierForm] = useState<SupplierPayload>(initialSupplier);
  const [purchaseForm, setPurchaseForm] = useState<PurchaseFormState>(initialPurchase);
  const [purchaseSearch, setPurchaseSearch] = useState("");
  const deferredPurchaseSearch = useDeferredValue(purchaseSearch);
  const [purchasePage, setPurchasePage] = useState(0);
  const [supplierListSearch, setSupplierListSearch] = useState("");
  const deferredSupplierListSearch = useDeferredValue(supplierListSearch);
  const [supplierListPage, setSupplierListPage] = useState(0);
  const [knownProducts, setKnownProducts] = useState<Record<string, ProductRecord>>({});
  const [selectedPurchase, setSelectedPurchase] = useState<PurchaseRecord | null>(null);
  const [cancellationReason, setCancellationReason] = useState("");
  const [returnForm, setReturnForm] = useState<ReturnFormState>({
    returnDate: new Date().toISOString().slice(0, 10),
    reason: "",
    notes: "",
    items: [],
  });
  const [paymentForm, setPaymentForm] = useState<PaymentFormState>(initialPaymentForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const suppliersQuery = useQuery({
    queryKey: ["suppliers", businessId, deferredSupplierListSearch, false, supplierListPage],
    queryFn: () =>
      listSuppliers(businessId!, {
        search: deferredSupplierListSearch,
        page: supplierListPage,
      }),
    enabled: Boolean(businessId),
  });

  const purchasesQuery = useQuery({
    queryKey: ["purchases", businessId, deferredPurchaseSearch, purchasePage],
    queryFn: () => listPurchases(businessId!, deferredPurchaseSearch, { page: purchasePage }),
    enabled: Boolean(businessId),
  });

  const purchaseReturnsQuery = useQuery({
    queryKey: ["purchase-returns", businessId, selectedPurchase?.id],
    queryFn: () => listPurchaseReturns(businessId!, selectedPurchase!.id),
    enabled: Boolean(businessId && selectedPurchase?.id),
  });

  const purchasePaymentsQuery = useQuery({
    queryKey: ["purchase-payments", businessId, selectedPurchase?.id],
    queryFn: () => listPurchasePayments(businessId!, selectedPurchase!.id),
    enabled: Boolean(businessId && selectedPurchase?.id),
  });

  useEffect(() => {
    setPurchasePage(0);
  }, [deferredPurchaseSearch]);

  useEffect(() => {
    setSupplierListPage(0);
  }, [deferredSupplierListSearch]);

  const supplierMutation = useMutation({
    mutationFn: async (payload: SupplierPayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before purchases can be managed.");
      }

      return createSupplier(businessId, payload);
    },
    onSuccess: async (supplier) => {
      setFeedback("Supplier created.");
      setSupplierForm(initialSupplier);
      setPurchaseForm((current) => ({ ...current, supplierId: supplier.id }));
      await queryClient.invalidateQueries({ queryKey: ["suppliers", businessId] });
    },
    onError: handleApiError,
  });

  const purchaseMutation = useMutation({
    mutationFn: async (payload: PurchasePayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before purchases can be managed.");
      }

      return createPurchase(businessId, payload);
    },
    onSuccess: async (purchase) => {
      setFeedback("Purchase recorded. Stock increased for the products you bought.");
      setSelectedPurchase(purchase);
      setPurchaseForm({
        ...initialPurchase,
        purchaseDate: new Date().toISOString().slice(0, 10),
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["purchases", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
      ]);
    },
    onError: handleApiError,
  });

  const updatePurchaseMutation = useMutation({
    mutationFn: async (purchase: PurchaseRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before purchases can be managed.");
      }

      return updatePurchase(businessId, purchase.id, {
        purchaseDate: purchase.purchaseDate,
        notes: purchase.notes ?? "",
      });
    },
    onSuccess: async (purchase) => {
      setFeedback("Purchase details updated.");
      setSelectedPurchase(purchase);
      await queryClient.invalidateQueries({ queryKey: ["purchases", businessId] });
    },
    onError: handleApiError,
  });

  const paymentMutation = useMutation({
    mutationFn: async () => {
      if (!businessId || !selectedPurchase) {
        throw new Error("Select a purchase before recording a payment.");
      }

      if (paymentForm.amount === "" || resolveNumericDraft(paymentForm.amount) <= 0) {
        throw new Error("Enter a payment amount.");
      }

      const payload: PaymentPayload = {
        paymentDate: paymentForm.paymentDate,
        amount: resolveNumericDraft(paymentForm.amount),
        notes: paymentForm.notes,
      };

      return createPurchasePayment(businessId, selectedPurchase.id, payload);
    },
    onSuccess: async () => {
      setFeedback("Payment recorded.");
      setPaymentForm({
        ...initialPaymentForm,
        paymentDate: new Date().toISOString().slice(0, 10),
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["purchases", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["purchase-payments", businessId] }),
      ]);

      const refreshed = selectedPurchase
        ? await getPurchase(businessId!, selectedPurchase.id)
        : null;
      if (refreshed) {
        setSelectedPurchase(refreshed);
      }
    },
    onError: (error) => {
      if (error instanceof Error && !(error instanceof ApiError)) {
        setFeedback(error.message);
        return;
      }
      handleApiError(error);
    },
  });

  const cancelPurchaseMutation = useMutation({
    mutationFn: async () => {
      if (!businessId || !selectedPurchase) {
        throw new Error("Select a purchase before cancelling.");
      }

      return cancelPurchase(businessId, selectedPurchase.id, cancellationReason);
    },
    onSuccess: async (purchase) => {
      setFeedback("Purchase cancelled and stock reversed.");
      setSelectedPurchase(purchase);
      setCancellationReason("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["purchases", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
      ]);
    },
    onError: handleApiError,
  });

  const returnPurchaseMutation = useMutation({
    mutationFn: async () => {
      if (!businessId || !selectedPurchase) {
        throw new Error("Select a purchase before recording a return.");
      }

      const items = returnForm.items
        .map((item) => ({
          purchaseItemId: item.purchaseItemId,
          quantity: resolveNumericDraft(item.quantity),
        }))
        .filter((item) => item.quantity > 0);
      if (items.length === 0) {
        throw new Error("Add at least one return quantity.");
      }

      return createPurchaseReturn(businessId, selectedPurchase.id, {
        ...returnForm,
        items,
      });
    },
    onSuccess: async (purchaseReturn) => {
      setFeedback("Purchase return recorded and stock reduced.");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["purchases", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["purchase-returns", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["purchase-payments", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["products", businessId] }),
      ]);

      const refreshed = await getPurchase(businessId!, purchaseReturn.purchaseId);
      selectPurchase(refreshed);
    },
    onError: (error) => {
      if (error instanceof Error && !(error instanceof ApiError)) {
        setFeedback(error.message);
        return;
      }
      handleApiError(error);
    },
  });

  function handleApiError(error: unknown) {
    if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
      const response = error.details as {
        fieldErrors?: Record<string, string>;
        message?: string;
      };

      setFieldErrors(response.fieldErrors ?? {});
      setFeedback(response.message ?? "Request failed.");
      return;
    }

    setFeedback("Request failed.");
  }

  const purchaseTotal = useMemo(
    () =>
      summarizeTotal(
        purchaseForm.items,
        gstEnabled,
        gstInclusivePricing,
        purchaseForm.interstate,
      ),
    [purchaseForm.items, purchaseForm.interstate, gstEnabled, gstInclusivePricing],
  );
  const amountPaidNow = resolveNumericDraft(purchaseForm.amountPaid);
  const outstandingAfterSave = Math.max(purchaseTotal - amountPaidNow, 0);

  const returnTotal = useMemo(() => {
    if (!selectedPurchase) {
      return 0;
    }

    return returnForm.items.reduce((total, item) => {
      const purchaseItem = selectedPurchase.items.find(
        (candidate) => candidate.id === item.purchaseItemId,
      );
      if (!purchaseItem) {
        return total;
      }
      return total + resolveNumericDraft(item.quantity) * Number(purchaseItem.purchasePrice);
    }, 0);
  }, [returnForm.items, selectedPurchase]);

  function selectPurchase(purchase: PurchaseRecord) {
    setSelectedPurchase(purchase);
    setCancellationReason("");
    setPaymentForm({
      ...initialPaymentForm,
      paymentDate: new Date().toISOString().slice(0, 10),
      amount:
        Number(purchase.outstandingAmount) > 0 ? Number(purchase.outstandingAmount) : "",
    });
    setReturnForm({
      returnDate: new Date().toISOString().slice(0, 10),
      reason: "",
      notes: "",
      items: purchase.items
        .filter((item) => Number(item.returnableQuantity) > 0)
        .map((item) => ({
          purchaseItemId: item.id,
          quantity: "",
        })),
    });
    setFeedback(null);
    setFieldErrors({});
  }

  function updateReturnQuantity(purchaseItemId: string, quantity: NumericDraft) {
    setReturnForm((current) => {
      const existing = current.items.find((item) => item.purchaseItemId === purchaseItemId);
      if (!existing) {
        return {
          ...current,
          items: [...current.items, { purchaseItemId, quantity }],
        };
      }

      return {
        ...current,
        items: current.items.map((item) =>
          item.purchaseItemId === purchaseItemId ? { ...item, quantity } : item,
        ),
      };
    });
  }

  function addItem() {
    setPurchaseForm((current) => ({
      ...current,
      items: [...current.items, emptyItem()],
    }));
  }

  function updateItem(index: number, nextItem: PurchaseItemDraft) {
    setPurchaseForm((current) => ({
      ...current,
      items: current.items.map((item, itemIndex) => (itemIndex === index ? nextItem : item)),
    }));
  }

  function removeItem(index: number) {
    setPurchaseForm((current) => ({
      ...current,
      items: current.items.filter((_, itemIndex) => itemIndex !== index),
    }));
  }

  async function handleCreateSupplier(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFeedback(null);
    await supplierMutation.mutateAsync(supplierForm);
  }

  async function handleCreatePurchase(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFeedback(null);

    if (purchaseForm.items.length === 0) {
      setFeedback("Add at least one product to this purchase.");
      return;
    }

    const incompleteItem = purchaseForm.items.find(
      (item) =>
        !item.productId ||
        item.quantity === "" ||
        resolveNumericDraft(item.quantity) <= 0 ||
        item.purchasePrice === "",
    );

    if (incompleteItem) {
      setFeedback("Each item needs a product, quantity, and purchase price.");
      return;
    }

    await purchaseMutation.mutateAsync(toPurchasePayload(purchaseForm));
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before recording purchases.</h1>
        <p>Set up your business first so purchases belong to the right shop.</p>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Purchase management</span>
        <h1>Record stock inflow, track supplier purchases, and reverse mistakes safely.</h1>
        <p>
          This workspace handles supplier setup, purchase entry, stock increase,
          history, payment status, and purchase cancellation.
        </p>
      </section>

      <section className="workspace-grid purchases-layout">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Quick supplier setup</h3>
              <p>Create a supplier here so the purchase form can select it immediately.</p>
            </div>
          </div>

          <form className="form-stack" onSubmit={handleCreateSupplier}>
            <div className="field">
              <label htmlFor="supplier-name">Supplier name</label>
              <input
                id="supplier-name"
                value={supplierForm.name}
                onChange={(event) => setSupplierForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Shakti Cements Ltd"
              />
              {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
            </div>

            <div className="split-grid">
              <div className="field">
                <label htmlFor="supplier-contact">Contact person</label>
                <input
                  id="supplier-contact"
                  value={supplierForm.contactPerson}
                  onChange={(event) =>
                    setSupplierForm((current) => ({ ...current, contactPerson: event.target.value }))
                  }
                  placeholder="Rahul Mehta"
                />
              </div>

              <div className="field">
                <label htmlFor="supplier-mobile">Mobile number</label>
                <input
                  id="supplier-mobile"
                  value={supplierForm.mobileNumber}
                  onChange={(event) =>
                    setSupplierForm((current) => ({ ...current, mobileNumber: event.target.value }))
                  }
                  placeholder="+91 9876543210"
                />
                {fieldErrors.mobileNumber ? (
                  <span className="field-error">{fieldErrors.mobileNumber}</span>
                ) : null}
              </div>
            </div>

            <div className="field">
              <label htmlFor="supplier-address">Address</label>
              <input
                id="supplier-address"
                value={supplierForm.addressLine}
                onChange={(event) =>
                  setSupplierForm((current) => ({ ...current, addressLine: event.target.value }))
                }
                placeholder="Industrial Road"
              />
            </div>

            <button type="submit" className="ghost-button" disabled={supplierMutation.isPending}>
              {supplierMutation.isPending ? "Creating supplier..." : "Create supplier"}
            </button>
          </form>

          <div className="field">
            <label htmlFor="purchase-supplier-search">Search suppliers</label>
            <input
              id="purchase-supplier-search"
              value={supplierListSearch}
              onChange={(event) => setSupplierListSearch(event.target.value)}
              placeholder="Search suppliers"
            />
          </div>

          <div className="supplier-list">
            {suppliersQuery.data?.items.map((supplier) => (
              <button
                key={supplier.id}
                type="button"
                className={`supplier-pill${purchaseForm.supplierId === supplier.id ? " supplier-pill--selected" : ""}`}
                onClick={() =>
                  setPurchaseForm((current) => ({
                    ...current,
                    supplierId: supplier.id,
                  }))
                }
              >
                {supplier.name}
              </button>
            ))}
          </div>
          <PaginationBar page={suppliersQuery.data} onPageChange={setSupplierListPage} />
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Create purchase</h3>
              <p>
                Record stock you bought from a supplier. Saving this adds quantity to inventory
                and tracks how much you paid versus what you still owe.
              </p>
            </div>
          </div>

          <form className="form-stack" onSubmit={handleCreatePurchase}>
            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="purchase-supplier"
                  label="Supplier"
                  info="Who you bought from. Create a supplier on the left if they are not listed yet."
                />
                <EntityPicker
                  id="purchase-supplier"
                  value={purchaseForm.supplierId}
                  enabled={Boolean(businessId)}
                  placeholder="Search suppliers"
                  queryKey={["suppliers", businessId]}
                  fetchPage={(search) => listSuppliers(businessId!, { search })}
                  fetchById={(id) => getSupplier(businessId!, id)}
                  getId={(supplier: SupplierRecord) => supplier.id}
                  getLabel={(supplier: SupplierRecord) => supplier.name}
                  onChange={(supplierId) =>
                    setPurchaseForm((current) => ({ ...current, supplierId }))
                  }
                />
                {fieldErrors.supplierId ? <span className="field-error">{fieldErrors.supplierId}</span> : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="purchase-date"
                  label="Purchase date"
                  info="The date this stock was purchased or received."
                />
                <input
                  id="purchase-date"
                  type="date"
                  value={purchaseForm.purchaseDate}
                  onChange={(event) =>
                    setPurchaseForm((current) => ({ ...current, purchaseDate: event.target.value }))
                  }
                />
              </div>
            </div>

            {gstEnabled ? (
              <label className="toggle">
                <input
                  type="checkbox"
                  checked={purchaseForm.interstate}
                  onChange={(event) =>
                    setPurchaseForm((current) => ({
                      ...current,
                      interstate: event.target.checked,
                    }))
                  }
                />
                <span>Interstate purchase (IGST)</span>
              </label>
            ) : null}

            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="purchase-amount-paid"
                  label="Amount paid now"
                  info="Cash or transfer paid to the supplier today. Leave blank or 0 if you will pay later — the unpaid part becomes outstanding."
                />
                <input
                  id="purchase-amount-paid"
                  type="number"
                  min="0"
                  step="0.01"
                  inputMode="decimal"
                  value={purchaseForm.amountPaid}
                  onChange={(event) =>
                    setPurchaseForm((current) => ({
                      ...current,
                      amountPaid: parseNumericDraft(event.target.value),
                    }))
                  }
                  placeholder="0"
                />
                {fieldErrors.amountPaid ? (
                  <span className="field-error">{fieldErrors.amountPaid}</span>
                ) : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="purchase-notes"
                  label="Notes"
                  info="Optional reminder for this purchase, such as invoice number or delivery reference."
                />
                <input
                  id="purchase-notes"
                  value={purchaseForm.notes}
                  onChange={(event) =>
                    setPurchaseForm((current) => ({ ...current, notes: event.target.value }))
                  }
                  placeholder="Restocking cement"
                />
              </div>
            </div>

            <div className="purchase-items">
              <div className="panel-heading">
                <div>
                  <div className="field-label-row">
                    <h3>What did you buy?</h3>
                    <FieldInfo label="Purchase items">
                      Add each product you received. Quantity increases stock using the product&apos;s
                      unit (Bags, Kg, etc.). Purchase price is what you paid per unit.
                    </FieldInfo>
                  </div>
                  <p>For each line: choose the product, enter how many units, and the price per unit.</p>
                </div>
                <button type="button" className="ghost-button" onClick={addItem}>
                  Add item
                </button>
              </div>

              {purchaseForm.items.length === 0 ? (
                <div className="empty-inline-state">
                  <strong>No items yet</strong>
                  <p>Click Add item, then fill product, quantity, and purchase price.</p>
                </div>
              ) : null}

              {purchaseForm.items.map((item, index) => {
                const product = knownProducts[item.productId];
                const lineTotal =
                  resolveNumericDraft(item.quantity) * resolveNumericDraft(item.purchasePrice);

                return (
                  <div key={`purchase-item-${index}`} className="purchase-item-card">
                    <div className="purchase-item-row purchase-item-row--compose">
                      <div className="field">
                        <FieldLabel
                          htmlFor={`purchase-item-product-${index}`}
                          label="Product"
                          info="The catalog product receiving this stock. Stock will increase for this product."
                        />
                        <EntityPicker
                          id={`purchase-item-product-${index}`}
                          value={item.productId}
                          enabled={Boolean(businessId)}
                          placeholder="Search products"
                          queryKey={["products", businessId]}
                          fetchPage={(search) => listProducts({ businessId: businessId!, search })}
                          fetchById={(id) => getProduct(businessId!, id)}
                          getId={(productOption: ProductRecord) => productOption.id}
                          getLabel={(productOption: ProductRecord) =>
                            productOption.sku
                              ? `${productOption.name} (${productOption.sku})`
                              : productOption.name
                          }
                          onChange={(productId, selected) => {
                            if (selected) {
                              setKnownProducts((current) => ({
                                ...current,
                                [selected.id]: selected,
                              }));
                            }
                            updateItem(index, {
                              ...item,
                              productId,
                              purchasePrice: selected
                                ? Number(selected.costPrice)
                                : item.purchasePrice,
                              gstRate: selected ? Number(selected.gstRate ?? 0) : item.gstRate,
                            });
                          }}
                        />
                      </div>

                      <div className="field">
                        <FieldLabel
                          htmlFor={`purchase-item-qty-${index}`}
                          label="Quantity"
                          info={`How many units you received${product ? ` (in ${product.unit})` : ""}. This amount is added to current stock.`}
                        />
                        <input
                          id={`purchase-item-qty-${index}`}
                          type="number"
                          min="0.001"
                          step="0.001"
                          inputMode="decimal"
                          value={item.quantity}
                          onChange={(event) =>
                            updateItem(index, {
                              ...item,
                              quantity: parseNumericDraft(event.target.value),
                            })
                          }
                          placeholder="e.g. 50"
                        />
                        {product ? (
                          <span className="field-hint">Unit: {product.unit}</span>
                        ) : null}
                      </div>

                      <div className="field">
                        <FieldLabel
                          htmlFor={`purchase-item-price-${index}`}
                          label="Price per unit"
                          info="What you paid the supplier for one unit. Line total = quantity × price per unit."
                        />
                        <input
                          id={`purchase-item-price-${index}`}
                          type="number"
                          min="0"
                          step="0.01"
                          inputMode="decimal"
                          value={item.purchasePrice}
                          onChange={(event) =>
                            updateItem(index, {
                              ...item,
                              purchasePrice: parseNumericDraft(event.target.value),
                            })
                          }
                          placeholder="e.g. 300"
                        />
                      </div>

                      <div className="field purchase-item-summary">
                        <span className="field-label-static">Line total</span>
                        <strong className="purchase-item-total">{formatMoney(lineTotal)}</strong>
                      </div>

                      <div className="purchase-item-actions">
                        <button
                          type="button"
                          className="ghost-button ghost-button--danger"
                          onClick={() => removeItem(index)}
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>

            <div className="purchase-summary">
              <div className="purchase-total">
                <strong>Total purchase amount</strong>
                <span>{formatMoney(purchaseTotal)}</span>
              </div>
              <div className="purchase-total purchase-total--soft">
                <strong>Paid now</strong>
                <span>{formatMoney(amountPaidNow)}</span>
              </div>
              <div className="purchase-total">
                <strong>Outstanding after save</strong>
                <span>{formatMoney(outstandingAfterSave)}</span>
              </div>
            </div>

            {feedback ? <p className="inline-note">{feedback}</p> : null}

            <button type="submit" className="primary-button" disabled={purchaseMutation.isPending}>
              {purchaseMutation.isPending ? "Recording purchase..." : "Record purchase"}
            </button>
          </form>
        </article>
      </section>

      <section className="panel-stack">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Purchase history</h3>
              <p>Review purchases, payment status, and cancellations.</p>
            </div>
          </div>

          <div className="field">
            <label htmlFor="purchase-search">Search purchases</label>
            <input
              id="purchase-search"
              value={purchaseSearch}
              onChange={(event) => setPurchaseSearch(event.target.value)}
              placeholder="Search by purchase number, supplier, or notes"
            />
          </div>

          <div className="product-list">
            {purchasesQuery.data?.items.map((purchase) => (
              <article
                key={purchase.id}
                className={`product-card${selectedPurchase?.id === purchase.id ? " product-card--selected" : ""}`}
              >
                <div className="product-card__row">
                  <div>
                    <h4>{purchase.purchaseNumber}</h4>
                    <p>
                      {purchase.supplierName} · {formatDate(purchase.purchaseDate)}
                    </p>
                  </div>
                  <span
                    className={`status-chip ${
                      purchase.cancelled ? "status-chip--warn" : "status-chip--success"
                    }`}
                  >
                    {purchase.cancelled ? "Cancelled" : purchase.paymentStatus}
                  </span>
                </div>

                <div className="product-metrics">
                  <span>Items: {purchase.items.length}</span>
                  <span>Total: {formatMoney(purchase.totalAmount)}</span>
                  {Number(purchase.returnedAmount) > 0 ? (
                    <span>Returned: {formatMoney(purchase.returnedAmount)}</span>
                  ) : null}
                  <span>Net: {formatMoney(purchase.netAmount)}</span>
                  <span>Paid: {formatMoney(purchase.amountPaid)}</span>
                  <span>Due: {formatMoney(purchase.outstandingAmount)}</span>
                </div>

                <div className="product-card__actions">
                  <button type="button" className="ghost-button" onClick={() => selectPurchase(purchase)}>
                    View details
                  </button>
                </div>
              </article>
            ))}
          </div>
          <PaginationBar page={purchasesQuery.data} onPageChange={setPurchasePage} />
        </article>

        {selectedPurchase ? (
          <article className="panel">
            <div className="panel-heading">
              <div>
                <h3>Purchase details</h3>
                <p>{selectedPurchase.purchaseNumber}</p>
              </div>
            </div>

            <div className="product-metrics">
              <span>Total: {formatMoney(selectedPurchase.totalAmount)}</span>
              <span>Returned: {formatMoney(selectedPurchase.returnedAmount)}</span>
              <span>Net: {formatMoney(selectedPurchase.netAmount)}</span>
              <span>Paid: {formatMoney(selectedPurchase.amountPaid)}</span>
              <span>Outstanding: {formatMoney(selectedPurchase.outstandingAmount)}</span>
              <span>Status: {selectedPurchase.paymentStatus}</span>
            </div>

            <div className="purchase-detail-list">
              {selectedPurchase.items.map((item) => (
                <div key={item.id} className="list-row">
                  <span>{item.productName}</span>
                  <span>
                    Bought {Number(item.quantity).toFixed(3)} · Returned{" "}
                    {Number(item.returnedQuantity).toFixed(3)} · Left{" "}
                    {Number(item.returnableQuantity).toFixed(3)} · {formatMoney(item.purchasePrice)}
                  </span>
                </div>
              ))}
            </div>

            <div className="product-card__actions">
              <button
                type="button"
                className="ghost-button"
                onClick={async () => {
                  try {
                    setFeedback(null);
                    await downloadPurchaseBill(businessId, selectedPurchase.id);
                    setFeedback("Purchase bill downloaded.");
                  } catch (error) {
                    handleApiError(error);
                  }
                }}
              >
                Download bill
              </button>
              <button
                type="button"
                className="primary-button"
                onClick={async () => {
                  try {
                    setFeedback(null);
                    await printPurchaseBill(businessId, selectedPurchase.id);
                    setFeedback("Purchase bill opened for printing.");
                  } catch (error) {
                    if (error instanceof Error && !(error instanceof ApiError)) {
                      setFeedback(error.message);
                      return;
                    }
                    handleApiError(error);
                  }
                }}
              >
                Print bill
              </button>
            </div>

            {!selectedPurchase.cancelled ? (
              <>
                <div className="field">
                  <label htmlFor="selected-purchase-date">Purchase date</label>
                  <input
                    id="selected-purchase-date"
                    type="date"
                    value={selectedPurchase.purchaseDate}
                    onChange={(event) =>
                      setSelectedPurchase((current) =>
                        current ? { ...current, purchaseDate: event.target.value } : current,
                      )
                    }
                  />
                </div>

                <div className="field">
                  <label htmlFor="selected-purchase-notes">Notes</label>
                  <input
                    id="selected-purchase-notes"
                    value={selectedPurchase.notes ?? ""}
                    onChange={(event) =>
                      setSelectedPurchase((current) =>
                        current ? { ...current, notes: event.target.value } : current,
                      )
                    }
                  />
                </div>

                <div className="product-card__actions">
                  <button
                    type="button"
                    className="ghost-button"
                    disabled={updatePurchaseMutation.isPending}
                    onClick={() => updatePurchaseMutation.mutate(selectedPurchase)}
                  >
                    {updatePurchaseMutation.isPending ? "Saving..." : "Save details"}
                  </button>
                </div>

                {Number(selectedPurchase.outstandingAmount) > 0 ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Record payment</h3>
                        <p>
                          Outstanding {formatMoney(selectedPurchase.outstandingAmount)}. Status
                          updates automatically from paid vs net amount.
                        </p>
                      </div>
                    </div>

                    <div className="split-grid">
                      <div className="field">
                        <label htmlFor="purchase-payment-date">Payment date</label>
                        <input
                          id="purchase-payment-date"
                          type="date"
                          value={paymentForm.paymentDate}
                          onChange={(event) =>
                            setPaymentForm((current) => ({
                              ...current,
                              paymentDate: event.target.value,
                            }))
                          }
                        />
                      </div>

                      <div className="field">
                        <FieldLabel
                          htmlFor="purchase-payment-amount"
                          label="Amount"
                          info="How much you are paying the supplier now. It cannot be more than the outstanding balance."
                        />
                        <input
                          id="purchase-payment-amount"
                          type="number"
                          min="0.01"
                          step="0.01"
                          inputMode="decimal"
                          value={paymentForm.amount}
                          onChange={(event) =>
                            setPaymentForm((current) => ({
                              ...current,
                              amount: parseNumericDraft(event.target.value),
                            }))
                          }
                          placeholder="0"
                        />
                      </div>
                    </div>

                    <div className="field">
                      <label htmlFor="purchase-payment-notes">Notes</label>
                      <input
                        id="purchase-payment-notes"
                        value={paymentForm.notes}
                        onChange={(event) =>
                          setPaymentForm((current) => ({
                            ...current,
                            notes: event.target.value,
                          }))
                        }
                        placeholder="Cash / UPI / bank transfer"
                      />
                    </div>

                    <button
                      type="button"
                      className="primary-button"
                      disabled={paymentMutation.isPending}
                      onClick={() => {
                        setFieldErrors({});
                        setFeedback(null);
                        paymentMutation.mutate();
                      }}
                    >
                      {paymentMutation.isPending ? "Recording payment..." : "Record payment"}
                    </button>
                  </div>
                ) : (
                  <div className="empty-inline-state">
                    <strong>Fully paid</strong>
                    <p>No outstanding balance on this purchase.</p>
                  </div>
                )}

                {purchasePaymentsQuery.data && purchasePaymentsQuery.data.length > 0 ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Payment history</h3>
                        <p>Payments recorded against {selectedPurchase.purchaseNumber}.</p>
                      </div>
                    </div>
                    {purchasePaymentsQuery.data.map((payment) => (
                      <div key={payment.id} className="list-row">
                        <span>
                          {payment.paymentDate}
                          {payment.notes ? ` · ${payment.notes}` : ""}
                        </span>
                        <span>{formatMoney(payment.amount)}</span>
                      </div>
                    ))}
                  </div>
                ) : null}

                {selectedPurchase.items.some((item) => Number(item.returnableQuantity) > 0) ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Record purchase return</h3>
                        <p>
                          Return stock to the supplier. Stock is reduced for the returned quantity.
                        </p>
                      </div>
                    </div>

                    <div className="split-grid">
                      <div className="field">
                        <label htmlFor="purchase-return-date">Return date</label>
                        <input
                          id="purchase-return-date"
                          type="date"
                          value={returnForm.returnDate}
                          onChange={(event) =>
                            setReturnForm((current) => ({
                              ...current,
                              returnDate: event.target.value,
                            }))
                          }
                        />
                      </div>

                      <div className="field">
                        <label htmlFor="purchase-return-reason">Reason</label>
                        <input
                          id="purchase-return-reason"
                          value={returnForm.reason}
                          onChange={(event) =>
                            setReturnForm((current) => ({
                              ...current,
                              reason: event.target.value,
                            }))
                          }
                          placeholder="Damaged bags"
                        />
                      </div>
                    </div>

                    <div className="field">
                      <label htmlFor="purchase-return-notes">Notes</label>
                      <input
                        id="purchase-return-notes"
                        value={returnForm.notes}
                        onChange={(event) =>
                          setReturnForm((current) => ({
                            ...current,
                            notes: event.target.value,
                          }))
                        }
                        placeholder="Sent unused stock back to supplier"
                      />
                    </div>

                    {selectedPurchase.items
                      .filter((item) => Number(item.returnableQuantity) > 0)
                      .map((item) => {
                        const quantity =
                          returnForm.items.find(
                            (candidate) => candidate.purchaseItemId === item.id,
                          )?.quantity ?? "";
                        const returnQty = resolveNumericDraft(quantity);

                        return (
                          <div key={item.id} className="purchase-item-card">
                            <div className="purchase-item-row purchase-item-row--compose">
                              <div className="field">
                                <div className="field-label-row">
                                  <span className="field-label-static">{item.productName}</span>
                                  <FieldInfo label={item.productName}>
                                    Return up to {Number(item.returnableQuantity).toFixed(3)} {item.unit}.
                                    Leave blank if this line is not being returned.
                                  </FieldInfo>
                                </div>
                                <span className="field-hint">
                                  Returnable: {Number(item.returnableQuantity).toFixed(3)} {item.unit}
                                </span>
                              </div>
                              <div className="field">
                                <label htmlFor={`purchase-return-qty-${item.id}`}>Quantity</label>
                                <input
                                  id={`purchase-return-qty-${item.id}`}
                                  type="number"
                                  min="0"
                                  max={Number(item.returnableQuantity)}
                                  step="0.001"
                                  inputMode="decimal"
                                  value={quantity}
                                  onChange={(event) =>
                                    updateReturnQuantity(
                                      item.id,
                                      parseNumericDraft(event.target.value),
                                    )
                                  }
                                  placeholder="0"
                                />
                              </div>
                              <div className="field purchase-item-summary">
                                <span className="field-label-static">Line total</span>
                                <strong className="purchase-item-total">
                                  {formatMoney(returnQty * Number(item.purchasePrice))}
                                </strong>
                              </div>
                            </div>
                          </div>
                        );
                      })}

                    <div className="purchase-total">
                      <strong>Return amount</strong>
                      <span>{formatMoney(returnTotal)}</span>
                    </div>

                    <button
                      type="button"
                      className="primary-button"
                      disabled={returnPurchaseMutation.isPending}
                      onClick={() => {
                        setFieldErrors({});
                        setFeedback(null);
                        returnPurchaseMutation.mutate();
                      }}
                    >
                      {returnPurchaseMutation.isPending
                        ? "Recording return..."
                        : "Record purchase return"}
                    </button>
                  </div>
                ) : (
                  <div className="empty-inline-state">
                    <strong>Nothing left to return</strong>
                    <p>All purchased quantities on this bill have already been returned.</p>
                  </div>
                )}

                {purchaseReturnsQuery.data && purchaseReturnsQuery.data.length > 0 ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Returns on this purchase</h3>
                        <p>Earlier purchase returns linked to {selectedPurchase.purchaseNumber}.</p>
                      </div>
                    </div>
                    {purchaseReturnsQuery.data.map((purchaseReturn) => (
                      <div key={purchaseReturn.id} className="empty-inline-state">
                        <strong>
                          {purchaseReturn.returnNumber} · {formatMoney(purchaseReturn.totalAmount)}
                        </strong>
                        <p>
                          {purchaseReturn.returnDate}
                          {purchaseReturn.reason ? ` · ${purchaseReturn.reason}` : ""}
                        </p>
                        {purchaseReturn.items.map((item) => (
                          <p key={item.id}>
                            {item.productName}: {Number(item.quantity).toFixed(3)} {item.unit}
                          </p>
                        ))}
                      </div>
                    ))}
                  </div>
                ) : null}

                <div className="field">
                  <label htmlFor="purchase-cancel-reason">Cancellation reason</label>
                  <input
                    id="purchase-cancel-reason"
                    value={cancellationReason}
                    onChange={(event) => setCancellationReason(event.target.value)}
                    placeholder="Duplicate bill from supplier"
                    disabled={selectedPurchase.hasReturns}
                  />
                </div>

                {selectedPurchase.hasReturns ? (
                  <p className="inline-note">
                    This purchase has returns, so it cannot be cancelled. Record another purchase
                    return instead if needed.
                  </p>
                ) : null}

                <button
                  type="button"
                  className="ghost-button ghost-button--danger"
                  disabled={cancelPurchaseMutation.isPending || selectedPurchase.hasReturns}
                  onClick={() => cancelPurchaseMutation.mutate()}
                >
                  {cancelPurchaseMutation.isPending ? "Cancelling..." : "Cancel purchase"}
                </button>
              </>
            ) : (
              <div className="empty-inline-state">
                <strong>Purchase cancelled</strong>
                <p>{selectedPurchase.cancellationReason ?? "No cancellation reason recorded."}</p>
              </div>
            )}
          </article>
        ) : null}
      </section>
    </>
  );
}
