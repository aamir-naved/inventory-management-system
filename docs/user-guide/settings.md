# Settings

**Menu:** Settings · **Address:** `/settings`  
**Who:** Owner and manager. Hidden for clerks.

---

## Why this screen exists

**Business** holds the shop identity (name, address, logo). **Settings** holds **how the workspace behaves**: currency display, date layout, whether you can sell below zero stock, the default reorder level for **new** products, and the same GST switches.

If you have not created a business yet, this page only shows **Complete business setup**.

---

## When to open it

- You want dates as `31/12/2026` vs `2026-12-31`
- New products should start with a reorder level of `10` instead of `0`
- You must bill even when stock is not yet entered (negative stock)
- You turned GST on here or want inclusive pricing without reopening Business

---

## Fields

| Field | What to choose | Effect |
|-------|----------------|--------|
| **Currency** | 3-letter code, uppercase as you type (`INR`). Preview shows how `1234.5` will look. | Money on Dashboard, bills, reports. |
| **Date format** | Dropdown: `dd/MM/yyyy` (31/12/2026), `MM/dd/yyyy` (12/31/2026), `yyyy-MM-dd` (2026-12-31), `dd-MMM-yyyy` (31-Dec-2026). Preview uses 31 Dec 2026. | How dates appear on sales, purchases, invoices, reports. |
| **Default low-stock threshold** | Number, 0 or more, decimals allowed. Placeholder `0`. | Copied onto **new** products when you open the create form. Changing this does **not** rewrite existing products. |
| **Allow negative stock on sales and adjustments** | Checkbox, **off** by default. | **Off:** a sale or negative adjustment is refused if it would take quantity below zero. **On:** you can sell or write off more than you have; stock can go negative. Turn **on** only if you often bill first and enter the purchase later. |
| **Enable GST on invoices** | Checkbox. | Same as Business: tax lines on sales/purchases, GST report, tax on PDFs. |
| **GSTIN / State code / State** | Shown when GST is on. Same meaning as [Business](business.md). | Printed and used for tax split. |
| **Selling and purchase prices include GST** | Shown when GST is on. | Same as **Prices include GST** on Business. |

Click **Save settings**. Success: **Settings saved.**

---

## Backup this PC (Windows desktop only)

If you installed the Windows app, Settings also shows **Backup this PC**. Cloud/browser installs do not.

- **Export backup** downloads a `.sql.gz` file. Keep it off this computer.
- **Queue restore** picks that file. Then **close the app and open it again** so the backup replaces local data.

Details: [Windows desktop](../10-windows-desktop.md).

---

## When to choose what

**Date format:** Indian shops usually keep `dd/MM/yyyy`.

**Negative stock off (recommended):** stock numbers stay honest. If a sale fails, enter the purchase or fix the count first.

**Negative stock on:** use when the lorry is unloading and the customer is already at the counter. Reconcile the same day with a purchase or inventory adjustment.

**Inclusive GST:** see [Business](business.md). Keep Business and Settings consistent; saving either updates the shop.

---

## Related

- Shop name and logo: [Business](business.md)
- Reorder warnings: [Inventory](inventory.md) and Alerts in [Around the workspace](workspace.md)
