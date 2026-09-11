# Dashboard

**Menu:** Dashboard · **Address:** `/dashboard`  
**Who:** Everyone (owner, manager, clerk). Home after login is **Counter**. Visiting `/` redirects there. Open Dashboard when you want totals.

---

## Why this screen exists

It answers, without opening five other pages:

- What did we sell and buy **today**?
- How much have we sold in total (after returns)?
- Is stock healthy?
- Who owes us, and whom do we owe?

It is a **read-only pulse**. You do not type invoices here.

---

## If you see “Business setup is the next required step”

Click **Complete business setup** and finish [Business](business.md). Cards will show zeros or stay empty until then.

---

## The eight cards

Amounts use the currency from Settings (usually rupees).

| Card | What the number is | What to do if it looks wrong |
|------|--------------------|------------------------------|
| **Today’s sales** | Net sales amount **for today** (shop time zone). | Record sales on **Sales** or **Counter**. Returns today reduce this. |
| **Today’s purchases** | Purchase total **for today**. | Record purchases on **Purchases**. |
| **Total revenue** | **Lifetime** net sales after returns (not only today). | Historical; it grows as you sell. |
| **Total products** | Count of **active** catalog items (not archived). | Add items on **Products**. |
| **Inventory value** | **Weighted average cost × current quantity** for stock. | Cost mixes in each purchase. Opening stock uses the cost you typed on the product. Sales reduce quantity, not the cost rate. |
| **Low stock** | How many products are at or **below** their threshold. | Open **Inventory**, tick **Low stock only**, then purchase or adjust. |
| **Outstanding customers** | Sum of unpaid customer balances. | **Sales** or **Customers** to collect. |
| **Outstanding suppliers** | Sum of unpaid supplier balances. | **Purchases** to pay. |

While loading: **Loading dashboard metrics…**  
If it fails: **Could not load dashboard metrics. Refresh to try again.**

---

## What “today” means

**Today** follows the business **time zone** (default `Asia/Kolkata`). A sale dated yesterday does not sit in **Today’s sales**.

---

## Related

- Sell: [Counter](counter.md) or [Sales](sales.md)
- Buy: [Purchases](purchases.md)
- Stock list: [Inventory](inventory.md)
- Printed summaries: [Reports](reports.md)
