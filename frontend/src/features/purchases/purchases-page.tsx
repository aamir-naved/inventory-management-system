import {
  useDeferredValue,
  useMemo,
  useState,
  type FormEvent,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import { useBusinessSettings } from "@/features/settings/use-business-settings";
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
  listPurchaseReturns,
  listPurchases,
  updatePurchase,
  type PurchaseItemPayload,
  type PurchasePayload,
  type PurchaseRecord,
  type PurchaseReturnPayload,
} from "@/features/purchases/purchase-api";
import {
  createSupplier,
  listSuppliers,
  type SupplierPayload,
} from "@/features/suppliers/supplier-api";
import { listProducts, type ProductRecord } from "@/features/products/product-api";

const initialSupplier: SupplierPayload = {
  name: "",
  contactPerson: "",
  mobileNumber: "",
  addressLine: "",
};

const initialPurchase: PurchasePayload = {
  supplierId: "",
  purchaseDate: new Date().toISOString().slice(0, 10),
  amountPaid: 0,
  notes: "",
  items: [],
};

const initialPaymentForm: PaymentPayload = {
  paymentDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  notes: "",
};

function emptyItem(productId = ""): PurchaseItemPayload {
  return {
    productId,
    quantity: 1,
    purchasePrice: 0,
  };
}

function summarizeTotal(items: PurchaseItemPayload[]) {
  return items.reduce((total, item) => total + item.quantity * item.purchasePrice, 0);
}

