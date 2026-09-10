import { useEffect, useRef, useState, type FormEvent, type KeyboardEvent } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import { listCustomers, type CustomerRecord } from "@/features/customers/customer-api";
import { getProductByBarcode, type ProductRecord } from "@/features/products/product-api";
import { createSale } from "@/features/sales/sales-api";
import { printSaleInvoice, shareSaleInvoice } from "@/features/documents/document-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";
import { newIdempotencyKey } from "@/lib/idempotency";
import { computeGstLine, isInterstateSupply } from "@/lib/gst";
import { useOnlineStatus } from "@/lib/use-online-status";
import { useT } from "@/i18n/locale-context";

type PosLine = {
  product: ProductRecord;
  quantity: number;
};

const POS_HINT_KEY = "ims.pos.first-run-hint";

function walkInCustomerId(customers: CustomerRecord[]) {
  const walkIn = customers.find((customer) => customer.name.trim().toLowerCase() === "walk-in");
  return walkIn?.id ?? customers[0]?.id ?? "";
}

export function PosPage() {
  const queryClient = useQueryClient();
  const { session } = useAuth();
  const online = useOnlineStatus();
  const t = useT();
  const businessId = session?.businessId ?? null;
  const { formatMoney, gstEnabled, gstInclusivePricing, stateCode: businessStateCode } =
    useBusinessSettings();
  const [barcode, setBarcode] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [lines, setLines] = useState<PosLine[]>([]);
  const [amountPaid, setAmountPaid] = useState("");
  const [feedback, setFeedback] = useState<string | null>(null);
  const [lastSale, setLastSale] = useState<{ id: string; saleNumber: string } | null>(null);
  const checkoutKeyRef = useRef<string | null>(null);
  const [showHint, setShowHint] = useState(() => {
    try {
      return window.localStorage.getItem(POS_HINT_KEY) !== "dismissed";
    } catch {
      return true;
    }
  });

  const customersQuery = useQuery({
    queryKey: ["pos-customers", businessId],
    queryFn: () => listCustomers(businessId!, { size: 100 }),
    enabled: Boolean(businessId),
  });

  const customers: CustomerRecord[] = customersQuery.data?.items ?? [];
  const selectedCustomer = customers.find((customer) => customer.id === customerId);
  const interstate = isInterstateSupply(businessStateCode, selectedCustomer?.stateCode);

  useEffect(() => {
    if (customerId || customers.length === 0) {
      return;
    }
    setCustomerId(walkInCustomerId(customers));
  }, [customers, customerId]);

  function dismissHint() {
    setShowHint(false);
    try {
      window.localStorage.setItem(POS_HINT_KEY, "dismissed");
    } catch {
      // Ignore private-mode storage failures.
    }
  }

  function addProduct(product: ProductRecord) {
    setLines((current) => {
      const existing = current.find((line) => line.product.id === product.id);
      if (existing) {
        return current.map((line) =>
          line.product.id === product.id
            ? { ...line, quantity: line.quantity + 1 }
            : line,
        );
      }
      return [...current, { product, quantity: 1 }];
    });
  }

  async function lookupBarcode() {
    if (!businessId || !barcode.trim()) {
      return;
    }
    setFeedback(null);
    try {
      const product = await getProductByBarcode(businessId, barcode.trim());
      addProduct(product);
      setBarcode("");
    } catch (error) {
      setFeedback(error instanceof ApiError ? error.message : "No product for that barcode.");
    }
  }

  function handleKey(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter") {
      event.preventDefault();
      void lookupBarcode();
    }
  }

  const totals = lines.reduce(
    (acc, line) => {
      const gst = computeGstLine(
        line.quantity,
        Number(line.product.sellingPrice),
        gstEnabled ? Number(line.product.gstRate ?? 0) : 0,
        gstInclusivePricing,
        interstate,
      );
      return {
        taxable: acc.taxable + gst.taxableAmount,
        tax: acc.tax + gst.taxAmount,
        total: acc.total + gst.lineTotal,
      };
    },
    { taxable: 0, tax: 0, total: 0 },
  );

  const createMutation = useMutation({
    mutationFn: async () => {
      if (!businessId) {
        throw new Error("Business required");
      }
      if (!customerId) {
        throw new Error("Choose a customer");
      }
      if (lines.length === 0) {
        throw new Error("Scan or add at least one product");
      }
      if (!online) {
        throw new Error("You are offline. Reconnect before completing the sale.");
      }
      if (!checkoutKeyRef.current) {
        checkoutKeyRef.current = newIdempotencyKey();
      }
      return createSale(
        businessId,
        {
          customerId,
          saleDate: new Date().toISOString().slice(0, 10),
          amountPaid: Number(amountPaid || totals.total),
          notes: "POS sale",
          items: lines.map((line) => ({
            productId: line.product.id,
            quantity: line.quantity,
            sellingPrice: Number(line.product.sellingPrice),
            gstRate: Number(line.product.gstRate ?? 0),
          })),
        },
        checkoutKeyRef.current,
      );
    },
    onSuccess: (sale) => {
      checkoutKeyRef.current = null;
      setLines([]);
      setAmountPaid("");
      setLastSale({ id: sale.id, saleNumber: sale.saleNumber });
      setFeedback(`Saved ${sale.saleNumber}. Share the bill with the customer.`);
      void queryClient.invalidateQueries({ queryKey: ["sales"] });
      void queryClient.invalidateQueries({ queryKey: ["inventory-summary"] });
      void queryClient.invalidateQueries({ queryKey: ["inventory-stock"] });
      void queryClient.invalidateQueries({ queryKey: ["inventory-movements"] });
      void queryClient.invalidateQueries({ queryKey: ["products"] });
      void queryClient.invalidateQueries({ queryKey: ["dashboard-metrics"] });
      void queryClient.invalidateQueries({ queryKey: ["notifications"] });
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status >= 400 && error.status < 500 && error.status !== 408) {
        // Client errors (except timeout-like) mean this attempt is done — rotate key.
        checkoutKeyRef.current = null;
      }
      setFeedback(error instanceof Error ? error.message : "Unable to save the sale.");
    },
  });

  async function handleCheckout(event: FormEvent) {
    event.preventDefault();
    await createMutation.mutateAsync();
  }

  async function handleShare(saleId: string, saleNumber: string) {
    if (!businessId) {
      return;
    }
    try {
      const result = await shareSaleInvoice(businessId, saleId, saleNumber);
      if (result === "shared") {
        setFeedback(`Shared ${saleNumber}.`);
      } else if (result === "downloaded") {
        setFeedback(`Saved ${saleNumber}. Attach the PDF in WhatsApp.`);
      }
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : "Unable to share the invoice.");
    }
  }

  async function handlePrint(saleId: string, saleNumber: string) {
    if (!businessId) {
      return;
    }
    try {
      await printSaleInvoice(businessId, saleId);
      setFeedback(`Opened ${saleNumber} for printing.`);
    } catch (error) {
      setFeedback(error instanceof Error ? error.message : "Print the invoice from Sales if needed.");
    }
  }

  if (!businessId) {
    return (
      <section className="empty-state">
        <h1>{t("pos.emptyTitle")}</h1>
        <Link to="/welcome" className="primary-button">
          {t("pos.openShop")}
        </Link>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">{t("pos.kicker")}</span>
        <h1>{t("pos.title")}</h1>
        <p>{t("pos.subtitle")}</p>
      </section>

      {!online ? (
        <section className="panel" role="alert">
          <h3>{t("pos.offlineTitle")}</h3>
          <p className="inline-note">{t("pos.offlineBody")}</p>
        </section>
      ) : null}

      {showHint ? (
        <section className="panel">
          <h3>First bill</h3>
          <p className="inline-note">
            Walk-in is selected. Type <strong>CEM-001</strong> and press Enter for Cement if you
            have no scanner (starter items use their SKU as the barcode). After Complete sale,
            tap Share bill so the customer gets the PDF on WhatsApp.
          </p>
          <button type="button" className="ghost-button" onClick={dismissHint}>
            Got it
          </button>
        </section>
      ) : null}

      <section className="panel pos-panel">
        <form className="form-stack" onSubmit={handleCheckout}>
          <div className="field">
            <label htmlFor="pos-barcode">{t("pos.barcode")}</label>
            <input
              id="pos-barcode"
              autoFocus
              value={barcode}
              onChange={(event) => setBarcode(event.target.value)}
              onKeyDown={handleKey}
              placeholder={t("pos.barcodePlaceholder")}
            />
          </div>

          <div className="field">
            <label htmlFor="pos-customer">{t("pos.customer")}</label>
            <select
              id="pos-customer"
              value={customerId}
              onChange={(event) => setCustomerId(event.target.value)}
            >
              <option value="">{t("pos.selectCustomer")}</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.name}
                </option>
              ))}
            </select>
            <p className="inline-note">{t("pos.walkInNote")}</p>
          </div>

          {gstEnabled ? (
            <p className="inline-note">
              {interstate
                ? "Interstate (IGST) — customer state differs from business state."
                : "Intrastate (CGST/SGST) — based on customer and business state codes."}
            </p>
          ) : null}

          <ul className="list">
            {lines.map((line) => (
              <li key={line.product.id} className="product-card">
                <strong>{line.product.name}</strong>
                <div className="product-card__row">
                  <span>
                    {line.quantity} × {formatMoney(Number(line.product.sellingPrice))}
                  </span>
                  <button
                    type="button"
                    className="ghost-button"
                    onClick={() =>
                      setLines((current) =>
                        current.filter((item) => item.product.id !== line.product.id),
                      )
                    }
                  >
                    {t("pos.remove")}
                  </button>
                </div>
              </li>
            ))}
          </ul>

          <div className="report-summary">
            {gstEnabled ? (
              <p>
                {t("pos.taxable", { amount: formatMoney(totals.taxable) })} ·{" "}
                {t("pos.tax", { amount: formatMoney(totals.tax) })}
              </p>
            ) : null}
            <p>
              <strong>{t("pos.total", { amount: formatMoney(totals.total) })}</strong>
            </p>
          </div>

          <div className="field">
            <label htmlFor="pos-paid">{t("pos.amountPaid")}</label>
            <input
              id="pos-paid"
              type="number"
              min="0"
              step="0.01"
              value={amountPaid}
              onChange={(event) => setAmountPaid(event.target.value)}
              placeholder={String(totals.total || 0)}
            />
          </div>

          {feedback ? <p className="inline-note">{feedback}</p> : null}

          <button
            type="submit"
            className="primary-button"
            disabled={createMutation.isPending || !online}
          >
            {createMutation.isPending
              ? t("pos.saving")
              : !online
                ? t("pos.offlineTitle")
                : t("pos.completeSale")}
          </button>
        </form>

        {lastSale ? (
          <div className="product-card__actions">
            <button
              type="button"
              className="primary-button"
              onClick={() => {
                void handleShare(lastSale.id, lastSale.saleNumber);
              }}
            >
              {t("pos.shareBill")}
            </button>
            <button
              type="button"
              className="ghost-button"
              onClick={() => {
                void handlePrint(lastSale.id, lastSale.saleNumber);
              }}
            >
              {t("pos.printBill")}
            </button>
          </div>
        ) : null}
      </section>
    </>
  );
}
