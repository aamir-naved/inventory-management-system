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
  createCustomer,
  listCustomers,
  type CustomerPayload,
} from "@/features/customers/customer-api";
import { listProducts, type ProductRecord } from "@/features/products/product-api";
import {
  createSalePayment,
  listSalePayments,
  type PaymentPayload,
} from "@/features/payments/payment-api";
import {
  downloadSaleInvoice,
  printSaleInvoice,
} from "@/features/documents/document-api";
import {
  cancelSale,
  createSale,
  createSaleReturn,
  listSaleReturns,
  listSales,
  updateSale,
  type SaleItemPayload,
  type SalePayload,
  type SaleRecord,
  type SaleReturnPayload,
} from "@/features/sales/sales-api";

const initialCustomer: CustomerPayload = {
  name: "",
  contactPerson: "",
  mobileNumber: "",
  addressLine: "",
};

const initialSale: SalePayload = {
  customerId: "",
  saleDate: new Date().toISOString().slice(0, 10),
  amountPaid: 0,
  notes: "",
  items: [],
};

const initialPaymentForm: PaymentPayload = {
  paymentDate: new Date().toISOString().slice(0, 10),
  amount: 0,
  notes: "",
};

function emptyItem(productId = "", sellingPrice = 0): SaleItemPayload {
  return {
    productId,
    quantity: 1,
    sellingPrice,
  };
}

function summarizeTotal(items: SaleItemPayload[]) {
  return items.reduce((total, item) => total + item.quantity * item.sellingPrice, 0);
}