export function PurchasesPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate } = useBusinessSettings();
  const [supplierForm, setSupplierForm] = useState<SupplierPayload>(initialSupplier);
  const [purchaseForm, setPurchaseForm] = useState<PurchasePayload>(initialPurchase);
  const [purchaseSearch, setPurchaseSearch] = useState("");
  const deferredPurchaseSearch = useDeferredValue(purchaseSearch);
  const [selectedPurchase, setSelectedPurchase] = useState<PurchaseRecord | null>(null);
  const [cancellationReason, setCancellationReason] = useState("");
  const [returnForm, setReturnForm] = useState<PurchaseReturnPayload>({
    returnDate: new Date().toISOString().slice(0, 10),
    reason: "",
    notes: "",
    items: [],
  });
  const [paymentForm, setPaymentForm] = useState<PaymentPayload>(initialPaymentForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const productsQuery = useQuery({
    queryKey: ["products", businessId, "", false],
    queryFn: () => listProducts({ businessId: businessId! }),
    enabled: Boolean(businessId),
  });

  const suppliersQuery = useQuery({
    queryKey: ["suppliers", businessId],
    queryFn: () => listSuppliers(businessId!),
    enabled: Boolean(businessId),
  });

  const purchasesQuery = useQuery({
    queryKey: ["purchases", businessId, deferredPurchaseSearch],
    queryFn: () => listPurchases(businessId!, deferredPurchaseSearch),
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
      setFeedback("Purchase recorded and stock increased.");
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

      return createPurchasePayment(businessId, selectedPurchase.id, paymentForm);
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

      const purchases = await queryClient.fetchQuery({
        queryKey: ["purchases", businessId, deferredPurchaseSearch],
        queryFn: () => listPurchases(businessId!, deferredPurchaseSearch),
      });
      const refreshed = purchases.find((purchase) => purchase.id === selectedPurchase?.id);
      if (refreshed) {
        setSelectedPurchase(refreshed);
      }
    },
    onError: handleApiError,
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

      const items = returnForm.items.filter((item) => item.quantity > 0);
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

      const purchases = await queryClient.fetchQuery({
        queryKey: ["purchases", businessId, deferredPurchaseSearch],
        queryFn: () => listPurchases(businessId!, deferredPurchaseSearch),
      });
      const refreshed = purchases.find((purchase) => purchase.id === purchaseReturn.purchaseId);
      if (refreshed) {
        selectPurchase(refreshed);
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

  const purchaseTotal = useMemo(() => summarizeTotal(purchaseForm.items), [purchaseForm.items]);

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
      return total + item.quantity * Number(purchaseItem.purchasePrice);
    }, 0);
  }, [returnForm.items, selectedPurchase]);

  function selectPurchase(purchase: PurchaseRecord) {
    setSelectedPurchase(purchase);
    setCancellationReason("");
    setPaymentForm({
      ...initialPaymentForm,
      paymentDate: new Date().toISOString().slice(0, 10),
      amount: Number(purchase.outstandingAmount) > 0 ? Number(purchase.outstandingAmount) : 0,
    });
    setReturnForm({
      returnDate: new Date().toISOString().slice(0, 10),
      reason: "",
      notes: "",
      items: purchase.items
        .filter((item) => Number(item.returnableQuantity) > 0)
        .map((item) => ({
          purchaseItemId: item.id,
          quantity: 0,
        })),
    });
    setFeedback(null);
    setFieldErrors({});
  }

  function updateReturnQuantity(purchaseItemId: string, quantity: number) {
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
    const firstProductId = productsQuery.data?.[0]?.id ?? "";
    setPurchaseForm((current) => ({
      ...current,
      items: [...current.items, emptyItem(firstProductId)],
    }));
  }

  function updateItem(index: number, nextItem: PurchaseItemPayload) {
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
    await purchaseMutation.mutateAsync(purchaseForm);
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before recording purchases.</h1>
        <p>Purchases are tenant-scoped and need the business profile first.</p>
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

          <div className="supplier-list">
            {suppliersQuery.data?.map((supplier) => (
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
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Create purchase</h3>
              <p>Every saved purchase increases stock and records inventory movements.</p>
            </div>
          </div>

          <form className="form-stack" onSubmit={handleCreatePurchase}>
            <div className="split-grid">
              <div className="field">
                <label htmlFor="purchase-supplier">Supplier</label>
                <select
                  id="purchase-supplier"
                  value={purchaseForm.supplierId}
                  onChange={(event) =>
                    setPurchaseForm((current) => ({ ...current, supplierId: event.target.value }))
                  }
                >
                  <option value="">Select supplier</option>
                  {suppliersQuery.data?.map((supplier) => (
                    <option key={supplier.id} value={supplier.id}>
                      {supplier.name}
                    </option>
                  ))}
                </select>
                {fieldErrors.supplierId ? <span className="field-error">{fieldErrors.supplierId}</span> : null}
              </div>

              <div className="field">
                <label htmlFor="purchase-date">Purchase date</label>
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

            <div className="split-grid">
              <div className="field">
                <label htmlFor="purchase-amount-paid">Amount paid now</label>
                <input
                  id="purchase-amount-paid"
                  type="number"
                  min="0"
                  step="0.01"
                  value={purchaseForm.amountPaid}
                  onChange={(event) =>
                    setPurchaseForm((current) => ({
                      ...current,
                      amountPaid: Number(event.target.value),
                    }))
                  }
                />
                {fieldErrors.amountPaid ? (
                  <span className="field-error">{fieldErrors.amountPaid}</span>
                ) : null}
              </div>

              <div className="field">
                <label htmlFor="purchase-notes">Notes</label>
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
                  <h3>Purchase items</h3>
                  <p>Add one or more products with quantity and purchase price.</p>
                </div>
                <button type="button" className="ghost-button" onClick={addItem}>
                  Add item
                </button>
              </div>

              {purchaseForm.items.map((item, index) => {
                const product = productsQuery.data?.find((candidate) => candidate.id === item.productId);

                return (
                  <div key={`${item.productId}-${index}`} className="purchase-item-row">
                    <select
                      value={item.productId}
                      onChange={(event) =>
                        updateItem(index, {
                          ...item,
                          productId: event.target.value,
                          purchasePrice:
                            productsQuery.data?.find((candidate) => candidate.id === event.target.value)?.costPrice ?? item.purchasePrice,
                        })
                      }
                    >
                      <option value="">Select product</option>
                      {productsQuery.data?.map((productOption: ProductRecord) => (
                        <option key={productOption.id} value={productOption.id}>
                          {productOption.name}
                        </option>
                      ))}
                    </select>

                    <input
                      type="number"
                      min="0.001"
                      step="0.001"
                      value={item.quantity}
                      onChange={(event) =>
                        updateItem(index, { ...item, quantity: Number(event.target.value) })
                      }
                      placeholder="Qty"
                    />

                    <input
                      type="number"
                      min="0"
                      step="0.01"
                      value={item.purchasePrice}
                      onChange={(event) =>
                        updateItem(index, { ...item, purchasePrice: Number(event.target.value) })
                      }
                      placeholder="Price"
                    />

                    <span className="purchase-item-total">
                      {formatMoney(item.quantity * item.purchasePrice)}
                      {product ? ` · ${product.unit}` : ""}
                    </span>

                    <button type="button" className="ghost-button ghost-button--danger" onClick={() => removeItem(index)}>
                      Remove
                    </button>
                  </div>
                );
              })}
            </div>

            <div className="purchase-total">
              <strong>Total purchase amount</strong>
              <span>{formatMoney(purchaseTotal)}</span>
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
            {purchasesQuery.data?.map((purchase) => (
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
                        <label htmlFor="purchase-payment-amount">Amount</label>
                        <input
                          id="purchase-payment-amount"
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={paymentForm.amount}
                          onChange={(event) =>
                            setPaymentForm((current) => ({
                              ...current,
                              amount: Number(event.target.value),
                            }))
                          }
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
                          )?.quantity ?? 0;

                        return (
                          <div key={item.id} className="purchase-item-row">
                            <span>
                              {item.productName} · up to {Number(item.returnableQuantity).toFixed(3)}{" "}
                              {item.unit}
                            </span>
                            <input
                              type="number"
                              min="0"
                              max={Number(item.returnableQuantity)}
                              step="0.001"
                              value={quantity}
                              onChange={(event) =>
                                updateReturnQuantity(item.id, Number(event.target.value))
                              }
                              placeholder="Return qty"
                            />
                            <span className="purchase-item-total">
                              {formatMoney(quantity * Number(item.purchasePrice))}
                            </span>
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
