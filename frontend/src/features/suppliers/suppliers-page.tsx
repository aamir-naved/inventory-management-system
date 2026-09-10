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
  archiveSupplier,
  createSupplier,
  getSupplierSummary,
  listSupplierPurchases,
  listSuppliers,
  unarchiveSupplier,
  updateSupplier,
  type SupplierPayload,
  type SupplierRecord,
} from "@/features/suppliers/supplier-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";

const initialForm: SupplierPayload = {
  name: "",
  contactPerson: "",
  mobileNumber: "",
  addressLine: "",
  stateCode: "",
};

function toPayload(supplier: SupplierRecord): SupplierPayload {
  return {
    name: supplier.name,
    contactPerson: supplier.contactPerson ?? "",
    mobileNumber: supplier.mobileNumber ?? "",
    addressLine: supplier.addressLine ?? "",
    stateCode: supplier.stateCode ?? "",
  };
}

export function SuppliersPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const businessId = session?.businessId ?? null;
  const { formatMoney, formatDate } = useBusinessSettings();
  const [search, setSearch] = useState("");
  const deferredSearch = useDeferredValue(search);
  const [page, setPage] = useState(0);
  const [includeArchived, setIncludeArchived] = useState(false);
  const [selectedSupplier, setSelectedSupplier] = useState<SupplierRecord | null>(null);
  const [form, setForm] = useState<SupplierPayload>(initialForm);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const suppliersQuery = useQuery({
    queryKey: ["suppliers", businessId, deferredSearch, includeArchived, page],
    queryFn: () =>
      listSuppliers(businessId!, {
        search: deferredSearch,
        includeArchived,
        page,
      }),
    enabled: Boolean(businessId),
  });

  const summaryQuery = useQuery({
    queryKey: ["supplier-summary", businessId, selectedSupplier?.id],
    queryFn: () => getSupplierSummary(businessId!, selectedSupplier!.id),
    enabled: Boolean(businessId && selectedSupplier),
  });

  const purchasesQuery = useQuery({
    queryKey: ["supplier-purchases", businessId, selectedSupplier?.id],
    queryFn: () => listSupplierPurchases(businessId!, selectedSupplier!.id),
    enabled: Boolean(businessId && selectedSupplier),
  });

  useEffect(() => {
    if (!selectedSupplier) {
      setForm(initialForm);
      return;
    }

    setForm(toPayload(selectedSupplier));
  }, [selectedSupplier]);

  useEffect(() => {
    setPage(0);
  }, [deferredSearch, includeArchived]);

  const saveMutation = useMutation({
    mutationFn: async (payload: SupplierPayload) => {
      if (!businessId) {
        throw new Error("Business setup is required before suppliers can be managed.");
      }

      if (selectedSupplier) {
        return updateSupplier(businessId, selectedSupplier.id, payload);
      }

      return createSupplier(businessId, payload);
    },
    onSuccess: async (supplier) => {
      setFeedback(selectedSupplier ? "Supplier updated." : "Supplier created.");
      setFieldErrors({});
      setSelectedSupplier(supplier);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["suppliers", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["supplier-summary", businessId, supplier.id] }),
        queryClient.invalidateQueries({
          queryKey: ["supplier-purchases", businessId, supplier.id],
        }),
      ]);
    },
    onError: (error) => {
      if (error instanceof ApiError && typeof error.details === "object" && error.details !== null) {
        const response = error.details as {
          fieldErrors?: Record<string, string>;
          message?: string;
        };

        setFieldErrors(response.fieldErrors ?? {});
        setFeedback(response.message ?? "Unable to save supplier.");
        return;
      }

      setFeedback("Unable to save supplier.");
    },
  });

  const archiveMutation = useMutation({
    mutationFn: async (supplier: SupplierRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before suppliers can be managed.");
      }

      return archiveSupplier(businessId, supplier.id);
    },
    onSuccess: async (supplier) => {
      setFeedback(`${supplier.name} archived and hidden from the default list.`);
      if (selectedSupplier?.id === supplier.id) {
        setSelectedSupplier(null);
      }
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["suppliers", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["supplier-summary", businessId, supplier.id] }),
        queryClient.invalidateQueries({
          queryKey: ["supplier-purchases", businessId, supplier.id],
        }),
      ]);
    },
    onError: () => {
      setFeedback("Unable to archive supplier.");
    },
  });

  const unarchiveMutation = useMutation({
    mutationFn: async (supplier: SupplierRecord) => {
      if (!businessId) {
        throw new Error("Business setup is required before suppliers can be managed.");
      }

      return unarchiveSupplier(businessId, supplier.id);
    },
    onSuccess: async (supplier) => {
      setFeedback(`${supplier.name} restored to the default list.`);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["suppliers", businessId] }),
        queryClient.invalidateQueries({ queryKey: ["supplier-summary", businessId, supplier.id] }),
        queryClient.invalidateQueries({
          queryKey: ["supplier-purchases", businessId, supplier.id],
        }),
      ]);
    },
    onError: () => {
      setFeedback("Unable to restore supplier.");
    },
  });

  function updateField<K extends keyof SupplierPayload>(key: K, value: SupplierPayload[K]) {
    setForm((current) => ({ ...current, [key]: value }));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFeedback(null);
    setFieldErrors({});
    await saveMutation.mutateAsync(form);
  }

  function resetForm() {
    setSelectedSupplier(null);
    setForm(initialForm);
    setFieldErrors({});
    setFeedback(null);
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <span className="brand-kicker">Business required</span>
        <h1>Finish business setup before managing suppliers.</h1>
        <p>Set up your business first so suppliers belong to the right shop.</p>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Supplier management</span>
        <h1>Track supplier contacts, what you owe, and purchase history together.</h1>
        <p>
          Create and update suppliers here, then open any party to review billed totals and every
          purchase tied to them. Archive hides a supplier from the default list; use Show archived
          to find them, then Restore to bring them back.
        </p>
      </section>

      <section className="workspace-grid">
        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>{selectedSupplier ? "Edit supplier" : "Create supplier"}</h3>
              <p>Contact details used across purchases and outstanding reports.</p>
            </div>
            {selectedSupplier ? (
              <button type="button" className="ghost-button" onClick={resetForm}>
                New supplier
              </button>
            ) : null}
          </div>

          <form className="form-stack" onSubmit={handleSubmit}>
            <div className="field">
              <label htmlFor="supplier-name">Supplier name</label>
              <input
                id="supplier-name"
                value={form.name}
                onChange={(event) => updateField("name", event.target.value)}
                placeholder="Shakti Cements Ltd"
              />
              {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
            </div>

            <div className="split-grid">
              <div className="field">
                <label htmlFor="supplier-contact">Contact person</label>
                <input
                  id="supplier-contact"
                  value={form.contactPerson}
                  onChange={(event) => updateField("contactPerson", event.target.value)}
                  placeholder="Rahul Mehta"
                />
              </div>

              <div className="field">
                <label htmlFor="supplier-mobile">Mobile number</label>
                <input
                  id="supplier-mobile"
                  value={form.mobileNumber}
                  onChange={(event) => updateField("mobileNumber", event.target.value)}
                  placeholder="+91 9876543210"
                />
              </div>
            </div>

            <div className="field">
              <label htmlFor="supplier-address">Address</label>
              <input
                id="supplier-address"
                value={form.addressLine}
                onChange={(event) => updateField("addressLine", event.target.value)}
                placeholder="Industrial Road"
              />
            </div>

            <div className="field">
              <label htmlFor="supplier-state-code">State code</label>
              <input
                id="supplier-state-code"
                value={form.stateCode}
                maxLength={2}
                onChange={(event) => updateField("stateCode", event.target.value.toUpperCase())}
                placeholder="29"
              />
              <p className="inline-note">GST state code — used to choose CGST/SGST vs IGST.</p>
            </div>

            {feedback ? <p className="inline-note">{feedback}</p> : null}

            <button type="submit" className="primary-button" disabled={saveMutation.isPending}>
              {saveMutation.isPending
                ? "Saving..."
                : selectedSupplier
                  ? "Save supplier"
                  : "Create supplier"}
            </button>
          </form>
        </article>

        <article className="panel">
          <div className="panel-heading">
            <div>
              <h3>Suppliers</h3>
              <p>
                Search by name, contact, or mobile. Archived suppliers stay saved but are
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
            <label htmlFor="supplier-search">Search suppliers</label>
            <input
              id="supplier-search"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search suppliers"
            />
          </div>

          {suppliersQuery.isLoading ? <p className="inline-note">Loading suppliers...</p> : null}

          {!suppliersQuery.isLoading && (suppliersQuery.data?.totalItems ?? 0) === 0 ? (
            <div className="empty-inline-state">
              <strong>No suppliers yet</strong>
              <p>Create your first supplier to track purchases and dues.</p>
            </div>
          ) : null}

          <div className="product-list">
            {suppliersQuery.data?.items.map((supplier) => (
              <article
                key={supplier.id}
                className={`product-card${
                  selectedSupplier?.id === supplier.id ? " product-card--selected" : ""
                }`}
              >
                <div className="product-card__row">
                  <div>
                    <h4>{supplier.name}</h4>
                    <p>
                      {supplier.contactPerson ?? "No contact"} ·{" "}
                      {supplier.mobileNumber ?? "No mobile"}
                    </p>
                  </div>
                  <span
                    className={`status-chip ${
                      supplier.archived ? "status-chip--warn" : "status-chip--success"
                    }`}
                  >
                    {supplier.archived ? "Archived" : "Active"}
                  </span>
                </div>

                {supplier.addressLine ? (
                  <div className="product-metrics">
                    <span>{supplier.addressLine}</span>
                  </div>
                ) : null}

                <div className="product-card__actions">
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() => {
                      setFeedback(null);
                      setFieldErrors({});
                      setSelectedSupplier(supplier);
                    }}
                  >
                    Open
                  </button>
                  {!supplier.archived ? (
                    <button
                      type="button"
                      className="ghost-button ghost-button--danger"
                      disabled={archiveMutation.isPending}
                      onClick={() => archiveMutation.mutate(supplier)}
                    >
                      Archive
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="ghost-button"
                      disabled={unarchiveMutation.isPending}
                      onClick={() => unarchiveMutation.mutate(supplier)}
                    >
                      Restore
                    </button>
                  )}
                </div>
              </article>
            ))}
          </div>
          <PaginationBar page={suppliersQuery.data} onPageChange={setPage} />
        </article>
      </section>

      {selectedSupplier ? (
        <section className="panel-stack">
          <article className="panel">
            <div className="panel-heading">
              <div>
                <h3>Outstanding balance</h3>
                <p>
                  {selectedSupplier.name} · payments on <Link to="/purchases">Purchases</Link>
                </p>
              </div>
            </div>

            {summaryQuery.isLoading ? <p className="inline-note">Loading balance...</p> : null}

            {summaryQuery.data ? (
              <div className="product-metrics">
                <span>Bills: {summaryQuery.data.billCount}</span>
                <span>Billed: {formatMoney(summaryQuery.data.billedAmount)}</span>
                <span>Paid: {formatMoney(summaryQuery.data.amountPaid)}</span>
                <span>Due: {formatMoney(summaryQuery.data.outstandingAmount)}</span>
              </div>
            ) : null}
          </article>

          <article className="panel">
            <div className="panel-heading">
              <div>
                <h3>Purchase history</h3>
                <p>All purchases for this supplier, including cancelled bills.</p>
              </div>
            </div>

            {purchasesQuery.isLoading ? (
              <p className="inline-note">Loading purchases...</p>
            ) : null}

            {!purchasesQuery.isLoading && (purchasesQuery.data?.length ?? 0) === 0 ? (
              <div className="empty-inline-state">
                <strong>No purchases yet</strong>
                <p>Record a purchase for this supplier from the Purchases page.</p>
              </div>
            ) : null}

            <div className="product-list">
              {purchasesQuery.data?.map((purchase) => (
                <article key={purchase.id} className="product-card">
                  <div className="product-card__row">
                    <div>
                      <h4>{purchase.purchaseNumber}</h4>
                      <p>{formatDate(purchase.purchaseDate)}</p>
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
                    <span>Total: {formatMoney(purchase.totalAmount)}</span>
                    {Number(purchase.returnedAmount) > 0 ? (
                      <span>Returned: {formatMoney(purchase.returnedAmount)}</span>
                    ) : null}
                    <span>Net: {formatMoney(purchase.netAmount)}</span>
                    <span>Paid: {formatMoney(purchase.amountPaid)}</span>
                    <span>Due: {formatMoney(purchase.outstandingAmount)}</span>
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
