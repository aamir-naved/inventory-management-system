Core stock + buy/sell loop is largely done. Per the MVP backlog/roadmap, the remaining **P0** work to finish Phase 1 looks like this:

1. ~~**Payments (Epic 9)**~~ — **Done.** Payment ledger on sales/purchases; full/partial payments; amount paid vs outstanding; status derived from amounts (sale outstanding uses net after returns). UI lives on Sales/Purchases detail panels.

2. ~~**Dashboard with live metrics (Epic 10)**~~ — **Done.** `GET /dashboard/metrics` feeds live cards: today’s sales/purchases, lifetime revenue (net after returns), product count, inventory value, low stock, outstanding customers/suppliers.

3. ~~**Reports (Epic 11)**~~ — **Done.** Inventory, purchases, sales, customer outstanding, supplier outstanding via `GET /reports/*` and the Reports page. PDF/Excel remains later.

4. ~~**Customer & supplier outstanding + history**~~ — **Done.** Dedicated Customers/Suppliers pages with CRUD/search; `GET /customers/{id}/summary|sales` and `GET /suppliers/{id}/summary|purchases` for outstanding (sale net after returns) and document history. Quick-create still on Sales/Purchases.

5. ~~**Auth / onboarding hardening**~~ — **Done.** Soft email verification (register unverified + resend banner); forgot/reset password via mail abstraction (logs links without SMTP); refresh tokens with rotation/logout revoke; profile page for name/password. Access TTL 1h, refresh 14d.

6. ~~**Settings (Epic 12, P1)**~~ — **Done.** Negative-stock toggle (enforced on sales/adjustments/purchase cancel), default low-stock threshold for new products, currency + date format via `GET/PUT /settings` and Settings page; money/dates use business prefs across the app.

7. ~~**Purchase returns**~~ — **Done.** Nested under purchases like sale returns: partial/full returns, stock out via `PURCHASE_RETURN` (respects negative-stock setting), net billable for payments/status, cancel blocked when returns exist. Dashboard, reports, and supplier outstanding use net after returns. UI on Purchases detail panel.

**Suggested order:** ~~Payments~~ → ~~Dashboard~~ → ~~Reports~~ → ~~Customer/Supplier outstanding views~~ → ~~Auth hardening~~ → ~~Settings~~ → ~~Purchase returns~~. Phase 2 (barcode, invoice PDF, Excel, employees) should wait until that MVP slice is solid.
