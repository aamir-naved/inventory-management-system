# Products

**Menu:** Products · **Address:** `/products`  
**Who:** Owner and manager can create, edit, archive, and import. Clerks can **open the page and browse** the catalog (no create form). Clerks should not use Archive, Restore, or Excel; those actions are refused by the server.

---

## Why this screen exists

Nothing else works without a catalog. A **product** is one sellable/buyable item: name, unit, prices, stock starting point, barcode, HSN, and GST %.

Purchases add quantity. Sales subtract quantity. This screen is where you **define** the item, not where you record a lorry or a counter bill.

---

## Before you start

- Business profile must exist.
- Decide the **unit** you will always use (do not mix bags and kg on the same product).
- If you use a scanner, have the barcode number ready.
- If GST is on, know the HSN and GST % for the item.

---

## Layout

**Left (owners/managers):** **Create product** or **Edit product** form.  
**Right:** **Product catalog** — search, Excel, archive toggle, list.

If the business is missing: **Finish business setup before adding products.**

---

## Create a product (left form)

The heading is **Create product** until you click **Edit** on a card.

Opening stock note on the form: **Opening stock becomes the initial current stock on creation.**

### Fields

| Field | Required | What to type / choose |
|-------|----------|------------------------|
| **Product name** | Yes | Display name on catalog, bills, and stock. Max 150 characters. Example: `Ultra Cement`. |
| **SKU** | No | Your internal code, max 60 characters. Example: `CEM-001`. Must be **unique** in this shop if set. Excellent for Excel updates and search. |
| **Barcode** | No | EAN/UPC or shop barcode, max 64 characters. Example: `8901234567890`. Must be **unique** if set. Used on **Counter**: scan or type and press Enter. |
| **Category** | No | Free-text group, max 100 characters. Example: `Cement`. Helps search; there is no separate category master. |
| **Unit** | Yes | How you count it. Dropdown: Pieces, Bags, Kg, Grams, Liters, Ml, Boxes, Packs, Tons, Meters, Rolls, Dozen. Choose **Other** and type a custom unit (max 30 characters) if needed. |
| **Opening stock** | No (blank = 0) | Quantity already in the shop. Decimals allowed (step 0.001). Example: `120`. On **create**, this becomes **current stock**. |
| **Low stock threshold** | No (blank = 0, or Settings default) | When current stock is at or below this, the item is **low stock**. Example: `10`. New forms may pre-fill Settings’ default. |
| **Cost price** | Typically fill | What you usually pay **per unit**. Example: `300`. Used for inventory **value**. |
| **Selling price** | Typically fill | What you usually charge **per unit**. Example: `350`. Counter and Sales copy this; you can still change the price on a bill. |
| **HSN** | No | HSN/SAC, max 8 characters. Example: `2523`. Printed on GST invoices. |
| **GST %** | Yes (dropdown) | `0%`, `5%`, `12%`, `18%`, or `28%`. Used when GST is enabled. Pick `0%` for exempt items. |

Click **Create product**. Success: **Product created.** The form stays on that product so you can tweak it.

### Edit an existing product

1. On the right, click **Edit** on a card.
2. Left heading becomes **Edit product**.
3. Change fields and click **Save product**.
4. Click **New product** to clear the form and add another item.

**Opening stock on edit:** if you change opening stock, **current stock moves by the same difference**. Example: opening was 100, current is 80 (you sold 20). You change opening to 110 → current becomes 90. Prefer **Inventory → Adjust stock** for damage and physical counts, not this field, unless you are correcting the original opening figure.

**SKU and barcode** must stay unique. Changing them to a value another product already uses will fail.

---

## Catalog (right side)

### Search products

Type part of **name, SKU, or category**. The list filters as you type.

### Show archived

Off (default): only active products.  
On: archived products appear with an **Archived** chip.

### Download Excel

Downloads `products.xlsx` with the current catalog (active items). Use this as a **template** or as a backup list for the accountant.

### Upload Excel

Opens a file picker. Choose an `.xlsx` file. The app creates new rows and **updates** existing ones.

Success message example: **Excel import finished: 12 added, 3 updated.**

If rows fail, **Rows that need a fix** lists the first 8 problems (`Row 4: Name is required`). Fix the file and upload again. Good rows from the same file are already saved.

#### Excel columns (first row = headers)

Use these headers (other common aliases like `Price` or `Qty` also work):

| Column | Meaning |
|--------|---------|
| Name | Required |
| SKU | Optional; unique; used to match an existing product to **update** |
| Category | Optional |
| Unit | Required |
| Cost price | Number |
| Selling price | Number |
| Opening stock | Number |
| Low stock threshold | Number; empty uses Settings default |
| Barcode | Optional, unique |
| HSN | Optional |
| GST % | 0, 5, 12, 18, or 28 |

Rules:

- Maximum **2000** data rows.
- If **SKU** is present, that SKU identifies the product. Duplicate SKUs **inside the file** are rejected.
- If there is no SKU, the app matches by **name**. Two products with the same name and no SKU cannot be updated safely — add SKUs.
- You cannot update an **archived** product via Excel (restore it first).
- Only `.xlsx` (Excel workbook), not `.xls` or CSV.

**Recommended:** Download Excel, keep the header row, fill or edit rows, save, Upload Excel.

### Product cards

Each card shows name, category, unit, **Active** or **Archived**, SKU, cost, selling, current stock, low-stock level.

| Button | What it does |
|--------|----------------|
| **Edit** | Loads the product into the left form (owners/managers). |
| **Archive** | Hides it from the default list and from normal pickers. History remains. Use when you stopped stocking it. |
| **Restore** | Shown when archived. Puts it back on the default list. |

Empty list: **No products yet** — create one or upload Excel.

---

## When to choose what

| Situation | What to do |
|-----------|------------|
| Brand-new shop, stock already counted | Put counts in **Opening stock**. |
| Empty shop, waiting for purchase bill | Opening stock `0`, then [Purchases](purchases.md). |
| 200 items from an old spreadsheet | Download Excel, paste columns, Upload. |
| Same cement, two bag sizes | Two products, different names/SKUs/units. |
| Seasonal item, might return next year | **Archive**, do not invent a new name later if you can Restore. |
| Barcode on the bag | Fill **Barcode** exactly as the scanner types it (no spaces unless they are on the label). |
| GST invoice needs HSN | Fill **HSN** and the correct **GST %**. |

---

## Common mistakes

- **Two products, same barcode** — Counter cannot tell them apart; uniqueness is enforced.
- **Changing unit after you already bought/sold** — old bills stay in the old unit. Avoid.
- **Using opening stock every week to “fix” counts** — use [Inventory](inventory.md) adjustments so history is clear.
- **Selling price 0** — allowed as a number, but bills will be free. Type the real rate.
- **Clerk expecting to add a product** — ask an owner/manager; the create form is hidden.

---

## Related

- Stock after sales/purchases: [Inventory](inventory.md)
- Scanning: [Counter](counter.md)
- First-day catalog: [First day](first-day.md)
