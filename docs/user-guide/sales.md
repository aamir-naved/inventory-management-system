# Sales

**Menu:** Sales · **Address:** `/sales`  
**Who:** Owner, manager, and clerk. Clerks **cannot cancel** a sale (the server refuses it). Clerks **can** create sales, take payments, and record returns.

---

## Why this screen exists

This is the full **invoice desk**: named customer, several products, credit (pay later), GST (local vs interstate from state codes), notes, print/download PDF, collect money later, customer returns, and voiding a wrong invoice.

Saving a sale **decreases** stock. A return **increases** stock. Cancel **restores** stock (only if there are no returns).

For “scan, take cash, print” use [Counter](counter.md). Counter **creates the same kind of sale**; you will find it here afterwards to reprint or collect a remaining balance.

---

## Before you start

- At least one [customer](customers.md) (create `Walk-in` for cash).
- Products with enough stock (unless Settings allow negative stock).
- If GST is on: the customer’s **State code** should be set when they are in another state (so IGST is used). Blank state = local tax.

---

## Layout

**Left:** quick customer create + customer chips.  
**Right:** **Create sale** form.  
**Below:** **Sales history** and **Sale details** after **View details**.

---

## Quick customer setup (left)

Same fields as [Customers](customers.md). **Create customer** selects them on the sale form. **Search customers** + click a chip to pick a party.

---

## Create sale (right)

### Header fields

| Field | Required | What to type / choose |
|-------|----------|------------------------|
| **Customer** | Yes | Search-and-select, or click a chip. |
| **Sale date** | Yes | Defaults to today. Back-date if you are entering yesterday’s register. |
| Tax note | If GST is on | Read-only: **Intrastate (CGST/SGST)** or **Interstate (IGST)** from customer vs shop state. Change it by editing the customer’s **State code**, not a tick box. |
| **Amount paid now** | No (blank = 0) | Cash/UPI received **today**. `0` = full credit (**PENDING**). Full total = **PAID**. In between = **PARTIAL**. |
| **Notes** | No | Example: `Counter sale`, site name, PO number. Max 255 characters. |

### What did you sell? (lines)

Click **Add item**.

| Field | Required | What to type / choose |
|-------|----------|------------------------|
| **Product** | Yes | Search name or SKU. **Selling price** and GST % copy from the catalog; you may change the price for this invoice (discount or special rate). |
| **Quantity** | Yes | Units sold, in the product’s unit. Must be &gt; 0. Hint shows **Unit** and **Stock** available. |
| **Price per unit** | Yes | What the **customer pays** per unit. Line total = quantity × price (then GST if enabled). |
| **Remove** | | Drops the line. |

Need at least one complete line.

### Totals

- **Total sale amount**
- **Paid now**
- **Outstanding after save** — what they will still owe

Click **Record sale**. Success: **Sale recorded and stock reduced.** The sale number looks like `SAL/2025-26/000001` (Indian financial year).

If stock is insufficient and negative stock is not allowed, the save fails. Reduce quantity, purchase stock, adjust inventory, or enable negative stock.

---

## Sales history

**Search sales** — sale number, customer, or notes.

Cards: sale number, customer, date, **PENDING / PARTIAL / PAID** or **Cancelled**, items, total, returned, net, paid, due.

**View details** opens the invoice panel.

---

## Sale details

Totals, status, and each line: sold / returned / left to return / price.

### Download invoice / Share bill / Print invoice

| Button | What it does |
|--------|----------------|
| **Download invoice** | Saves the GST invoice PDF (logo, GSTIN, HSN/tax when configured). |
| **Share bill** | Phone share sheet with the PDF, or a download plus WhatsApp. |
| **Print invoice** | Opens print. Use this if you need a paper copy. |

### Save details

Change **Sale date** or **Notes** only, then **Save details**. Wrong products → cancel (owner/manager, no returns) or **Record sale return**.

### Record payment (if due &gt; 0)

| Field | What to type |
|-------|----------------|
| **Payment date** | Defaults to today. |
| **Amount** | Pre-filled with outstanding. Must be &gt; 0 and not more than due. |
| **Notes** | `Cash`, `UPI`, `Bank transfer`, cheque no. |

**Record payment**. **Payment history** lists earlier receipts (the amount typed at create time is stored as **Initial payment**). If this sale was later **cancelled**, a **Refund** line appears for the amount that had been paid.

**Fully paid** means nothing left to collect.

### Record sale return

Customer brings goods back. Stock **comes back in**. Net invoice and dues reduce.

| Field | What to type |
|-------|----------------|
| **Return date** | Defaults to today. |
| **Reason** | Example: `Damaged bags`. |
| **Notes** | Example: `Customer brought unused stock`. |
| **Quantity** per product | Up to **Returnable**. Leave blank if that line is not returned. At least one quantity &gt; 0. |

**Record sale return**. Listed under **Returns on this sale**. The credit includes tax (a ₹118 inclusive line refunds ₹118, not ₹100). Return numbers look like `RET/2025-26/000001`.

If nothing is returnable: **Nothing left to return**.

### Cancel sale (owner / manager)

Whole invoice was a mistake and **no returns** exist.

| Field | What to type |
|-------|----------------|
| **Cancellation reason** | Required. Example: `Duplicate invoice`. |

**Cancel sale**. Stock restored. Money that was already recorded is written as a **Refund** in payment history (receipts stay as a trail). History keeps the number as Cancelled.

If returns exist, cancel is blocked — use another return instead.

Clerks: do not rely on Cancel; ask an owner. The app will say the role cannot perform that action.

---

## Payment status

Same as purchases: **PENDING**, **PARTIAL**, **PAID**, or **Cancelled**.

Returns can turn a PARTIAL sale into PAID if the remaining net equals what was already paid (the customer then may be owed a refund outside this app — there is no separate “refund cash” voucher).

---

## GST

Product GST % and inclusive vs exclusive come from Settings / the catalog. Local vs interstate is **not** a tick on this form — see [GST on invoices](gst.md). Renaming a product later does **not** change the name already printed on this invoice.

---

## Sales vs Counter

| Use Sales when… | Use Counter when… |
|-----------------|-------------------|
| Credit, many lines, custom prices, notes | Queue at the desk, barcode, cash |
| You need to search products by name | You scan barcodes |
| You are entering yesterday’s register | Today’s live billing |

Both produce a sale you can reprint here.

---

## Common mistakes

- No customer selected.
- Quantity higher than stock.
- **Amount paid now** left 0 when cash was taken — they will show as outstanding until you record payment.
- Cancelling instead of returning two bags — cancel voids **everything**.
- Wrong **State code** on the customer (IGST vs CGST/SGST follows that, not a tick box).

---

## Related

- Fast path: [Counter](counter.md)
- Parties: [Customers](customers.md)
- Stock: [Inventory](inventory.md)
- Dues: [Reports](reports.md)
