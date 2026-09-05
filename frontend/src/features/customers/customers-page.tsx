import {
  useDeferredValue,
  useEffect,
  useState,
  type FormEvent,
} from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { PaginationBar } from "@/components/ui/pagination-bar";
import { useAuth } from "@/features/auth/auth-context";
import {
  archiveCustomer,
  createCustomer,
  getCustomerSummary,
  listCustomerSales,
  listCustomers,
  unarchiveCustomer,
  updateCustomer,
  type CustomerPayload,
  type CustomerRecord,
} from "@/features/customers/customer-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";

const initialForm: CustomerPayload = {
  name: "",
  contactPerson: "",
  mobileNumber: "",
  addressLine: "",
};

function toPayload(customer: CustomerRecord): CustomerPayload {
  return {
    name: customer.name,
    contactPerson: customer.contactPerson ?? "",
    mobileNumber: customer.mobileNumber ?? "",
    addressLine: customer.addressLine ?? "",
  };
}

export function CustomersPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate } = useBusinessSettings();
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [page, setPage] = useState(0);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState<CustomerRecord | null>(null);
  const [form, setForm] = useState<CustomerPayload>(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const customersQuery = useQuery({
    queryKey: ["customers", businessId, deferredSearch, includeArchived, page],
    queryFn: () =>
      listCustomers(businessId!, {
        search: deferredSearch,
        includeArchived,
        page,
      }),
    enabled: Boolean(businessId),
  });

  const summaryQuery = useQuery({
    queryKey: ["customer-summary", businessId, selectedCustomer?.id],
    queryFn: () => getCustomerSummary(businessId!, selectedCustomer!.id),
    enabled: Boolean(businessId && selectedCustomer),
  });

  const salesQuery = useQuery({
    queryKey: ["customer-sales", businessId, selectedCustomer?.id],
    queryFn: () => listCustomerSales(businessId!, selectedCustomer!.id),
    enabled: Boolean(businessId && selectedCustomer),
  });

  useEffect(() => {
    if (!selectedCustomer) {
      setForm(initialForm);
      return;
    }

    setForm(toPayload(selectedCustomer));
  }, [selectedCustomer]);

  useEffect(() => {
    setPage(0);
  }, [deferredSearch, includeArchived]);

  const saveMutation = useMutation({
    mutationFn: async (payload: CustomerPayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before customers can be managed.");
      }

      if (selectedCustomer) {
        return updateCustomer(businessId, selectedCustomer.id, payload);
      }

      return createCustomer(businessId, payload);
    },
    onSuccess: async (customer) => {
      setFeedback(selectedCustomer ? "Customer updated." : "Customer created.");
      setFieldErrors({});
      setSelectedCustomer(customer);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["customers", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["customer-summary", businessId, customer.id] }),
        queryClient.invalidateQueries({ queryKey: ["customer-sales", businessId, customer.id] }),
      ]);
    },
    onError: (error) => {
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };

        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? "Unable to save customer.");
        return;
      }

      setFeedback("Unable to save customer.");
    },
  });

  const archiveMutation = useMutation({
    mutationFn: async (customer: CustomerRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before customers can be managed.");
      }

      return archiveCustomer(businessId, customer.id);
    },
    onSuccess: async (customer) => {
      setFeedback(`${customer.name} archived and hidden from the default list.`);
      if (selectedCustomer?.id === customer.id) {
        setSelectedCustomer(null);
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["customers", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["customer-summary", businessId, customer.id] }),
        queryClient.invalidateQueries({ queryKey: ["customer-sales", businessId, customer.id] }),
      ]);
    },
    onError: () => {
      setFeedback("Unable to archive customer.");
    },
  });

  const unarchiveMutation = useMutation({
    mutationFn: async (customer: CustomerRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before customers can be managed.");
      }

      return unarchiveCustomer(businessId, customer.id);
    },
    onSuccess: async (customer) => {
      setFeedback(`${customer.name} restored to the default list.`);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["customers", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["customer-summary", businessId, customer.id] }),
        queryClient.invalidateQueries({ queryKey: ["customer-sales", businessId, customer.id] }),
      ]);
    },
    onError: () => {
      setFeedback("Unable to restore customer.");
    },
  });

  function updateField<K extends keyof CustomerPayload>(key: K, value: CustomerPayload[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});
    await saveMutation.mutateAsync(form);
  }

  function resetForm() {
    setSelectedCustomer(null);
    setForm(initialForm);
    setFieldErrors({});
    setFeedback(null);
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before managing customers.</h1>
        <p>Set up your business first so customers belong to the right shop.</p>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Customer management</span>
        <h1>Keep customer contacts, outstanding balances, and sale history in one place.</h1>
        <p>
          Create and update customers here, then open any party to see what they owe and every
          sale tied to them. Archive hides a customer from the default list; use Show archived
          to find them, then Restore to bring them back.
        </p>
      </section>

      <section className="workspace-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>{selectedCustomer ? "Edit customer" : "Create customer"}</h3>
              <p>Contact details used across sales and outstanding reports.</p>
            </div>
            {selectedCustomer ? (
              <button type="button" className="ghost-button" onClick={resetForm}>
                New customer
              </button>
            ) : null}
          </div>

          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="customer-name">Customer name</label>
              <input
                id="customer-name"
                value={form.name}
                onChange={(event) => updateField("name", event.target.value)}
                placeholder="Apex Builders"
              />
              {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
            </div>

            <div className="split-grid">
              <div className="field">
                <label htmlFor="customer-contact">Contact person</label>
                <input
                  id="customer-contact"
                  value={form.contactPerson}
                  onChange={(event) => updateField("contactPerson", event.target.value)}
                  placeholder="Neha Shah"
                />
              </div>

              <div className="field">
                <label htmlFor="customer-mobile">Mobile number</label>
                <input
                  id="customer-mobile"
                  value={form.mobileNumber}
                  onChange={(event) => updateField("mobileNumber", event.target.value)}
                  placeholder="+91 9998887776"
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="customer-address">Address</label>
              <input
                id="customer-address"
                value={form.addressLine}
                onChange={(event) => updateField("addressLine", event.target.value)}
                placeholder="Ring Road"
              />
            </div>

            {feedback ? <p className="inline-note">{feedback}</p> : null}

            <button type="submit" className="primary-button" disabled={saveMutation.isPending}>
              {saveMutation.isPending
                ? "Saving..."
                : selectedCustomer
                  ? "Save customer"
                  : "Create customer"}
            </button>
          </form>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Customers</h3>
              <p>
                Search by name, contact, or mobile. Archived customers stay saved but are
                hidden unless Show archived is on. Restore puts them back on the default
                list.
              </p>
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
            <label htmlFor="customer-search">Search customers</label>
            <input
              id="customer-search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search customers"
            />
          </div>

          {customersQuery.isLoading ? <p className="inline-note">Loading customers...</p> : null}

          {!customersQuery.isLoading && (customersQuery.data?.totalItems ?? 0) === 0 ? (
            <div className="empty-inline-state">
              <strong>No customers yet</strong>
              <p>Create your first customer to track sales and dues.</p>
            </div>
          ) : null}

          <div className="product-list">
            {customersQuery.data?.items.map((customer) => (
              <article
                key={customer.id}
                className={`product-card${
                  selectedCustomer?.id === customer.id ? " product-card--selected" : ""
                }`}
              >
                <div className="product-card__row">
                  <div>
                    <h4>{customer.name}</h4>
                    <p>
                      {customer.contactPerson ?? "No contact"} ·{" "}
                      {customer.mobileNumber ?? "No mobile"}
                    </p>
                  </div>
                  <span
                    className={`status-chip ${
                      customer.archived ? "status-chip--warn" : "status-chip--success"
                    }`}
                  >
                    {customer.archived ? "Archived" : "Active"}
                  </span>
                </div>

                {customer.addressLine ? (
                  <div className="product-metrics">
                    <span>{customer.addressLine}</span>
                  </div>
                ) : null}

                <div className="product-card__actions">
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => {
                      setFeedback(null);
                      setFieldErrors({});
                      setSelectedCustomer(customer);
                    }}
                  >
                    Open
                  </button>
                  {!customer.archived ? (
                    <button
                      type="button"
                      className="ghost-button ghost-button--danger"
                      disabled={archiveMutation.isPending}
                      onClick={() => archiveMutation.mutate(customer)}
                    >
                      Archive
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="ghost-button"
                      disabled={unarchiveMutation.isPending}
                      onClick={() => unarchiveMutation.mutate(customer)}
                    >
                      Restore
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
          <PaginationBar page={customersQuery.data} onPageChange={setPage} />
        </article>
      </section>

      {selectedCustomer ? (
        <section className="panel-stack">
          <article className="panel">
            <div className="panel-heading">
              <div>
                <h3>Outstanding balance</h3>
                <p>
                  {selectedCustomer.name} · net after returns · payments on{" "}
                  <Link to="/sales">Sales</Link>
                </p>
              </div>
            </div>

            {summaryQuery.isLoading ? <p className="inline-note">Loading balance...</p> : null}

            {summaryQuery.data ? (
              <div className="product-metrics">
                <span>Invoices: {summaryQuery.data.invoiceCount}</span>
                <span>Billed: {formatMoney(summaryQuery.data.netBilled)}</span>
                <span>Paid: {formatMoney(summaryQuery.data.amountPaid)}</span>
                <span>Due: {formatMoney(summaryQuery.data.outstandingAmount)}</span>
              </div>
            ) : null}
          </article>

          <article className="panel">
            <div className="panel-heading">
              <div>
                <h3>Sale history</h3>
                <p>All sales for this customer, including cancelled invoices.</p>
              </div>
            </div>

            {salesQuery.isLoading ? <p className="inline-note">Loading sales...</p> : null}

            {!salesQuery.isLoading && (salesQuery.data?.length ?? 0) === 0 ? (
              <div className="empty-inline-state">
                <strong>No sales yet</strong>
                <p>Record a sale for this customer from the Sales page.</p>
              </div>
            ) : null}

            <div className="product-list">
              {salesQuery.data?.map((sale) => (
                <article key={sale.id} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{sale.saleNumber}</h4>
                      <p>{formatDate(sale.saleDate)}</p>
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
                    <span>Total: {formatMoney(sale.totalAmount)}</span>
                    {Number(sale.returnedAmount) > 0 ? (
                      <span>Returned: {formatMoney(sale.returnedAmount)}</span>
                    ) : null}
                    <span>Net: {formatMoney(sale.netAmount)}</span>
                    <span>Paid: {formatMoney(sale.amountPaid)}</span>
                    <span>Due: {formatMoney(sale.outstandingAmount)}</span>
                  </div>
                </article>
              ))}
            </div>
          </article>
        </section>
      ) : null}
    </>
  );
}
