import {
  useDeferredValue,
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
} from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { EntityPicker } from "@/components/ui/entity-picker";
import { FieldInfo, FieldLabel } from "@/components/ui/field-label";
import { PaginationBar } from "@/components/ui/pagination-bar";
import { useAuth } from "@/features/auth/auth-context";
import { computeGstLine, isInterstateSupply } from "@/lib/gst";
import { newIdempotencyKey } from "@/lib/idempotency";
import { useBusinessSettings } from "@/features/settings/use-business-settings";
import {
  createCustomer,
  getCustomer,
  listCustomers,
  type CustomerPayload,
  type CustomerRecord,
} from "@/features/customers/customer-api";
import { getProduct, listProducts, type ProductRecord } from "@/features/products/product-api";
import {
  createSalePayment,
  listSalePayments,
  type PaymentPayload,
} from "@/features/payments/payment-api";
import {
  downloadSaleInvoice,
  printSaleInvoice,
  shareSaleInvoice,
} from "@/features/documents/document-api";
import {
  parseNumericDraft,
  resolveNumericDraft,
  type NumericDraft,
} from "@/lib/numeric-draft";
import {
  cancelSale,
  createSale,
  createSaleReturn,
  getSale,
  listSaleReturns,
  listSales,
  updateSale,
  type SalePayload,
  type SaleRecord,
} from "@/features/sales/sales-api";

type SaleItemDraft = {
  productId: string;
  quantity: NumericDraft;
  sellingPrice: NumericDraft;
  gstRate: number;
};

type SaleFormState = {
  customerId: string;
  saleDate: string;
  amountPaid: NumericDraft;
  notes: string;
  items: SaleItemDraft[];
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
  items: Array<{ saleItemId: string; quantity: NumericDraft }>;
};

const initialCustomer: CustomerPayload = {
  name: "",
  contactPerson: "",
  mobileNumber: "",
  addressLine: "",
  stateCode: "",
};

const initialSale: SaleFormState = {
  customerId: "",
  saleDate: new Date().toISOString().slice(0, 10),
  amountPaid: "",
  notes: "",
  items: [],
};

const initialPaymentForm: PaymentFormState = {
  paymentDate: new Date().toISOString().slice(0, 10),
  amount: "",
  notes: "",
};

function emptyItem(product?: ProductRecord): SaleItemDraft {
  return {
    productId: product?.id ?? "",
    quantity: "",
    sellingPrice: product ? Number(product.sellingPrice) : "",
    gstRate: product ? Number(product.gstRate ?? 0) : 0,
  };
}

function summarizeTotal(
  items: SaleItemDraft[],
  gstEnabled: boolean,
  inclusive: boolean,
  interstate: boolean,
) {
  return items.reduce((total, item) => {
    const line = computeGstLine(
      resolveNumericDraft(item.quantity),
      resolveNumericDraft(item.sellingPrice),
      gstEnabled ? item.gstRate : 0,
      inclusive,
      interstate,
    );
    return total + line.lineTotal;
  }, 0);
}

function toSalePayload(form: SaleFormState): SalePayload {
  return {
    customerId: form.customerId,
    saleDate: form.saleDate,
    amountPaid: resolveNumericDraft(form.amountPaid),
    notes: form.notes,
    items: form.items.map((item) => ({
      productId: item.productId,
      quantity: resolveNumericDraft(item.quantity),
      sellingPrice: resolveNumericDraft(item.sellingPrice),
      gstRate: item.gstRate,
    })),
  };
}