export function SalesPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate } = useBusinessSettings();
  const [customerForm, setCustomerForm] = useState<CustomerPayload>(initialCustomer);
  const [saleForm, setSaleForm] = useState<SalePayload>(initialSale);
  const [saleSearch, setSaleSearch] = useState("");
  const deferredSaleSearch = useDeferredValue(saleSearch);
  const [selectedSale, setSelectedSale] = useState<SaleRecord | null>(null);
  const [cancellationReason, setCancellationReason] = useState("");
  const [returnForm, setReturnForm] = useState<SaleReturnPayload>({
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

  const customersQuery = useQuery({
    queryKey: ["customers", businessId],
    queryFn: () => listCustomers(businessId!),
    enabled: Boolean(businessId),
  });

  const salesQuery = useQuery({
    queryKey: ["sales", businessId, deferredSaleSearch],
    queryFn: () => listSales(businessId!, deferredSaleSearch),
    enabled: Boolean(businessId),
  });

  const saleReturnsQuery = useQuery({
    queryKey: ["sale-returns", businessId, selectedSale?.id],
    queryFn: () => listSaleReturns(businessId!, selectedSale!.id),
    enabled: Boolean(businessId && selectedSale?.id),
  });

  const salePaymentsQuery = useQuery({
    queryKey: ["sale-payments", businessId, selectedSale?.id],
    queryFn: () => listSalePayments(businessId!, selectedSale!.id),
    enabled: Boolean(businessId && selectedSale?.id),
  });

  const customerMutation = useMutation({
    mutationFn: async (payload: CustomerPayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before sales can be managed.");
      }

      return createCustomer(businessId, payload);
    },
    onSuccess: async (customer) => {
      setFeedback("Customer created.");
      setCustomerForm(initialCustomer);
      setSaleForm((current) => ({ ...current, customerId: customer.id }));
      await queryClient.invalidateQueries({ queryKey: ["customers", businessId] });
    },
    onError: handleApiError,
  });

  const saleMutation = useMutation({
    mutationFn: async (payload: SalePayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before sales can be managed.");
      }

      return createSale(businessId, payload);
    },
    onSuccess: async (sale) => {
      setFeedback("Sale recorded and stock reduced.");
      setSelectedSale(sale);
      setSaleForm({
        ...initialSale,
        saleDate: new Date().toISOString().slice(0, 10),
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["sales", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["products", businessId] }),
      ]);
    },
    onError: handleApiError,
  });

  const updateSaleMutation = useMutation({
    mutationFn: async (sale: SaleRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before sales can be managed.");
      }

      return updateSale(businessId, sale.id, {
        saleDate: sale.saleDate,
        notes: sale.notes ?? "",
      });
    },
    onSuccess: async (sale) => {
      setFeedback("Sale details updated.");
      setSelectedSale(sale);
      await queryClient.invalidateQueries({ queryKey: ["sales", businessId] });
    },
    onError: handleApiError,
  });

  const paymentMutation = useMutation({
    mutationFn: async () => {
      if (!businessId || !selectedSale) {
        throw new Error("Select a sale before recording a payment.");
      }

      return createSalePayment(businessId, selectedSale.id, paymentForm);
    },
    onSuccess: async () => {
      setFeedback("Payment recorded.");
      setPaymentForm({
        ...initialPaymentForm,
        paymentDate: new Date().toISOString().slice(0, 10),
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["sales", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["sale-payments", businessId] }),
      ]);

      const sales = await queryClient.fetchQuery({
        queryKey: ["sales", businessId, deferredSaleSearch],
        queryFn: () => listSales(businessId!, deferredSaleSearch),
      });
      const refreshed = sales.find((sale) => sale.id === selectedSale?.id);
      if (refreshed) {
        setSelectedSale(refreshed);
      }
    },
    onError: handleApiError,
  });

  const cancelSaleMutation = useMutation({
    mutationFn: async () => {
      if (!businessId || !selectedSale) {
        throw new Error("Select a sale before cancelling.");
      }

      return cancelSale(businessId, selectedSale.id, cancellationReason);
    },
    onSuccess: async (sale) => {
      setFeedback("Sale cancelled and stock restored.");
      setSelectedSale(sale);
      setCancellationReason("");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["sales", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["products", businessId] }),
      ]);
    },
    onError: handleApiError,
  });

  const returnSaleMutation = useMutation({
    mutationFn: async () => {
      if (!businessId || !selectedSale) {
        throw new Error("Select a sale before recording a return.");
      }

      const items = returnForm.items.filter((item) => item.quantity > 0);
      if (items.length === 0) {
        throw new Error("Add at least one return quantity.");
      }

      return createSaleReturn(businessId, selectedSale.id, {
        ...returnForm,
        items,
      });
    },
    onSuccess: async (saleReturn) => {
      setFeedback("Sale return recorded and stock restored.");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["sales", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["sale-returns", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["sale-payments", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-summary", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-stock", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["inventory-movements", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["products", businessId] }),
      ]);

      const sales = await queryClient.fetchQuery({
        queryKey: ["sales", businessId, deferredSaleSearch],
        queryFn: () => listSales(businessId!, deferredSaleSearch),
      });
      const refreshed = sales.find((sale) => sale.id === saleReturn.saleId);
      if (refreshed) {
        selectSale(refreshed);
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

  const saleTotal = useMemo(() => summarizeTotal(saleForm.items), [saleForm.items]);

  const returnTotal = useMemo(() => {
    if (!selectedSale) {
      return 0;
    }

    return returnForm.items.reduce((total, item) => {
      const saleItem = selectedSale.items.find((candidate) => candidate.id === item.saleItemId);
      if (!saleItem) {
        return total;
      }
      return total + item.quantity * Number(saleItem.sellingPrice);
    }, 0);
  }, [returnForm.items, selectedSale]);

  function selectSale(sale: SaleRecord) {
    setSelectedSale(sale);
    setCancellationReason("");
    setPaymentForm({
      ...initialPaymentForm,
      paymentDate: new Date().toISOString().slice(0, 10),
      amount: Number(sale.outstandingAmount) > 0 ? Number(sale.outstandingAmount) : 0,
    });
    setReturnForm({
      returnDate: new Date().toISOString().slice(0, 10),
      reason: "",
      notes: "",
      items: sale.items
        .filter((item) => Number(item.returnableQuantity) > 0)
        .map((item) => ({
          saleItemId: item.id,
          quantity: 0,
        })),
    });
    setFeedback(null);
    setFieldErrors({});
  }

  function updateReturnQuantity(saleItemId: string, quantity: number) {
    setReturnForm((current) => {
      const existing = current.items.find((item) => item.saleItemId === saleItemId);
      if (!existing) {
        return {
          ...current,
          items: [...current.items, { saleItemId, quantity }],
        };
      }

      return {
        ...current,
        items: current.items.map((item) =>
          item.saleItemId === saleItemId ? { ...item, quantity } : item,
        ),
      };
    });
  }

  function addItem() {
    const firstProduct = productsQuery.data?.[0];
    setSaleForm((current) => ({
      ...current,
      items: [...current.items, emptyItem(firstProduct?.id ?? "", firstProduct?.sellingPrice ?? 0)],
    }));
  }

  function updateItem(index: number, nextItem: SaleItemPayload) {
    setSaleForm((current) => ({
      ...current,
      items: current.items.map((item, itemIndex) => (itemIndex === index ? nextItem : item)),
    }));
  }

  function removeItem(index: number) {
    setSaleForm((current) => ({
      ...current,
      items: current.items.filter((_, itemIndex) => itemIndex !== index),
    }));
  }

  async function handleCreateCustomer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFeedback(null);
    await customerMutation.mutateAsync(customerForm);
  }

  async function handleCreateSale(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFieldErrors({});
    setFeedback(null);
    await saleMutation.mutateAsync(saleForm);
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before recording sales.</h1>
        <p>Sales are tenant-scoped and need the business profile first.</p>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Sales management</span>
        <h1>Record stock outflow, manage customer billing, and reverse mistakes safely.</h1>
        <p>
          This workspace handles customer setup, sale entry, stock reduction,
          history, payment status, sale returns, and sale cancellation.
        </p>
      </section>

      <section className="workspace-grid purchases-layout">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Quick customer setup</h3>
              <p>Create a customer here so the sales form can select them immediately.</p>
            </div>
          </div>

          <form className="form-stack" onSubmit={handleCreateCustomer}>
            <div className="field">
              <label htmlFor="customer-name">Customer name</label>
              <input
                id="customer-name"
                value={customerForm.name}
                onChange={(event) => setCustomerForm((current) => ({ ...current, name: event.target.value }))}
                placeholder="Apex Builders"
              />
              {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
            </div>

            <div className="split-grid">
              <div className="field">
                <label htmlFor="customer-contact">Contact person</label>
                <input
                  id="customer-contact"
                  value={customerForm.contactPerson}
                  onChange={(event) =>
                    setCustomerForm((current) => ({ ...current, contactPerson: event.target.value }))
                  }
                  placeholder="Neha Shah"
                />
              </div>

              <div className="field">
                <label htmlFor="customer-mobile">Mobile number</label>
                <input
                  id="customer-mobile"
                  value={customerForm.mobileNumber}
                  onChange={(event) =>
                    setCustomerForm((current) => ({ ...current, mobileNumber: event.target.value }))
                  }
                  placeholder="+91 9998887776"
                />
                {fieldErrors.mobileNumber ? (
                  <span className="field-error">{fieldErrors.mobileNumber}</span>
                ) : null}
              </div>
            </div>

            <div className="field">
              <label htmlFor="customer-address">Address</label>
              <input
                id="customer-address"
                value={customerForm.addressLine}
                onChange={(event) =>
                  setCustomerForm((current) => ({ ...current, addressLine: event.target.value }))
                }
                placeholder="Ring Road"
              />
            </div>

            <button type="submit" className="ghost-button" disabled={customerMutation.isPending}>
              {customerMutation.isPending ? "Creating customer..." : "Create customer"}
            </button>
          </form>

          <div className="supplier-list">
            {customersQuery.data?.map((customer) => (
              <button
                key={customer.id}
                type="button"
                className={`supplier-pill${saleForm.customerId === customer.id ? " supplier-pill--selected" : ""}`}
                onClick={() =>
                  setSaleForm((current) => ({
                    ...current,
                    customerId: customer.id,
                  }))
                }
              >
                {customer.name}
              </button>
            ))}
          </div>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Create sale</h3>
              <p>Every saved sale reduces stock and records inventory movements.</p>
            </div>
          </div>

          <form className="form-stack" onSubmit={handleCreateSale}>
            <div className="split-grid">
              <div className="field">
                <label htmlFor="sale-customer">Customer</label>
                <select
                  id="sale-customer"
                  value={saleForm.customerId}
                  onChange={(event) =>
                    setSaleForm((current) => ({ ...current, customerId: event.target.value }))
                  }
                >
                  <option value="">Select customer</option>
                  {customersQuery.data?.map((customer) => (
                    <option key={customer.id} value={customer.id}>
                      {customer.name}
                    </option>
                  ))}
                </select>
                {fieldErrors.customerId ? <span className="field-error">{fieldErrors.customerId}</span> : null}
              </div>

              <div className="field">
                <label htmlFor="sale-date">Sale date</label>
                <input
                  id="sale-date"
                  type="date"
                  value={saleForm.saleDate}
                  onChange={(event) =>
                    setSaleForm((current) => ({ ...current, saleDate: event.target.value }))
                  }
                />
              </div>
            </div>

            <div className="split-grid">
              <div className="field">
                <label htmlFor="sale-amount-paid">Amount paid now</label>
                <input
                  id="sale-amount-paid"
                  type="number"
                  min="0"
                  step="0.01"
                  value={saleForm.amountPaid}
                  onChange={(event) =>
                    setSaleForm((current) => ({
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
                <label htmlFor="sale-notes">Notes</label>
                <input
                  id="sale-notes"
                  value={saleForm.notes}
                  onChange={(event) =>
                    setSaleForm((current) => ({ ...current, notes: event.target.value }))
                  }
                  placeholder="Counter sale"
                />
              </div>
            </div>

            <div className="purchase-items">
              <div className="panel-heading">
                <div>
                  <h3>Sale items</h3>
                  <p>Add one or more products with quantity and selling price.</p>
                </div>
                <button type="button" className="ghost-button" onClick={addItem}>
                  Add item
                </button>
              </div>

              {saleForm.items.map((item, index) => {
                const product = productsQuery.data?.find((candidate) => candidate.id === item.productId);

                return (
                  <div key={`${item.productId}-${index}`} className="purchase-item-row">
                    <select
                      value={item.productId}
                      onChange={(event) => {
                        const nextProduct = productsQuery.data?.find(
                          (candidate) => candidate.id === event.target.value,
                        );
                        updateItem(index, {
                          ...item,
                          productId: event.target.value,
                          sellingPrice: nextProduct?.sellingPrice ?? item.sellingPrice,
                        });
                      }}
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
                      value={item.sellingPrice}
                      onChange={(event) =>
                        updateItem(index, { ...item, sellingPrice: Number(event.target.value) })
                      }
                      placeholder="Price"
                    />

                    <span className="purchase-item-total">
                      {formatMoney(item.quantity * item.sellingPrice)}
                      {product ? ` · ${product.unit} · Stock ${Number(product.currentStock).toFixed(3)}` : ""}
                    </span>

                    <button type="button" className="ghost-button ghost-button--danger" onClick={() => removeItem(index)}>
                      Remove
                    </button>
                  </div>
                );
              })}
            </div>

            <div className="purchase-total">
              <strong>Total sale amount</strong>
              <span>{formatMoney(saleTotal)}</span>
            </div>

            {feedback ? <p className="inline-note">{feedback}</p> : null}

            <button type="submit" className="primary-button" disabled={saleMutation.isPending}>
              {saleMutation.isPending ? "Recording sale..." : "Record sale"}
            </button>
          </form>
        </article>
      </section>

      <section className="panel-stack">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Sales history</h3>
              <p>Review sales, returns, payment status, and cancellations.</p>
            </div>
          </div>

          <div className="field">
            <label htmlFor="sale-search">Search sales</label>
            <input
              id="sale-search"
              value={saleSearch}
              onChange={(event) => setSaleSearch(event.target.value)}
              placeholder="Search by sale number, customer, or notes"
            />
          </div>

          <div className="product-list">
            {salesQuery.data?.map((sale) => (
              <article
                key={sale.id}
                className={`product-card${selectedSale?.id === sale.id ? " product-card--selected" : ""}`}
              >
                <div className="product-card__row">
                  <div>
                    <h4>{sale.saleNumber}</h4>
                    <p>
                      {sale.customerName} · {formatDate(sale.saleDate)}
                    </p>
                  </div>
                  <span
                    className={`status-chip ${
                      sale.cancelled ? "status-chip--warn" : "status-chip--success"
                    }`}
                  >
                    {sale.cancelled ? "Cancelled" : sale.paymentStatus}
                  </span>
                </div>

                <div className="product-metrics">
                  <span>Items: {sale.items.length}</span>
                  <span>Total: {formatMoney(sale.totalAmount)}</span>
                  {Number(sale.returnedAmount) > 0 ? (
                    <span>Returned: {formatMoney(sale.returnedAmount)}</span>
                  ) : null}
                  <span>Net: {formatMoney(sale.netAmount)}</span>
                  <span>Paid: {formatMoney(sale.amountPaid)}</span>
                  <span>Due: {formatMoney(sale.outstandingAmount)}</span>
                </div>

                <div className="product-card__actions">
                  <button type="button" className="ghost-button" onClick={() => selectSale(sale)}>
                    View details
                  </button>
                </div>
              </article>
            ))}
          </div>
        </article>

        {selectedSale ? (
          <article className="panel">
            <div className="panel-heading">
              <div>
                <h3>Sale details</h3>
                <p>{selectedSale.saleNumber}</p>
              </div>
            </div>

            <div className="product-metrics">
              <span>Total: {formatMoney(selectedSale.totalAmount)}</span>
              <span>Returned: {formatMoney(selectedSale.returnedAmount)}</span>
              <span>Net: {formatMoney(selectedSale.netAmount)}</span>
              <span>Paid: {formatMoney(selectedSale.amountPaid)}</span>
              <span>Outstanding: {formatMoney(selectedSale.outstandingAmount)}</span>
              <span>Status: {selectedSale.paymentStatus}</span>
            </div>

            <div className="purchase-detail-list">
              {selectedSale.items.map((item) => (
                <div key={item.id} className="list-row">
                  <span>{item.productName}</span>
                  <span>
                    Sold {Number(item.quantity).toFixed(3)} · Returned{" "}
                    {Number(item.returnedQuantity).toFixed(3)} · Left{" "}
                    {Number(item.returnableQuantity).toFixed(3)} · {formatMoney(item.sellingPrice)}
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
                    await downloadSaleInvoice(businessId, selectedSale.id);
                    setFeedback("Invoice downloaded.");
                  } catch (error) {
                    handleApiError(error);
                  }
                }}
              >
                Download invoice
              </button>
              <button
                type="button"
                className="primary-button"
                onClick={async () => {
                  try {
                    setFeedback(null);
                    await printSaleInvoice(businessId, selectedSale.id);
                    setFeedback("Invoice opened for printing.");
                  } catch (error) {
                    if (error instanceof Error && !(error instanceof ApiError)) {
                      setFeedback(error.message);
                      return;
                    }
                    handleApiError(error);
                  }
                }}
              >
                Print invoice
              </button>
            </div>

            {!selectedSale.cancelled ? (
              <>
                <div className="field">
                  <label htmlFor="selected-sale-date">Sale date</label>
                  <input
                    id="selected-sale-date"
                    type="date"
                    value={selectedSale.saleDate}
                    onChange={(event) =>
                      setSelectedSale((current) =>
                        current ? { ...current, saleDate: event.target.value } : current,
                      )
                    }
                  />
                </div>

                <div className="field">
                  <label htmlFor="selected-sale-notes">Notes</label>
                  <input
                    id="selected-sale-notes"
                    value={selectedSale.notes ?? ""}
                    onChange={(event) =>
                      setSelectedSale((current) =>
                        current ? { ...current, notes: event.target.value } : current,
                      )
                    }
                  />
                </div>

                <div className="product-card__actions">
                  <button
                    type="button"
                    className="ghost-button"
                    disabled={updateSaleMutation.isPending}
                    onClick={() => updateSaleMutation.mutate(selectedSale)}
                  >
                    {updateSaleMutation.isPending ? "Saving..." : "Save details"}
                  </button>
                </div>

                {Number(selectedSale.outstandingAmount) > 0 ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Record payment</h3>
                        <p>
                          Outstanding {formatMoney(selectedSale.outstandingAmount)}. Status
                          updates automatically from paid vs net amount.
                        </p>
                      </div>
                    </div>

                    <div className="split-grid">
                      <div className="field">
                        <label htmlFor="sale-payment-date">Payment date</label>
                        <input
                          id="sale-payment-date"
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
                        <label htmlFor="sale-payment-amount">Amount</label>
                        <input
                          id="sale-payment-amount"
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
                      <label htmlFor="sale-payment-notes">Notes</label>
                      <input
                        id="sale-payment-notes"
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
                    <p>No outstanding balance on this sale.</p>
                  </div>
                )}

                {salePaymentsQuery.data && salePaymentsQuery.data.length > 0 ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Payment history</h3>
                        <p>Payments recorded against {selectedSale.saleNumber}.</p>
                      </div>
                    </div>
                    {salePaymentsQuery.data.map((payment) => (
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

                {selectedSale.items.some((item) => Number(item.returnableQuantity) > 0) ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Record sale return</h3>
                        <p>Return part of this sale. Stock comes back in for the returned quantity.</p>
                      </div>
                    </div>

                    <div className="split-grid">
                      <div className="field">
                        <label htmlFor="return-date">Return date</label>
                        <input
                          id="return-date"
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
                        <label htmlFor="return-reason">Reason</label>
                        <input
                          id="return-reason"
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
                      <label htmlFor="return-notes">Notes</label>
                      <input
                        id="return-notes"
                        value={returnForm.notes}
                        onChange={(event) =>
                          setReturnForm((current) => ({
                            ...current,
                            notes: event.target.value,
                          }))
                        }
                        placeholder="Customer brought unused stock"
                      />
                    </div>

                    {selectedSale.items
                      .filter((item) => Number(item.returnableQuantity) > 0)
                      .map((item) => {
                        const quantity =
                          returnForm.items.find((candidate) => candidate.saleItemId === item.id)
                            ?.quantity ?? 0;

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
                              {formatMoney(quantity * Number(item.sellingPrice))}
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
                      disabled={returnSaleMutation.isPending}
                      onClick={() => {
                        setFieldErrors({});
                        setFeedback(null);
                        returnSaleMutation.mutate();
                      }}
                    >
                      {returnSaleMutation.isPending ? "Recording return..." : "Record sale return"}
                    </button>
                  </div>
                ) : (
                  <div className="empty-inline-state">
                    <strong>Nothing left to return</strong>
                    <p>All sold quantities on this sale have already been returned.</p>
                  </div>
                )}

                {saleReturnsQuery.data && saleReturnsQuery.data.length > 0 ? (
                  <div className="form-stack">
                    <div className="panel-heading">
                      <div>
                        <h3>Returns on this sale</h3>
                        <p>Earlier sale returns linked to {selectedSale.saleNumber}.</p>
                      </div>
                    </div>
                    {saleReturnsQuery.data.map((saleReturn) => (
                      <div key={saleReturn.id} className="empty-inline-state">
                        <strong>
                          {saleReturn.returnNumber} · {formatMoney(saleReturn.totalAmount)}
                        </strong>
                        <p>
                          {saleReturn.returnDate}
                          {saleReturn.reason ? ` · ${saleReturn.reason}` : ""}
                        </p>
                        {saleReturn.items.map((item) => (
                          <p key={item.id}>
                            {item.productName}: {Number(item.quantity).toFixed(3)} {item.unit}
                          </p>
                        ))}
                      </div>
                    ))}
                  </div>
                ) : null}

                <div className="field">
                  <label htmlFor="sale-cancel-reason">Cancellation reason</label>
                  <input
                    id="sale-cancel-reason"
                    value={cancellationReason}
                    onChange={(event) => setCancellationReason(event.target.value)}
                    placeholder="Duplicate invoice"
                    disabled={selectedSale.hasReturns}
                  />
                </div>

                {selectedSale.hasReturns ? (
                  <p className="inline-note">
                    This sale has returns, so it cannot be cancelled. Record another sale return
                    instead if needed.
                  </p>
                ) : null}

                <button
                  type="button"
                  className="ghost-button ghost-button--danger"
                  disabled={cancelSaleMutation.isPending || selectedSale.hasReturns}
                  onClick={() => cancelSaleMutation.mutate()}
                >
                  {cancelSaleMutation.isPending ? "Cancelling..." : "Cancel sale"}
                </button>
              </>
            ) : (
              <div className="empty-inline-state">
                <strong>Sale cancelled</strong>
                <p>{selectedSale.cancellationReason ?? "No cancellation reason recorded."}</p>
              </div>
            )}
          </article>
        ) : null}
      </section>
    </>
  );
}
