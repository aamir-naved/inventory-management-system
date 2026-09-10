# Market Readiness Roadmap

**Source:** [production-readiness-audit](../.cursor/projects/Users-aamirnaved-inventory-management-system/canvases/production-readiness-audit.canvas.tsx) · Sep 10, 2026  
**Goal:** Ship a product that is safe with a real shop's money, stock, and GST filings.  
**Rule:** Finish one step, update this doc and the audit canvas, then start the next. Do not skip gates.

---

## Status snapshot

| Gate | Goal | Status |
|------|------|--------|
| Gate 1 | Stop producing wrong numbers | **Done** (2026-09-10) |
| Gate 2 | Survive the internet and a bad night | **Done** (2026-09-10) |
| Gate 3 | Earn a second shop | **Done** (2026-09-10) |

**Current step:** Gate 3 complete — ready for second-shop polish follow-ups outside this roadmap  
**Last completed:** G3-S7 — Stock cache invalidation; i18n; route code splitting (2026-09-10)  
**Last updated:** 2026-09-10

---

## Gate 1 — Stop producing wrong numbers

Backend-only. No new infrastructure. This is the real gate before any shop uses the product.

### G1-S1 — Concurrency control (findings #1, #2, #3, #4)

**Status:** Done (2026-09-10)

- Added `@Version` on `Product`, `Sale`, and `Purchase` via Flyway `V15__optimistic_locking.sql`.
- Added `PESSIMISTIC_WRITE` `…ForUpdate` repository loads; mutation paths lock products in UUID order.
- Sale creation aggregates quantity per product before validating stock (duplicate lines cannot pass).
- Sale/purchase returns and payment writes lock the parent document row before read-modify-write.
- Same locking applied to sale/purchase cancel, inventory adjust, and product opening-stock updates.
- Tests: duplicate-line stock reject; payment cannot exceed outstanding.

**Done when:** Concurrent clerks cannot oversell, over-return, or overpay; one bill with the same product twice cannot drive stock negative.

### G1-S2 — GST-compliant document numbers (finding #7)

**Status:** Done (2026-09-10)

- Added `document_sequences` table (`V16`) with per-business / per-document-type / per-FY counters.
- `DocumentNumberService` allocates under `PESSIMISTIC_WRITE`, using Indian FY (1 Apr–31 Mar) in the business timezone.
- Format: `SAL/2025-26/000001` (also `PUR`, `RET`, `PRT`).
- Wired into sale, purchase, sale-return, and purchase-return create paths.
- Tests: consecutive sale serials; FY boundary unit tests; return prefix expectations updated.

**Done when:** Invoice numbers satisfy CGST Rule 46(b) consecutive serial per FY.

### G1-S3 — Return credit includes tax (finding #8)

**Status:** Done (2026-09-10)

- `GstCalculator.proportionalLineCredit` credits against the snapshot `lineTotal` (tax included).
- Final return on a line absorbs rounding remainder so credits sum exactly to the original line.
- Wired into sale returns and purchase returns; repository sums prior credited amounts per line item.
- Tests: unit remainder absorption; GST exclusive 18% full return credits ₹1180 not ₹1000.

**Done when:** A tax-exclusive ₹118 line refunds ₹118, not ₹100.

### G1-S4 — GST report correctness (findings #9, #10)

**Status:** Done (2026-09-10)

- GST report now tracks separate output vs input taxable/CGST/SGST/IGST/tax totals.
- Sale returns and purchase returns are included as negative rows and subtracted from the matching side.
- `netTax` = output tax − input tax (no more meaningless summed total).
- Frontend GST tab shows Output / Input / Net.
- Test: exclusive 18% sale+purchase with half return → net tax 0.

**Done when:** An owner can read net GST liability components without inventing spreadsheet math.

### G1-S5 — Snapshot product name and unit on line items (finding #11)

**Status:** Done (2026-09-10)

- Flyway `V17` adds `product_name` + `unit` on sale/purchase/return line items with backfill from products.
- Create paths snapshot at write time; returns copy from the parent line snapshot.
- API responses and invoice PDFs read snapshots, not live product rows.
- Test: rename product after sale → sale still shows original name.

**Done when:** Renaming a product does not rewrite last year's invoices.

### G1-S6 — Business timezone for “today” (finding #13)

**Status:** Done (2026-09-10)

- Added `BusinessClock` resolving `today()` / `zoneId()` from the current business `time_zone`.
- Dashboard “today” sales/purchases and notification overdue cutoff use it.
- Document number FY allocation uses the same clock.
- Test: business set to UTC → dashboard `asOfDate` matches UTC today.

**Done when:** IST sales between 00:00–05:29 on a UTC host land on the correct business day.

### Gate 1 exit criteria

All G1 steps done, related tests green, audit canvas marks findings #1–4, #7–11, #13 resolved.