export function SalesPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate, gstEnabled, gstInclusivePricing, stateCode: businessStateCode } =
    useBusinessSettings();
  const [customerForm, setCustomerForm] = useState<CustomerPayload>(initialCustomer);
  const [saleForm, setSaleForm] = useState<SaleFormState>(initialSale);
  const [saleSearch, setSaleSearch] = useState("");
  const deferredSaleSearch = useDeferredValue(saleSearch);
  const [salePage, setSalePage] = useState(0);
  const [customerListSearch, setCustomerListSearch] = useState("");
  const deferredCustomerListSearch = useDeferredValue(customerListSearch);
  const [customerListPage, setCustomerListPage] = useState(0);
  const [knownProducts, setKnownProducts] = useState<Record<string, ProductRecord>>({});
  const [selectedSale, setSelectedSale] = useState<SaleRecord | null>(null);
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
  const saleIdempotencyKeyRef = useRef<string | null>(null);
  const paymentIdempotencyKeyRef = useRef<string | null>(null);

  const customersQuery = useQuery({
    queryKey: ["customers", businessId, deferredCustomerListSearch, false, customerListPage],
    queryFn: () =>
      listCustomers(businessId!, {
        search: deferredCustomerListSearch,
        page: customerListPage,
      }),
    enabled: Boolean(businessId),
  });

  const salesQuery = useQuery({
    queryKey: ["sales", businessId, deferredSaleSearch, salePage],
    queryFn: () => listSales(businessId!, deferredSaleSearch, { page: salePage }),
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

  useEffect(() => {
    setSalePage(0);
  }, [deferredSaleSearch]);

  useEffect(() => {
    setCustomerListPage(0);
  }, [deferredCustomerListSearch]);

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

      if (!saleIdempotencyKeyRef.current) {
        saleIdempotencyKeyRef.current = newIdempotencyKey();
      }
      return createSale(businessId, payload, saleIdempotencyKeyRef.current);
    },
    onSuccess: async (sale) => {
      saleIdempotencyKeyRef.current = null;
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
    onError: (error) => {
      if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
        saleIdempotencyKeyRef.current = null;
      }
      handleApiError(error);
    },
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

      if (paymentForm.amount === "" || resolveNumericDraft(paymentForm.amount) <= 0) {
        throw new Error("Enter a payment amount.");
      }

      const payload: PaymentPayload = {
        paymentDate: paymentForm.paymentDate,
        amount: resolveNumericDraft(paymentForm.amount),
        notes: paymentForm.notes,
      };

      if (!paymentIdempotencyKeyRef.current) {
        paymentIdempotencyKeyRef.current = newIdempotencyKey();
      }
      return createSalePayment(
        businessId,
        selectedSale.id,
        payload,
        paymentIdempotencyKeyRef.current,
      );
    },
    onSuccess: async () => {
      paymentIdempotencyKeyRef.current = null;
      setFeedback("Payment recorded.");
      setPaymentForm({
        ...initialPaymentForm,
        paymentDate: new Date().toISOString().slice(0, 10),
      });
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["sales", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["sale-payments", businessId] }),
      ]);

      const refreshed = selectedSale
        ? await getSale(businessId!, selectedSale.id)
        : null;
      if (refreshed) {
        setSelectedSale(refreshed);
      }
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
        paymentIdempotencyKeyRef.current = null;
      }
      if (error instanceof Error && !(error instanceof ApiError)) {
        setFeedback(error.message);
        return;
      }
      handleApiError(error);
    },
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

      const items = returnForm.items
        .map((item) => ({
          saleItemId: item.saleItemId,
          quantity: resolveNumericDraft(item.quantity),
        }))
        .filter((item) => item.quantity > 0);
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

      const refreshed = await getSale(businessId!, saleReturn.saleId);
      selectSale(refreshed);
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

  const saleInterstate = isInterstateSupply(
    businessStateCode,
    customersQuery.data?.items.find((customer) => customer.id === saleForm.customerId)?.stateCode,
  );

  const saleTotal = useMemo(
    () => summarizeTotal(saleForm.items, gstEnabled, gstInclusivePricing, saleInterstate),
    [saleForm.items, saleInterstate, gstEnabled, gstInclusivePricing],
  );
  const amountPaidNow = resolveNumericDraft(saleForm.amountPaid);
  const outstandingAfterSave = Math.max(saleTotal - amountPaidNow, 0);

  const returnTotal = useMemo(() => {
    if (!selectedSale) {
      return 0;
    }

    return returnForm.items.reduce((total, item) => {
      const saleItem = selectedSale.items.find((candidate) => candidate.id === item.saleItemId);
      if (!saleItem) {
        return total;
      }
      return total + resolveNumericDraft(item.quantity) * Number(saleItem.sellingPrice);
    }, 0);
  }, [returnForm.items, selectedSale]);

  function selectSale(sale: SaleRecord) {
    setSelectedSale(sale);
    setCancellationReason("");
    setPaymentForm({
      ...initialPaymentForm,
      paymentDate: new Date().toISOString().slice(0, 10),
      amount: Number(sale.outstandingAmount) > 0 ? Number(sale.outstandingAmount) : "",
    });
    setReturnForm({
      returnDate: new Date().toISOString().slice(0, 10),
      reason: "",
      notes: "",
      items: sale.items
        .filter((item) => Number(item.returnableQuantity) > 0)
        .map((item) => ({
          saleItemId: item.id,
          quantity: "",
        })),
    });
    setFeedback(null);
    setFieldErrors({});
  }

  function updateReturnQuantity(saleItemId: string, quantity: NumericDraft) {
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
    setSaleForm((current) => ({
      ...current,
      items: [...current.items, emptyItem()],
    }));
  }

  function updateItem(index: number, nextItem: SaleItemDraft) {
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
    if (saleForm.items.length === 0) {
      setFeedback("Add at least one product to this sale.");
      return;
    }

    const incompleteItem = saleForm.items.find(
      (item) =>
        !item.productId ||
        item.quantity === "" ||
        resolveNumericDraft(item.quantity) <= 0 ||
        item.sellingPrice === "",
    );

    if (incompleteItem) {
      setFeedback("Each item needs a product, quantity, and selling price.");
      return;
    }

    await saleMutation.mutateAsync(toSalePayload(saleForm));
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before recording sales.</h1>
        <p>Set up your business first so sales belong to the right shop.</p>
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

            <div className="field">
              <label htmlFor="customer-state-code">State code</label>
              <input
                id="customer-state-code"
                value={customerForm.stateCode}
                maxLength={2}
                onChange={(event) =>
                  setCustomerForm((current) => ({
                    ...current,
                    stateCode: event.target.value.toUpperCase(),
                  }))
                }
                placeholder="29"
              />
            </div>

            <button type="submit" className="ghost-button" disabled={customerMutation.isPending}>
              {customerMutation.isPending ? "Creating customer..." : "Create customer"}
            </button>
          </form>

          <div className="field">
            <label htmlFor="sale-customer-search">Search customers</label>
            <input
              id="sale-customer-search"
              value={customerListSearch}
              onChange={(event) => setCustomerListSearch(event.target.value)}
              placeholder="Search customers"
            />
          </div>

          <div className="supplier-list">
            {customersQuery.data?.items.map((customer) => (
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
          <PaginationBar page={customersQuery.data} onPageChange={setCustomerListPage} />
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Create sale</h3>
              <p>
                Record stock you sold to a customer. Saving this reduces quantity in inventory
                and tracks how much they paid versus what they still owe.
              </p>
            </div>
          </div>

          <form className="form-stack" onSubmit={handleCreateSale}>
            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="sale-customer"
                  label="Customer"
                  info="Who you sold to. Create a customer on the left if they are not listed yet."
                />
                <EntityPicker
                  id="sale-customer"
                  value={saleForm.customerId}
                  enabled={Boolean(businessId)}
                  placeholder="Search customers"
                  queryKey={["customers", businessId]}
                  fetchPage={(search) => listCustomers(businessId!, { search })}
                  fetchById={(id) => getCustomer(businessId!, id)}
                  getId={(customer: CustomerRecord) => customer.id}
                  getLabel={(customer: CustomerRecord) => customer.name}
                  onChange={(customerId) =>
                    setSaleForm((current) => ({ ...current, customerId }))
                  }
                />
                {fieldErrors.customerId ? <span className="field-error">{fieldErrors.customerId}</span> : null}
              </div>

              <div className="field">
                <FieldLabel
                  htmlFor="sale-date"
                  label="Sale date"
                  info="The date this sale happened."
                />
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

            {gstEnabled ? (
              <p className="inline-note">
                {saleInterstate
                  ? "Interstate (IGST) — customer state differs from business state."
                  : "Intrastate (CGST/SGST) — based on customer and business state codes."}
              </p>
            ) : null}

            <div className="split-grid">
              <div className="field">
                <FieldLabel
                  htmlFor="sale-amount-paid"
                  label="Amount paid now"
                  info="Cash or transfer received from the customer today. Leave blank or 0 if they will pay later — the unpaid part becomes outstanding."
                />
                <input
                  id="sale-amount-paid"
                  type="number"
                  min="0"
                  step="0.01"
                  inputMode="decimal"
                  value={saleForm.amountPaid}
                  onChange={(event) =>
                    setSaleForm((current) => ({
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
                  htmlFor="sale-notes"
                  label="Notes"
                  info="Optional reminder for this sale, such as a delivery note or counter reference."
                />
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
                  <div className="field-label-row">
                    <h3>What did you sell?</h3>
                    <FieldInfo label="Sale items">
                      Add each product you sold. Quantity decreases stock using the product&apos;s
                      unit (Bags, Kg, etc.). Selling price is what the customer pays per unit.
                    </FieldInfo>
                  </div>
                  <p>For each line: choose the product, enter how many units, and the price per unit.</p>
                </div>
                <button type="button" className="ghost-button" onClick={addItem}>
                  Add item
                </button>
              </div>

              {saleForm.items.length === 0 ? (
                <div className="empty-inline-state">
                  <strong>No items yet</strong>
                  <p>Click Add item, then fill product, quantity, and selling price.</p>
                </div>
              ) : null}

              {saleForm.items.map((item, index) => {
                const product = knownProducts[item.productId];
                const lineTotal =
                  resolveNumericDraft(item.quantity) * resolveNumericDraft(item.sellingPrice);

                return (
                  <div key={`sale-item-${index}`} className="purchase-item-card">
                    <div className="purchase-item-row purchase-item-row--compose">
                      <div className="field">
                        <FieldLabel
                          htmlFor={`sale-item-product-${index}`}
                          label="Product"
                          info="The catalog product being sold. Stock will decrease for this product."
                        />
                        <EntityPicker
                          id={`sale-item-product-${index}`}
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
                              sellingPrice: selected
                                ? Number(selected.sellingPrice)
                                : item.sellingPrice,
                              gstRate: selected ? Number(selected.gstRate ?? 0) : item.gstRate,
                            });
                          }}
                        />
                      </div>

                      <div className="field">
                        <FieldLabel
                          htmlFor={`sale-item-qty-${index}`}
                          label="Quantity"
                          info={`How many units you sold${product ? ` (in ${product.unit})` : ""}. This amount is taken from current stock.`}
                        />
                        <input
                          id={`sale-item-qty-${index}`}
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
                          placeholder="e.g. 10"
                        />
                        {product ? (
                          <span className="field-hint">
                            Unit: {product.unit} · Stock {Number(product.currentStock).toFixed(3)}
                          </span>
                        ) : null}
                      </div>

                      <div className="field">
                        <FieldLabel
                          htmlFor={`sale-item-price-${index}`}
                          label="Price per unit"
                          info="What the customer pays for one unit. Line total = quantity × price per unit."
                        />
                        <input
                          id={`sale-item-price-${index}`}
                          type="number"
                          min="0"
                          step="0.01"
                          inputMode="decimal"
                          value={item.sellingPrice}
                          onChange={(event) =>
                            updateItem(index, {
                              ...item,
                              sellingPrice: parseNumericDraft(event.target.value),
                            })
                          }
                          placeholder="e.g. 350"
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
                <strong>Total sale amount</strong>
                <span>{formatMoney(saleTotal)}</span>
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
            {salesQuery.data?.items.map((sale) => (
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
          <PaginationBar page={salesQuery.data} onPageChange={setSalePage} />
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
                    const result = await shareSaleInvoice(
                      businessId,
                      selectedSale.id,
                      selectedSale.saleNumber,
                    );
                    if (result === "shared") {
                      setFeedback("Invoice shared.");
                    } else if (result === "downloaded") {
                      setFeedback("Invoice downloaded. Attach the PDF in WhatsApp.");
                    }
                  } catch (error) {
                    if (error instanceof Error && !(error instanceof ApiError)) {
                      setFeedback(error.message);
                      return;
                    }
                    handleApiError(error);
                  }
                }}
              >
                Share bill
              </button>
              <button
                type="button"
                className="ghost-button"
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
                        <FieldLabel
                          htmlFor="sale-payment-amount"
                          label="Amount"
                          info="How much the customer is paying now. It cannot be more than the outstanding balance."
                        />
                        <input
                          id="sale-payment-amount"
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
                          {payment.paymentKind === "REFUND" ? "Refund · " : ""}
                          {payment.paymentDate}
                          {payment.notes ? ` · ${payment.notes}` : ""}
                        </span>
                        <span>
                          {payment.paymentKind === "REFUND" ? "−" : ""}
                          {formatMoney(payment.amount)}
                        </span>
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
                            ?.quantity ?? "";
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
                                <label htmlFor={`sale-return-qty-${item.id}`}>Quantity</label>
                                <input
                                  id={`sale-return-qty-${item.id}`}
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
                                  {formatMoney(returnQty * Number(item.sellingPrice))}
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
