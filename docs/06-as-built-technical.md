# As-Built Technical Documentation

## Purpose

This document describes **what the product actually is today**, based on the running codebase.

Companion docs: [01-product-vision.md](01-product-vision.md), [02-mvp-scope.md](02-mvp-scope.md), [07-path-to-usable-product.md](07-path-to-usable-product.md).

---

## Product identity

**What it is:** A self-hosted **inventory + GST buy/sell ledger**. Each shop is a `businesses` row. Shop users are OWNER / MANAGER / CLERK. A platform Super Admin (`PLATFORM_ADMIN`) creates and suspends shops from `/platform`. Production registration is closed unless `APP_OPEN_REGISTRATION=true`.

**What it is not:** ERP, multi-warehouse, e-way bill, WhatsApp, native apps, or a hosted cloud SaaS (you operate Docker + SMTP).

**Maturity:** Daily shop loop, staff RBAC, GST documents, POS barcode, report Excel, audit, alerts, mobile nav, Compose, HTTPS overlay, backups, CI, and MIT license are in the tree.

---

## Stack

| Layer | As built |
|-------|----------|
| Frontend | React 18, Vite, TypeScript, TanStack Query, React Router 6, custom `styles.css`, Vitest |
| Backend | Java 17, Spring Boot 3.3, PostgreSQL, Flyway V1–V14 |
| Auth | JWT HS256 (`sub` + email); refresh/email/reset/invite tokens hashed in DB; phone OTP |
| Tenancy | `X-Business-Id` + active membership + `businesses.active` |
| RBAC | `OWNER` / `MANAGER` / `CLERK` on membership; `PLATFORM_ADMIN` on `app_users` (no membership) |
| Docs/Excel | OpenPDF invoices/bills (logo, GSTIN, HSN/tax); POI product + report workbooks |
| Deploy | `docker-compose.yml` (local); `docker-compose.prod.yml` + Caddy HTTPS; `scripts/backup.sh` |

---

## Repository layout

```
├── docs/
├── backend/                 Spring Boot modular monolith
├── frontend/                Vite React SPA (nginx `/api` proxy in image)
├── scripts/                 dev.sh, build.sh, test.sh, deploy.sh, backup.sh, restore.sh
├── docker-compose.yml       Local Postgres + API + UI
├── docker-compose.prod.yml  Hide DB/API ports; Caddy 80/443
├── Caddyfile
├── Makefile
├── .env.example
├── LICENSE                  MIT
└── .github/workflows/       mvn test; npm test + build
```

### Compose runtime

`docker compose up --build` starts Postgres 16, the API (`:8080`), and nginx (`:3000` → `:80`). SPA is built with `VITE_API_URL=/api`. `prod` refuses to start without `APP_JWT_SECRET` (≥ 32 chars, not the dev default), `SPRING_MAIL_HOST`, `APP_PUBLIC_APP_URL`, and (when registration is closed) `APP_PLATFORM_ADMIN_EMAIL` plus a ≥12-character `APP_PLATFORM_ADMIN_PASSWORD`.

Production overlay does not publish Postgres or the API on the host. Caddy terminates TLS for `APP_DOMAIN`.

---

## Domain model

`BaseEntity` (UUID) → `AuditableEntity` → `TenantAwareEntity` (`business_id`) for tenant data.

- Products: SKU, barcode (unique per business), HSN, GST rate, category/unit **free-text**, stock.
- Sales/purchases: line + header taxable/CGST/SGST/IGST; interstate flag.
- Business: GSTIN, state, inclusive pricing, logo bytes.
- Staff: `staff_invites`; membership role CHECK; one active membership per user.
- `audit_events` for staff and sale/purchase create/cancel.

### Flyway

| Version | Purpose |
|---------|---------|
| V1–V11 | Core shop schema, auth tokens, settings, returns, list indexes |
| V12 | GST/logo/barcode/HSN, tax columns, roles, staff invites, audit |
| V13 | Phone login and OTP |
| V14 | `platform_role`, `suspended_reason`, `plan_code` |

Tests use H2 `ddl-auto: create-drop` (Flyway off); JPA entities must stay complete.

---

## Backend modules

Base path: **`/api`**.

| Module | Status |
|--------|--------|
| Auth | Register (when open), login, OTP, public-config, verify, reset, refresh, profile, invite preview/accept |
| Business | Create/quick-start when registration is open; logo POST/GET/DELETE (PNG/JPEG ≤ 512KB) |
| Platform | Super Admin stats, shops, add shop, suspend, reset owner, user directory |
| Staff | Invite MANAGER/CLERK, roster, revoke, deactivate (owner writes) |
| Product | CRUD, archive, barcode lookup, Excel with barcode/HSN/GST |
| Inventory | Paged stock, movements, adjustments (manager+) |
| Sales / purchases | GST totals, cancel, returns, payments, PDF |
| Reports | Inventory, sales, purchases, dues, GST + `.xlsx` |
| Notifications | Computed low stock + overdue documents |
| Audit | Paged list (manager+) |
| Settings | Currency, dates, negative stock, GST flags |

**RBAC:** Clerks may use sales (except cancel), customers, GET products (no import/export/archive), GET inventory, dashboard, notifications, GET settings/business/logo. Managers match owners except staff writes. `/platform/**` requires `PLATFORM_ADMIN`. Platform admins cannot send `X-Business-Id`.

**Mail:** `LoggingMailService` when SMTP is absent; `SmtpMailService` when configured.

---

## Frontend

Routes: `/login`, `/welcome`, `/pos` (default after shop login), `/dashboard`, `/sales`, `/customers`, `/products`, `/inventory`, `/purchases`, `/suppliers`, `/reports`, `/team`, `/audit`, `/business-setup`, `/settings`, `/profile`. Platform Super Admin: `/platform`, `/platform/shops`, `/platform/shops/new`, `/platform/shops/:id`, `/platform/users`, `/platform/settings`.

Clerk nav: Counter, Sales, Customers, Dashboard, Inventory, Profile. Role guard on protected routes. No business → `/welcome` when registration is open, otherwise a contact-the-operator panel. Platform admin is routed to `/platform` and cannot use shop APIs.

---

## Tests and CI

**Backend:** `@SpringBootTest` + MockMvc across the shop loop, GST calculator, role filter, staff invite→403 clerk product POST, prod fail-fast.

**Frontend:** Vitest for GST math and clerk path rules; `npm run build` typechecks the SPA.

**CI:** Java 17 `mvn test`; Node 22 `npm test` + `npm run build`.
