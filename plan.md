Core stock + buy/sell loop is largely done. Per the MVP backlog/roadmap, the remaining **P0** work to finish Phase 1 looks like this:

1. ~~**Payments (Epic 9)**~~ — **Done.** Payment ledger on sales/purchases; full/partial payments; amount paid vs outstanding; status derived from amounts (sale outstanding uses net after returns). UI lives on Sales/Purchases detail panels.

2. **Dashboard with live metrics (Epic 10)** — Replace the placeholder `/dashboard` with real numbers: today’s sales/purchases, revenue, product count, inventory value, low stock, outstanding customers/suppliers. Depends heavily on payments for the outstanding cards.

3. **Reports (Epic 11)** — Inventory, purchases, sales, customer outstanding, supplier outstanding. Keep v1 on-screen/API first; PDF/Excel is explicitly later.

4. **Customer & supplier outstanding + history** — Backlog calls these out under Epics 5/6. Wire dedicated views (or stronger panels) for balances and purchase/sale history; today customers/suppliers are mostly embedded in Sales/Purchases.

5. **Auth / onboarding hardening** — Still open in Epic 1: forgot/reset password, email verification, refresh tokens, profile. Less “daily ops,” but needed before real users.

6. **Settings (Epic 12, P1)** — Negative-stock toggle, low-stock defaults, currency/date format. MVP already mentions “negative stock unless allowed in settings.”

7. **Purchase returns** — Not a separate backlog epic, but the natural mirror of sale returns if you want buy-side parity before Phase 2 (invoice/PDF/barcode/employees).

**Suggested order:** ~~Payments~~ → Dashboard → Reports → Customer/Supplier outstanding views → Auth hardening → Settings. Phase 2 (barcode, invoice PDF, Excel, employees) should wait until that MVP slice is solid.
