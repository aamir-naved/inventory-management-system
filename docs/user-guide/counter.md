# Counter

**Menu:** Counter · **Address:** `/pos`  
**Who:** Owner, manager, and clerk. After accepting an invite, staff land here.

---

## Why this screen exists

The **billing desk**. One hand on the scanner, the other on cash/UPI. It creates a real **sale** (same records as the Sales page) with today’s date and notes `POS sale`. After you save, **Share bill** and **Print** appear so the customer can get the PDF on the phone.

It is not a hardware driver. A USB barcode scanner that types digits and sends **Enter** is enough. Starter items from shop naming use the SKU as the barcode (`CEM-001` for Cement).

If the shop is not named yet, you are sent to **Start**.

Menu labels and Counter buttons can be **English** or **हिन्दी** (sidebar **Language**).

---

## Before each session

1. Products have **Barcode** filled (exactly what the scanner types), or you type a starter SKU.
2. **Walk-in** is selected automatically when that customer exists.
3. Printer is on, if you print.
4. Click the **Barcode** box so it is focused (it focuses itself when the page opens).
5. The phone or PC must be **online**. If you see **You are offline**, reconnect before billing.

---

## What to do (typical cash sale)

1. **Customer** — **Walk-in** is chosen for you. Change it only for a named party. The list shows up to **100** customers. If someone is missing, add them on [Customers](customers.md) and come back.
2. **Barcode** — scan, or type the number and press **Enter**. The product is added with quantity **1**. Scan the same barcode again to add 1 more. Wrong scan: **No product for that barcode** (or an API error) — check Products. First-run hint: type `CEM-001` if you have no scanner.
3. Review the list. **Remove** deletes that product from this ticket (not from the catalog).
4. **Amount paid**
   - **Leave blank** if they paid the **full total** (the app uses the full total).
   - Type a **smaller** number for part payment / credit.
   - Type `0` only if they pay nothing now (credit).
5. Click **Complete sale**.
6. Click **Share bill** so the customer can keep the PDF (phone share sheet, or a download plus WhatsApp). Click **Print** if you have a printer.

Success: **Saved SAL/2025-26/000001.** (The year follows the Indian financial year.) Lines clear for the next customer. Share/Print stay on the last sale until you make another.

If GST is on, a note under the customer shows **Intrastate (CGST/SGST)** or **Interstate (IGST)**. That is decided from the customer’s **State code** vs the shop’s state. There is **no tick box**. Walk-in with a blank state is treated as local. For an out-of-state named party, set their state on [Customers](customers.md) before you bill.

If share or print fails: open [Sales](sales.md), find that number, **Share bill** or **Print invoice**.

---

## Fields and buttons (complete)

| Control | What it is for |
|---------|----------------|
| **Barcode** | Scan or type; **Enter** looks up the product. Box clears after a successful add. |
| **Customer** | Required. **Walk-in** is selected when that party exists. |
| Tax note | Shown when GST is on. Local vs interstate is derived; you cannot override it here. |
| Line list | Name, quantity × selling price. **Remove** drops the line. Quantity only goes up by scanning again; there is **no quantity box**. For 7 bags, scan seven times **or** use **Sales** and type `7`. |
| **Taxable / Tax** | Shown when GST is on. |
| **Total** | What the invoice is worth. |
| **Amount paid** | See above. Placeholder shows the current total. |
| **Complete sale** | Saves. **Saving...** while it works. Disabled while offline. |
| **Share bill** | After a save: phone share sheet with the PDF, or download + WhatsApp. |
| **Print** | After a save: opens the PDF for printing. |

You cannot complete with zero lines: **Scan or add at least one product**.  
You cannot complete with no customer: **Choose a customer**.  
You cannot complete while offline: reconnect first.

If the network blips after you tap Complete sale, wait and check **Sales** before tapping again. A retry of the **same** ticket will not create a second invoice.

---

## What Counter does **not** do

- Search product **by name** (use Sales)
- Change selling price on the ticket (catalog price only)
- Sale returns, extra payments, cancel (use Sales after the fact)
- Purchase / stock adjustment
- Pick from more than the first 100 customers in this dropdown — add/search on Customers, or use Sales search-and-select
- Queue a bill on paper while the internet is down — Complete sale needs the server

---

## After the sale

- Stock is reduced
- **Dashboard** today’s sales, inventory, and alerts refresh without a manual reload
- The invoice appears on **Sales** with notes `POS sale`
- If amount paid was less than total, status is **PARTIAL** or **PENDING** — collect the rest on Sales → **Record payment**

---

## Related

- Full invoice: [Sales](sales.md)
- Barcodes: [Products](products.md)
- Walk-in party: [Customers](customers.md)
- First bill: [First day](first-day.md)
