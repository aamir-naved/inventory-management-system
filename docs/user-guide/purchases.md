# Purchases

**Menu:** Purchases · **Address:** `/purchases`  
**Who:** Owner and manager. Hidden for clerks.

---

## Why this screen exists

This is how stock **comes in** with a supplier bill: who you bought from, what, at what rate, how much you paid today, GST (if enabled), later payments, returns to the supplier, and cancelling a mistaken bill.

Saving a purchase **increases** inventory for each line. A return or cancellation **decreases** it again (cancellation only if there are no returns).

If you only counted opening stock on Products and never buy, you may rarely open this page. Most shops use it every time a lorry arrives.

---

## Before you start

- Products exist (you cannot type a free-text item name on a bill).
- Know the supplier, quantities, and **purchase** rates (not selling rates).
- If GST is on: set the supplier’s **State code** when they are in another state (so IGST is used). Blank state = local tax.

---

## Layout (top of the page)

**Left:** quick supplier create + searchable supplier chips.  
**Right:** **Create purchase** form.

**Below:** **Purchases history** and, after you click **View details**, the full bill (PDF, pay, return, cancel).

If business is missing: **Finish business setup before recording purchases.**

---

## Quick supplier setup (left)

Same fields as [Suppliers](suppliers.md): name (required), contact person, mobile, address, state code.

Click **Create supplier**. That supplier is **selected** for the purchase form immediately. Message: **Supplier created.**

**Search suppliers** filters the chips. Click a chip to select that supplier for the new bill. Selected chip is highlighted.

---

## Create purchase (right)

This records **new** stock in. It does not edit an old bill’s lines (you can only change date/notes later, or cancel / return).

### Header fields

| Field | Required | What to type / choose |
|-------|----------|------------------------|
| **Supplier** | Yes | Click the box, type part of the name, click the match. Or click a chip on the left. Create them first if **No matches**. |
| **Purchase date** | Yes | Date the stock was bought or received. Defaults to **today**. Use the calendar. |
| Tax note | Only if GST is on | Read-only: **Intrastate (CGST/SGST)** or **Interstate (IGST)** from supplier vs shop state. Change it by editing the supplier’s **State code**. |
| **Amount paid now** | No (blank = 0) | Money you paid the supplier **with this bill**. `0` or blank = full credit (status **PENDING**). Pay the full total for **PAID**. Anything in between is **PARTIAL**. |
| **Notes** | No | Max 255 characters. Example: `Restocking cement`, lorry number, supplier bill number. |

### What did you buy? (lines)

Click **Add item** for each product on the supplier bill.

| Field | Required | What to type / choose |
|-------|----------|------------------------|
| **Product** | Yes | Search by name or SKU, then click it. Cost price from the catalog is copied into **Price per unit** — change it if this bill’s rate is different. GST % is copied from the product. |
| **Quantity** | Yes | How many **units** arrived (the product’s unit, e.g. Bags). Must be &gt; 0. Decimals allowed. Placeholder `e.g. 50`. |
| **Price per unit** | Yes | What **you pay** for one unit on this bill. Placeholder `e.g. 300`. Line total = quantity × price (GST rules then apply if GST is on). |
| **Line total** | (shown) | Calculated. |
| **Remove** | | Deletes that line before you save. |

You need **at least one** complete line. Incomplete lines (missing product, quantity, or price) are rejected: **Each item needs a product, quantity, and purchase price.**

### Totals before save

- **Total purchase amount** — what the bill is worth (including tax if GST exclusive, or as configured)
- **Paid now** — the amount you typed
- **Outstanding after save** — total minus paid now (not below zero)

Click **Record purchase**. While working: **Recording purchase...**

Success: **Purchase recorded. Stock increased for the products you bought.** The bill number looks like `PUR/2025-26/000001`. Each product’s **cost price** is updated to a weighted average of stock you already had and this bill’s rate (so inventory value is not stuck on the old cost). The new bill is selected below. The create form resets to today for the next lorry.

---

## Purchases history

**Search purchases** — purchase number, supplier name, or notes.

