# Inventory

**Menu:** Inventory · **Address:** `/inventory`  
**Who:** Everyone can **view** stock and movement history. **Adjust stock** is for owner and manager only. Clerks see **View history** instead of Adjust.

---

## Why this screen exists

**Products** defines the item. **Inventory** is the live **quantity, value, and story** of that quantity.

Stock moves automatically when you:

- Create a product with opening stock
- Record a **purchase** (up) or **purchase return** (down)
- Record a **sale** (down) or **sale return** (up)
- **Cancel** a sale or purchase (reverses the stock effect)

Use **manual adjustment** only when none of those fit: theft, breakage, physical count mismatch, samples given away.

---

## Before you start

- Products must exist.
- For a count correction, stand in front of the pile and know the **difference** (found 5 extra bags → `+5`; 2 damaged → `-2`).

---

## Top cards

| Card | Meaning |
|------|---------|
| **Total products** | Active items in stock tracking |
| **Current stock value** | Cost price × current quantity |
| **Low stock alerts** | How many items are at or below threshold |

---

## Stock overview (left)

### Search stock

Type product name, SKU, or category.

### Low stock only

Tick to hide healthy items. Use this when Alerts said something is low.

### Show archived

Tick to include archived products in the stock list.

### Each stock card

- Name, category, unit
- Chip: **Low stock** (warning) or **Healthy**
- Current stock, threshold, value, SKU

| Button | Who | What it does |
|--------|-----|----------------|
| **Adjust stock** | Owner / manager | Selects the item for the right-hand form |
| **View history** | Clerk | Selects the item to read movements only |

---

## Manual stock adjustment (right, owner/manager)

Until you select a product: **No product selected**.

After you select one:

- Product name and **Current stock** with unit
- Form to add or remove quantity
- **Stock history** underneath

### Fields

| Field | Required | What to type |
|-------|----------|----------------|
| **Adjustment quantity** | Yes, not zero | **Positive** adds stock (found extra, unboxed goods). **Negative** removes stock (damage, missing). Example: `5` or `-2`. Decimals allowed. Cannot be blank or `0`. |
| **Reason** | Yes | Why this is **not** a purchase or sale. Stored forever; you cannot edit it later. Example: `Physical count correction`, `Damaged in rain`, `Shop sample`. Max 255 characters. |

Click **Apply adjustment**. Success: **Stock adjusted successfully.** Quantity and history refresh. The quantity box clears so you do not double-submit.

If **Allow negative stock** is off in Settings, a negative adjustment that would go below zero is refused.

### Stock history

Each movement shows:

- Type (words like `SALE`, `PURCHASE`, `ADJUSTMENT`, opening stock, returns — displayed with spaces)
- Date and time
- Notes (your reason, or notes from the bill)
- **Change**, **Before**, **After** quantities

This is the audit trail for that product. Clerks use this to answer “why is bag count different from yesterday?” without changing numbers.

---

## When to choose what

| Situation | Use |
|-----------|-----|
| Goods arrived from supplier | [Purchases](purchases.md), not an adjustment |
| Customer bought goods | [Sales](sales.md) or [Counter](counter.md) |
| Customer returned goods | Sale return on [Sales](sales.md) |
| You sent goods back to supplier | Purchase return on [Purchases](purchases.md) |
| Counted 3 extra bags in the godown | Adjustment `+3` with reason `Physical count` |
| Two bags wet and dumped | Adjustment `-2` with reason `Damaged` |
| Wrong opening stock on day one | You may edit opening stock on [Products](products.md) (this shifts current stock by the difference) **or** adjust here. Prefer adjustment if trading has already started. |

Do **not** use adjustments to “record a purchase without a supplier”. You will lose dues, GST, and supplier history.

---

## Related

- Catalog: [Products](products.md)
- Low stock Excel: [Reports](reports.md) → Inventory tab
- Alerts: [Around the workspace](workspace.md)
