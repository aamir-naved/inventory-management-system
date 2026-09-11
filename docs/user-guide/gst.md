# GST on invoices (what to type, which state)

This is not a separate menu item. GST is a **shop setting** plus fields on **products**, **customers**, **suppliers**, **sales**, **purchases**, **Counter**, and the **GST** report.

Use this page when you are GST-registered (or about to be) and you want invoices to show the right tax.

If you are **not** registered and you do not charge GST, leave **Enable GST** off. You can still sell and buy; PDFs will not show GSTIN or tax splits.

---

## 1. Turn GST on for the shop

Open **Business** (or **Settings** — the GST boxes are the same shop data).

1. Tick **Enable GST on invoices and bills**.
2. **GSTIN** — 15 characters. Letters are forced to uppercase. Example: `29ABCDE1234F1Z5`.
3. **State code** — 2 characters, usually the first two digits of GSTIN (`29` = Karnataka, `27` = Maharashtra, `07` = Delhi, `33` = Tamil Nadu, `24` = Gujarat). This is the shop’s place of supply.
4. **State** — full name (`Karnataka`).
5. **Prices include GST** — read the next section before ticking.
6. **Save**.
7. Upload a **logo** on Business if you want it on the PDF.

Until this is on, Sales / Purchases / Counter hide the tax note, and tax columns stay at zero.

---

## 2. Inclusive vs exclusive prices (choose once)

| Your habit | What to tick | What you type on a bill |
|------------|--------------|-------------------------|
| Rack rate is ₹118 and that **already includes** 18% | **Prices include GST** = on | Type `118`. The app splits taxable + tax inside that amount. |
| Rack rate is ₹100 and you **add** 18% on the invoice | Leave **Prices include GST** off | Type `100`. The app adds tax. Line total becomes about ₹118. |

Pick one way for the shop and stick to it. Changing the tick does **not** rewrite old invoices.

---

## 3. Put HSN and GST % on each product

**Products** → each item:

| Field | What to choose |
|-------|----------------|
| **HSN** | Optional but needed for a proper GST invoice. Up to 8 characters. Example: `2523`. |
| **GST %** | `0`, `5`, `12`, `18`, or `28`. Use `0` for exempt / non-GST items. |

Sales, purchases, and Counter **copy this %** onto the line. You do not type a different GST % on the bill in the UI.

If you imported Excel, the **GST %** column must be one of those five rates.

---

## 4. Local vs interstate (every bill)

Your shop state is **State code** from step 1.

There is **no Interstate tick** on Counter, Sales, or Purchases. Tax follows **place of supply**:

| Party **State code** | Tax on the bill |
|----------------------|-----------------|
| Same as the shop, or **blank** | **CGST + SGST** (intrastate). Blank is treated as local. |
| Different from the shop | **IGST** only |

Set **State code** on the [customer](customers.md) or [supplier](suppliers.md) (2 digits, example `27`). Counter and Sales then show a note such as **Interstate (IGST) — customer state differs from business state.**

**Walk-in** cash sales usually have no state → local tax. For an out-of-state named party, fill their state **before** you bill.

Wrong state code = wrong tax on the PDF and in the GST report. You cannot edit the tax split after save. For a fully wrong invoice with no returns, owner/manager can **Cancel sale** / **Cancel purchase** and enter it again.

Invoice numbers are consecutive per shop per Indian financial year (1 April–31 March), for example `SAL/2025-26/000001`.

---

## 5. What prints on the PDF

**Print invoice** / **Download invoice** (Sales) and **Print bill** / **Download bill** (Purchases) include, when GST is on:

- Shop name, address, mobile
- GSTIN
- Logo (if uploaded)
- HSN and tax amounts from the lines
- Product **name and unit as they were on the day of the bill** (renaming the product later does not rewrite old PDFs)

After **Complete sale** on Counter, use **Share bill** or **Print** on that ticket (it does not auto-print).

---

## 6. GST report for the CA

**Reports** → **GST** tab.

1. Set **From date** and **To date** (example: first and last day of the month).
2. Read the three cards:
   - **Output tax** — sales minus sale returns
   - **Input tax** — purchases minus purchase returns
   - **Net tax** — output minus input (what you still owe, in this working paper)
3. Each row is a document: type, number, party, date, rate, CGST, SGST, IGST. Returns appear as negative rows.
4. **Download Excel**.

This file is a working paper. It does **not** file GSTR-1, GSTR-3B, or e-invoice IRN, and it does not create e-way bills.

Returns credit the **full line including tax**. A tax-exclusive ₹100 + 18% line that is fully returned credits ₹118, not ₹100.

---

## 7. Quick checklist before the first GST invoice

- [ ] GST enabled, GSTIN and shop state saved
- [ ] Inclusive/exclusive choice matches how you quote rates
- [ ] Products have HSN and the right GST %
- [ ] Out-of-state customers and suppliers have **State code** filled
- [ ] Print a test invoice and read the tax lines before giving it to a customer

---

## Related screens

- [Business](business.md) · [Settings](settings.md) · [Products](products.md)
- [Sales](sales.md) · [Purchases](purchases.md) · [Counter](counter.md)
- [Customers](customers.md) · [Suppliers](suppliers.md)
- [Reports](reports.md)