Each card: purchase number, supplier, date, status or **Cancelled**, item count, total, returned (if any), net, paid, due.

**View details** opens the panel on the right/below.

---

## Purchase details (after View details)

Shows purchase number, total, returned, net, paid, outstanding, payment status, and each line: bought / returned / still returnable / price.

### Download bill / Print bill

| Button | What it does |
|--------|----------------|
| **Download bill** | Saves a PDF (shop logo and GSTIN if configured). |
| **Print bill** | Opens the PDF in a print dialog. |

### Save details (date and notes only)

You can change **Purchase date** and **Notes**, then **Save details**. You **cannot** change lines, supplier, or prices here. Wrong items → **Cancel purchase** (if no returns) or **Record purchase return**.

### Record payment (if Due &gt; 0)

When outstanding is zero you see **Fully paid**.

Otherwise:

| Field | What to type |
|-------|----------------|
| **Payment date** | Defaults to today. |
| **Amount** | How much you are paying **now**. Must be &gt; 0 and **cannot exceed** outstanding. The box is pre-filled with the full due amount — lower it for a part payment. |
| **Notes** | Optional. Example: `Cash / UPI / bank transfer`, cheque number. |

Click **Record payment**. Status becomes PARTIAL or PAID. **Payment history** lists earlier payments with date, notes, and amount. If this bill is later **cancelled**, a **Refund** line appears for what had been paid.

### Record purchase return (if any quantity is still returnable)

Use this when you send goods **back to the supplier**. Stock **goes down**. Net bill and dues reduce.

| Field | What to type |
|-------|----------------|
| **Return date** | Defaults to today. |
| **Reason** | Example: `Damaged bags`. Max 255. |
| **Notes** | Example: `Sent unused stock back to supplier`. |
| **Quantity** per line | Only lines with remaining quantity appear. Type how many to return, up to **Returnable**. Leave blank/`0` on lines you are not returning. At least one line must have quantity &gt; 0. |

**Return amount** shows a live rupee total. Click **Record purchase return**. Return numbers look like `PRT/2025-26/000001`. The credit includes tax, same as sales.

Earlier returns appear under **Returns on this purchase** with return number and quantities.

If everything is already returned: **Nothing left to return**.

### Cancel purchase

Use when the **whole bill was a mistake** (duplicate, never received) and you have **not** recorded returns.

| Field | What to type |
|-------|----------------|
| **Cancellation reason** | Required. Example: `Duplicate bill from supplier`. Max 255. |

Click **Cancel purchase**. Stock is reversed. Money already recorded is written as a **Refund** in payment history. The bill stays in history as **Cancelled**.

If the purchase **has returns**, cancel is disabled: **This purchase has returns, so it cannot be cancelled. Record another purchase return instead if needed.**

Cancelled bills show the reason and no pay/return form.

---

## Payment status (what the chip means)

| Status | Meaning |
|--------|---------|
| **PENDING** | Paid ₹0 so far |
| **PARTIAL** | Some paid, some still due |
| **PAID** | Paid covers the **net** amount (after returns) |
| **Cancelled** | Bill voided; stock reversed |

---

## GST on this screen

If GST is off, you will not see the tax note; totals are quantity × price.

If GST is on:

- Each product’s **GST %** from the catalog is used
- Local vs interstate follows the supplier’s **State code** vs the shop (no tick box)
- **Prices include GST** (Settings/Business) changes whether tax is inside the rate or added on top

---

## Common mistakes

- Using **selling** price as purchase price — you will overstate stock value and dues.
- Forgetting **Amount paid now** when you already paid cash — the supplier will show as outstanding until you **Record payment**.
- Adjusting inventory instead of a purchase — you lose the supplier bill.
- Cancelling after a return — not allowed; return the rest or live with the bill.
- Wrong **State code** on the supplier — tax split on the PDF will be IGST instead of CGST/SGST (or the other way around).

---

## Related

- Parties: [Suppliers](suppliers.md)
- Stock effect: [Inventory](inventory.md)
- Buy-side Excel: [Reports](reports.md)
- Mirror of this flow for customers: [Sales](sales.md)
