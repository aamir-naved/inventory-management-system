# Reports

**Menu:** Reports · **Address:** `/reports`  
**Who:** Owner and manager. Hidden for clerks.

---

## Why this screen exists

On-screen totals plus **Excel** for the accountant (or to print from the browser). This is **not** the GSTR portal and **not** full books of account. It is stock, invoices, dues, and GST lines from **this shop**.

If business is missing: **Finish business setup before viewing reports.**

---

## How to use the page

1. Click a **tab** (Inventory, Sales, Purchases, Customer dues, Supplier dues, GST).
2. Set filters if that tab has them.
3. Read the summary cards and rows.
4. Optionally click **Download Excel** — file name like `sales-report.xlsx` for the **currently selected** tab and filters.

Use your browser’s print if you want paper of what you see.

---

## Tabs and filters

### Inventory

**Filter:** tick **Low stock only** to hide healthy items.

**Cards:** product count, stock value (weighted average cost × qty), how many are low.

**Each row:** name, SKU, In stock / Low stock, current stock, value, cost, selling price.

**When:** “What is in the godown?” / “What must I reorder?”

Excel includes the same view (low-stock filter is kept if ticked).

### Sales

**Filters:** **From date** and **To date** (optional). Empty = no bound on that side.

**Cards:** how many sales, **net amount**, **outstanding**.

**Each row:** sale number, customer, date, payment status, net, paid, due.

**When:** “What did we sell this month?” “Which invoices are unpaid?”

Cancelled sales are not the focus of a healthy period report; use Sales history if you need voids.

### Purchases

Same date filters as Sales.

**Cards:** bill count, total, outstanding.

**Each row:** purchase number, supplier, date, status, total, paid, due.

**When:** “What did we buy?” “What do we still owe suppliers?”

### Customer dues

No date filter. Parties with money still due.

**Cards:** how many customers, total outstanding.

**Each row:** name, open invoice count, outstanding, billed, paid.

**When:** “Who hasn’t paid?” Collect on [Sales](sales.md).

### Supplier dues

Same idea for suppliers. Pay on [Purchases](purchases.md).

### GST

**Filters:** From date, To date.

**Cards:**

| Card | Meaning |
|------|---------|
| **Output tax** | GST on sales minus sale returns |
| **Input tax** | GST on purchases minus purchase returns |
| **Net tax** | Output − input (working paper for the CA) |

**Each row:** document type + number (`SAL/` / `PUR/` / `RET/` / `PRT/…`), party, date, taxable, GST %, CGST, SGST, IGST. Returns are negative rows.

**When:** give Excel to the CA. This does **not** file GSTR-1. Interstate bills show IGST; local show CGST/SGST. Tax split follows party **State code**, not a tick on the bill.

If GST is off in Settings, this tab will be empty or not useful until you enable GST and record taxed bills.

---

## Date boxes

They are calendar fields. Example: From `2026-04-01`, To `2026-04-30` for April.

Leave both empty for all dates. Fill only **From** to mean “on or after”. Fill only **To** to mean “on or before”.

---

## Download Excel

Always exports **the tab you are looking at**, with **the filters currently set**. Switch tab, then download again for another file.

If download fails, check you are still signed in and the business is selected.

---

## Related

- Live stock: [Inventory](inventory.md)
- Invoices: [Sales](sales.md) / [Purchases](purchases.md)
- Alerts for overdue (7+ days): [Around the workspace](workspace.md)
