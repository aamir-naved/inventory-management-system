import { useEffect, useState, type FormEvent, type KeyboardEvent } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { ApiError } from "@/api/http-client";
import { useAuth } from "@/features/auth/auth-context";
import { listCustomers, type CustomerRecord } from "@/features/customers/customer-api";
import { getProductByBarcode, type ProductRecord } from "@/features/products/product-api";
import { createSale } from "@/features/sales/sales-api";
import { printSaleInvoice, shareSaleInvoice } from "@/features/documents/document-api";
import { useBusinessSettings } from "@/features/settings/use-business-settings";
import { computeGstLine } from "@/lib/gst";

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
  const businessId = session?.businessId ?? null;
  const { formatMoney, gstEnabled, gstInclusivePricing } = useBusinessSettings();
  const [barcode, setBarcode] = useState("");
  const [customerId, setCustomerId] = useState("");
  const [lines, setLines] = useState<PosLine[]>([]);
  const [amountPaid, setAmountPaid] = useState("");
  const [interstate, setInterstate] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [lastSale, setLastSale] = useState<{ id: string; saleNumber: string } | null>(null);
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
      return createSale(businessId, {
        customerId,
        saleDate: new Date().toISOString().slice(0, 10),
        amountPaid: Number(amountPaid || totals.total),
        notes: "POS sale",
        interstate,
        items: lines.map((line) => ({
          productId: line.product.id,
          quantity: line.quantity,
          sellingPrice: Number(line.product.sellingPrice),
          gstRate: Number(line.product.gstRate ?? 0),
        })),
      });
    },
    onSuccess: (sale) => {
      setLines([]);
      setAmountPaid("");
      setLastSale({ id: sale.id, saleNumber: sale.saleNumber });
      setFeedback(`Saved ${sale.saleNumber}. Share the bill with the customer.`);
      void queryClient.invalidateQueries({ queryKey: ["sales"] });
    },
    onError: (error) => {
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
        <h1>Name the shop before using the counter.</h1>
        <Link to="/welcome" className="primary-button">
          Open the shop
        </Link>
      </section>
    );
  }

  return (
    <>
      <section className="page-intro">
        <span className="brand-kicker">Counter</span>
        <h1>Fast sale entry with barcode.</h1>
        <p>Scan or type a barcode, collect payment, then share or print the bill.</p>
      </section>

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
            <label htmlFor="pos-barcode">Barcode</label>
            <input
              id="pos-barcode"
              autoFocus
              value={barcode}
              onChange={(event) => setBarcode(event.target.value)}
              onKeyDown={handleKey}
              placeholder="Scan or type and press Enter"
            />
          </div>

          <div className="field">
            <label htmlFor="pos-customer">Customer</label>
            <select
              id="pos-customer"
              value={customerId}
              onChange={(event) => setCustomerId(event.target.value)}
            >
              <option value="">Select customer</option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customer.name}
                </option>
              ))}
            </select>
            <p className="inline-note">Walk-in is chosen automatically for cash sales.</p>
          </div>

          {gstEnabled ? (
            <label className="toggle">
              <input
                type="checkbox"
                checked={interstate}
                onChange={(event) => setInterstate(event.target.checked)}
              />
              <span>Interstate (IGST)</span>
            </label>
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
                    Remove
                  </button>
                </div>
              </li>
            ))}
          </ul>

          <div className="report-summary">
            {gstEnabled ? <p>Taxable {formatMoney(totals.taxable)} · Tax {formatMoney(totals.tax)}</p> : null}
            <p>
              <strong>Total {formatMoney(totals.total)}</strong>
            </p>
          </div>

          <div className="field">
            <label htmlFor="pos-paid">Amount paid</label>
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

          <button type="submit" className="primary-button" disabled={createMutation.isPending}>
            {createMutation.isPending ? "Saving..." : "Complete sale"}
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
              Share bill
            </button>
            <button
              type="button"
              className="ghost-button"
              onClick={() => {
                void handlePrint(lastSale.id, lastSale.saleNumber);
              }}
            >
              Print
            </button>
          </div>
        ) : null}
      </section>
    </>
  );
}