---

## Gate 2 — Survive the internet and a bad night

Before exposing a public URL.

### G2-S1 — Rate limits (finding #14)

**Status:** Done (2026-09-10)

- `AuthRateLimiter` sliding-window limits on login, OTP request/verify, forgot-password (per IP + per account).
- Returns HTTP 429 with `Retry-After: 60`.
- Unit + controller tests for account lockout after 5 failed logins / minute.

### G2-S2 — OTP / secret logging (finding #15)

**Status:** Done (2026-09-10)

- `SmsGateway` never logs OTP bodies; non-prod keeps in-memory capture for tests only.
- `LoggingMailService` no longer logs email bodies (verify/reset links stay out of logs).
- Prod refuses boot without `APP_SMS_WEBHOOK_URL` / `app.auth.sms-webhook-url`.

### G2-S3 — Frontend survival (findings #27, #28)

**Status:** Done (2026-09-10)

- `AppErrorBoundary` wraps the router so an uncaught render error shows Reload / Go home instead of a white screen.
- Shared `auth-session` refresh + `expireAuthSession()` notifies listeners when refresh fails after a 401.
- `AuthProvider` clears React auth state and React Query cache on expiry; `ProtectedRoute` redirects to login.
- Same expiry path used by `http-client` and `download-client`.

### G2-S4 — Backups and restore (findings #20, #21)

**Status:** Done (2026-09-10)

- `backup.sh` dumps with `--clean`, writes SHA-256, copies to `BACKUP_COPY_DIR`, prunes by `BACKUP_RETENTION_DAYS` (default 14).
- Prod deploy and prod backup runs require `BACKUP_COPY_DIR` (off this VPS disk).
- `install-backup-cron.sh` installs a nightly 02:15 UTC cron entry.
- `restore.sh` confirms, stops API/UI, drops/recreates DB, restores with `ON_ERROR_STOP`, verifies public tables, then starts the app.

### G2-S5 — Uptime monitoring (finding #22)

**Status:** Done (2026-09-10)

- `scripts/uptime-check.sh` probes public `/api/actuator/health`, alerts via SMS webhook and/or generic webhook, recovers with a follow-up SMS.
- GitHub Actions `uptime.yml` runs every 5 minutes on GitHub runners (true off-VPS watchdog) when secrets are set.
- `install-uptime-cron.sh` for a second host; README also recommends UptimeRobot/Better Stack.
- Prod `deploy.sh` requires `UPTIME_ALERT_PHONE`.

### G2-S6 — Runtime hardening (findings #23, #25)

**Status:** Done (2026-09-10)

- Prod overlay sets cgroup memory limits (Postgres 512 m, backend 768 m, frontend/caddy 128 m; overridable).
- Backend image + prod compose set `JAVA_TOOL_OPTIONS` with `MaxRAMPercentage=75` so the heap tracks the container limit.
- All prod services use json-file log rotation (`10m` × 3 files).

### Gate 2 exit criteria

All G2 steps done. Shop can be exposed on a public URL with rate limits, secret hygiene, UI survival, off-server backups, uptime SMS, and bounded runtime resources.

---

## Gate 3 — Earn a second shop

Before selling to anyone you do not know.

### G3-S1 — E2E and counter tests (finding #29)

**Status:** Done (2026-09-10)

- Vitest + Testing Library component tests for `PosPage` (Walk-in, barcode line, complete sale + payment, share bill, no-shop gate).
- Playwright E2E shop loop: register → name shop → counter sale → invoice PDF share → inventory stock 19.000.
- `scripts/e2e.sh`, `make e2e`, and `.github/workflows/e2e.yml` (Compose + Chromium).
- Compose exposes `APP_OPEN_REGISTRATION` so E2E can open signup without editing a closed prod `.env`.

### G3-S2 — Idempotency and offline (findings #30, #31)

**Status:** Done (2026-09-10)

- `Idempotency-Key` header on `POST /sales` and `POST /sales/{id}/payments` with Postgres `idempotency_keys` (claim → process → replay).
- Same key + same body replays `201`; same key + different body → `409`.
- Counter and Sales UI send a stable UUID per attempt; network retries reuse it.
- Counter shows an offline banner and disables Complete sale when `navigator.onLine` is false.

### G3-S3 — Cancel payments and cost (findings #5, #6)

**Status:** Done (2026-09-10)

- Sale/purchase cancel writes a `REFUND` payment for the paid amount and zeroes `amountPaid` (receipt rows kept as trail).
- Purchase create updates product `costPrice` with weighted average: `(qtyBefore×cost + qtyIn×unitCost) / qtyAfter`.
- UI payment history labels refunds; tests cover cancel refund trail and WAC `319.29` after 120@320 + 20@315.

### G3-S4 — Interstate from state (finding #12)

**Status:** Done (2026-09-10)

