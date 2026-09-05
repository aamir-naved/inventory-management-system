# Inventory Management System

Self-hosted inventory, GST invoicing, and counter sales for a small Indian shop. One owner creates the business, invites managers and clerks, keeps stock, buys from suppliers, sells at the counter (barcode), and downloads invoices and Excel reports.

This is **not** a billed SaaS product yet. You run **one** Docker install on a VPS. Shop data is isolated by `business_id`. In production, **you** onboard shops from `/platform` unless you set `APP_OPEN_REGISTRATION=true`. Auth email needs SMTP you provide. Phone OTP logs the code until `APP_SMS_WEBHOOK_URL` is set. The `prod` profile will not start without a real JWT secret, SMTP host, public app URL, and (when registration is closed) a Super Admin email/password.

## Quick start (local)

Prerequisites: [Docker Desktop](https://www.docker.com/products/docker-desktop/) (or Docker Engine + Compose v2).

```bash
./scripts/start.sh
```

That builds images and starts Postgres, the API, and the UI. Or: `make start`. After you change code, run `./scripts/restart.sh` (a browser refresh is not enough). Full start/stop/restart cheat sheet: [running-app.md](running-app.md).

Open **http://localhost:3000**. The UI and API share that origin (`/api` is reverse-proxied to the backend).

| Service  | URL / port        | Notes                                      |
|----------|-------------------|--------------------------------------------|
| UI       | http://localhost:3000 | nginx SPA + `/api` proxy               |
| API      | http://localhost:8080/api | also reachable via the UI origin     |
| Health   | http://localhost:3000/api/actuator/health | via the proxy                    |
| Postgres | host port in `.env` (`POSTGRES_PORT`, default 5432) | `inventory_management` / `inventory_user`  |

Stop with `./scripts/stop.sh` or `make stop`. Follow logs with `make logs`.

If ports 3000, 8080, or 5432 are already in use, change `FRONTEND_PORT`, `BACKEND_PORT`, or `POSTGRES_PORT` in `.env`.

### First-day shop setup

1. Shop owner opens the URL. In production you create the shop from `/platform` and send credentials (or they self-register if `APP_OPEN_REGISTRATION=true`).
2. They sign in with **mobile OTP** or the email you issued.
3. If the shop is new and self-serve is on, they type the **shop name** on Start. Walk-in plus five hardware starter items are created.
4. They sell from **Counter**. **Share bill** or **Print**.
5. Add GSTIN/logo on **Business** when needed. Invite a clerk from **Team** by email.

Direction and definition of done: [08-small-town-cloud.md](docs/08-small-town-cloud.md). Super Admin: [09-platform-admin.md](docs/09-platform-admin.md). Shop screens: [docs/user-guide/](docs/user-guide/README.md).

### Verify / reset email (`dev`, no SMTP)

Without mail settings, the backend **logs** verification, invite, and password-reset links:

```text
Mail not configured; logging outbound message. to=... subject=... body=...
```

Copy the link from `docker compose logs backend` (or `make logs`).

## What the product includes

- Owner / manager / clerk roles (invite by email; clerks sell, cannot edit catalog or reports)
- GST on sales and purchases (CGST/SGST or IGST), GSTIN on PDFs, GST Excel report
- Barcode lookup at the counter
- Invoice and purchase-bill PDFs with shop logo
- Excel for inventory, sales, purchases, dues, and GST
- Low-stock and overdue-payment alerts
- Activity log for sales, purchases, and staff changes
- Phone-friendly navigation

Out of scope: native mobile apps, WhatsApp Business API, e-way bill, multi-warehouse, and full accounting. Device share / `wa.me` for invoice PDFs is in scope. A Phase 1 **Windows desktop installer** (local engine, no cloud sync yet) is documented in [docs/10-windows-desktop.md](docs/10-windows-desktop.md).

## Production (HTTPS on a VPS)

Set these in `.env` (see [.env.example](.env.example)):

| Variable | Requirement |
|----------|-------------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `APP_JWT_SECRET` | At least 32 characters; **not** the development default. `openssl rand -base64 48` |
| `SPRING_MAIL_HOST` | SMTP so verify/invite/reset mail is delivered |
| `APP_PUBLIC_APP_URL` | Public UI origin, for example `https://your-shop.example.com` |
| `APP_DOMAIN` | Hostname for Let’s Encrypt (Caddy) |
| `ACME_EMAIL` | Contact email for certificates |
| `POSTGRES_PASSWORD` | Strong password; do not keep the example value |

Then:

```bash
./scripts/deploy.sh
```

That starts Compose with [docker-compose.prod.yml](docker-compose.prod.yml): Caddy on 80/443, Postgres and the API **not** published on the host, `prod` fail-fast on the backend.

Backups:

```bash
./scripts/backup.sh
./scripts/restore.sh backups/inventory-YYYYMMDDTHHMMSSZ.sql.gz
```

## Host development (without Compose)

Prerequisites: JDK 17, Maven 3.9+, Node 22, and a running PostgreSQL database.

```bash
# terminal 1 — API (http://localhost:8080/api)
cd backend
DB_URL=jdbc:postgresql://localhost:5432/inventory_management \
DB_USERNAME=inventory_user \
DB_PASSWORD=inventory_password \
mvn spring-boot:run

# terminal 2 — Vite UI (http://localhost:5173)
cd frontend
npm install
npm run dev
```

The Vite app calls `http://localhost:8080/api` unless you set `VITE_API_URL`. CORS already allows `http://localhost:5173`.

Day-to-day start/stop/restart commands: [running-app.md](running-app.md). Tests: `./scripts/test.sh` (backend `mvn test`, frontend `npm test` + build).

## Environment variables

Copy [.env.example](.env.example) to `.env` for Compose interpolation.

| Variable | Default | Used by |
|----------|---------|---------|
| `POSTGRES_DB` | `inventory_management` | Compose Postgres |
| `POSTGRES_USER` | `inventory_user` | Compose Postgres / `DB_USERNAME` |
| `POSTGRES_PASSWORD` | `inventory_password` | Compose Postgres / `DB_PASSWORD` |
| `POSTGRES_PORT` | `5432` | Compose host port (not published in prod overlay) |
| `DB_URL` | `jdbc:postgresql://localhost:5432/inventory_management` | Backend (Compose overrides host to `postgres`) |
| `BACKEND_PORT` | `8080` | Compose host port (not published in prod overlay) |
| `FRONTEND_PORT` | `3000` | Compose host port (not published in prod overlay) |
| `SPRING_PROFILES_ACTIVE` | `dev` | Backend. Use `prod` on a shared host |
| `APP_JWT_SECRET` | development default in `application-dev.yml` only | **Required** when `prod` is active |
| `APP_PUBLIC_APP_URL` | `http://localhost:3000` (Compose) | Links in verify/invite/reset emails |
| `APP_MAIL_FROM` | `noreply@inventory.local` | From address when SMTP is enabled |
| `APP_CORS_ALLOWED_ORIGINS` | Vite hosts in `dev` | Compose UI is same-origin |
| `APP_DOMAIN` / `ACME_EMAIL` | unset | Caddy HTTPS in the prod overlay |
| `APP_SMS_WEBHOOK_URL` | unset | Optional POST `{to,body}` for OTP SMS; otherwise logs the code |
| `APP_OPEN_REGISTRATION` | `true` in `dev`, `false` in `prod` | When false, only Super Admin creates shops |
| `APP_PLATFORM_ADMIN_EMAIL` | unset | Bootstrap Super Admin; **required** in prod if registration is closed |
| `APP_PLATFORM_ADMIN_PASSWORD` | unset | Min 12 characters in prod when registration is closed |
| `APP_PLATFORM_ADMIN_RESET` | `false` | Set true for one restart to rotate the admin password |
| `VITE_API_URL` | `http://localhost:8080/api` (Vite) / `/api` (Compose image) | Baked in at `npm run build` |

## Product docs

| Doc | Contents |
|-----|----------|
| [01-product-vision.md](docs/01-product-vision.md) | Why the product exists |
| [02-mvp-scope.md](docs/02-mvp-scope.md) | Version 1.0 boundary |
| [03-product-backlog.md](docs/03-product-backlog.md) | Feature checklist |
| [04-system-architecture.md](docs/04-system-architecture.md) | Intended architecture |
| [05-product-strategy-roadmap.md](docs/05-product-strategy-roadmap.md) | Longer-term roadmap |
| [06-as-built-technical.md](docs/06-as-built-technical.md) | What the codebase actually is today |
| [07-path-to-usable-product.md](docs/07-path-to-usable-product.md) | Market-ready status and remaining out-of-scope items |
| [08-small-town-cloud.md](docs/08-small-town-cloud.md) | Hosted register: phone OTP, sell-first, backups |
| [09-platform-admin.md](docs/09-platform-admin.md) | Super Admin console: add/suspend shops |
| [10-windows-desktop.md](docs/10-windows-desktop.md) | Phase 1 Windows installer (local engine) |
| [user-guide/](docs/user-guide/README.md) | Shop operator screens |

## Stack

- **Frontend:** React 18, Vite, TypeScript, TanStack Query, Vitest
- **Backend:** Java 17, Spring Boot 3.3, PostgreSQL, Flyway
- **Documents:** OpenPDF invoices/bills; Apache POI product Excel and report Excel
- **License:** MIT