- Flyway `V20` adds `state_code` on customers and suppliers.
- `GstPlaceOfSupply` derives interstate from party vs business state (missing party state → intrastate).
- Sale/purchase create ignores client `interstate`; CGST/SGST vs IGST follows place of supply.
- POS/sales/purchases UI shows derived tax mode; clerk checkbox removed; party forms collect state code.
- Tests: place-of-supply unit cases; sale with customer state 27 vs business 29 charges IGST even when `interstate:false` is sent.

### G3-S5 — Session revoke + no plaintext temps (findings #16, #17)

**Status:** Done (2026-09-10)

- Flyway `V21` adds `token_version` on `app_users`; JWTs carry `tv` and are rejected when the version no longer matches.
- Logout, password change/reset, user disable, owner password reset, and staff deactivate bump the version and clear refresh tokens.
- Platform create-shop / reset-owner no longer return temporary passwords in JSON; delivery is email-only (`passwordDelivery`).
- Platform UI and docs updated; tests assert access JWT dies after logout/password change and shop create omits the password field.

### G3-S6 — Headers, tenants, rollback, DB password (findings #18, #19, #24, #26)

**Status:** Done (2026-09-10)

- Caddy and nginx send HSTS (edge), CSP, X-Frame-Options, X-Content-Type-Options, Referrer-Policy, Permissions-Policy.
- `TenantAccessAspect` rejects tenant-aware repository results whose `businessId` does not match the active shop (covers bare `findById`).
- Deploy tags `ims-backend` / `ims-frontend` with `IMAGE_TAG`, health-gates, records `.deploy/current`; `scripts/rollback.sh` / `make rollback` restores the previous tag.
- Prod refuses `DB_PASSWORD` / `POSTGRES_PASSWORD` equal to `inventory_password` or shorter than 12 characters (validator + deploy.sh).

### G3-S7 — Cache, i18n, code split (findings #32, #33, #34)

**Status:** Done (2026-09-10)

- POS sale success invalidates sales, products, inventory summary/stock/movements, dashboard metrics, and notifications.
- Lightweight `en` / `hi` locale provider for shell + counter strings; language switcher in the sidebar; `document.documentElement.lang` updated.
- Money/time formatting uses `en-IN` Indian digit grouping.
- Router uses `React.lazy` + `Suspense` per page; production build emits separate chunks (e.g. `pos-page`, `sales-page`) instead of one monolithic page bundle.

| Step | Findings | Work |
|------|----------|------|
| G3-S1 | #29 | Done |
| G3-S2 | #30, #31 | Done |
| G3-S3 | #5, #6 | Done |
| G3-S4 | #12 | Done |
| G3-S5 | #16, #17 | Done |
| G3-S6 | #18, #19, #24, #26 | Done |
| G3-S7 | #32, #33, #34 | Done |

### Gate 3 exit criteria

All G3 steps done; findings #5–6, #12, #16–19, #24, #26, #29–34 closed.

---

## Finding → step map

| # | Severity | Step |
|---|----------|------|
| 1 | Blocker | G1-S1 |
| 2 | Blocker | G1-S1 |
| 3 | Blocker | G1-S1 |
| 4 | Blocker | G1-S1 |
| 5 | High | G3-S3 |
| 6 | High | G3-S3 |
| 7 | Blocker | G1-S2 |
| 8 | Blocker | G1-S3 |
| 9 | Blocker | G1-S4 |
| 10 | Blocker | G1-S4 |
| 11 | High | G1-S5 |
| 12 | High | G3-S4 |
| 13 | High | G1-S6 |
| 14 | Blocker | G2-S1 |
| 15 | Blocker | G2-S2 |
| 16 | High | G3-S5 |
| 17 | High | G3-S5 |
| 18 | Medium | G3-S6 |
| 19 | Medium | G3-S6 |
| 20 | Blocker | G2-S4 |
| 21 | Blocker | G2-S4 |
| 22 | Blocker | G2-S5 |
| 23 | High | G2-S6 |
| 24 | High | G3-S6 |
| 25 | High | G2-S6 |
| 26 | Medium | G3-S6 |
| 27 | Blocker | G2-S3 |
| 28 | Blocker | G2-S3 |
| 29 | Blocker | G3-S1 |
| 30 | High | G3-S2 |
| 31 | High | G3-S2 |
| 32 | Medium | G3-S7 |
| 33 | Medium | G3-S7 |
| 34 | Medium | G3-S7 |

---

## Working agreement

1. One roadmap step at a time; prefer completing a step over partial progress on many.
2. After each step: mark it done here, tick the matching audit-canvas findings, run the relevant test suite.
3. Do not treat `docs/07-path-to-usable-product.md` as the baseline until it is rewritten against this roadmap.
4. Prefer fixes with file:line evidence from the audit over speculative refactors.
