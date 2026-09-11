# Path to a Fully Usable Product

## Status (this release)

The product is **ready for a self-hosted Indian shop**: owner + staff, GST invoices, barcode counter, Excel reports, logo on PDFs, alerts, activity log, Docker local and HTTPS VPS overlay, backups, CI.

It is **not** a turnkey hosted SaaS (you supply SMTP and a domain) and it does **not** include e-way bill, WhatsApp, native apps, multi-warehouse, or full accounting.

Companion: [06-as-built-technical.md](06-as-built-technical.md).

---

## Definition of “market-complete” for this product

A shop owner can:

1. Sign up, verify email, and create one business (GSTIN, state, logo)
2. Maintain products (barcode, HSN, GST %) and stock
3. Record purchases and sales with payments, returns, and tax
4. Sell from a barcode counter and print GST invoices
5. Invite a clerk who cannot change catalog, purchases, or reports
6. Download Excel for the accountant (including GST)
7. See low-stock / overdue alerts and an activity log
8. Run the stack locally or on a VPS with HTTPS, without publishing Postgres

**Out of scope:** AI, WhatsApp, OCR, voice, native apps, multi-warehouse, industry packs, e-way bill, GSTR filing, and full accounting.

---

## Scorecard

| Dimension | Verdict |
|-----------|---------|
| Core buy/sell loop | Done |
| GST invoices and GST report | Done (not e-way / GSTR portal) |
| Staff RBAC | Done (OWNER / MANAGER / CLERK) |
| Counter / barcode | Done (lookup + POS page; not hardware driver) |
| Report Excel + logo PDFs | Done |
| Audit + in-app alerts | Done |
| Mobile navigation | Done (drawer under 960px) |
| Ops | Compose, `prod` fail-fast, Caddy HTTPS overlay, backup/restore, MIT, CI |
| Hosted email / multi-tenant SaaS billing | Operator SMTP; many shops per host via `/platform`. No billed SaaS plans yet |

---

## Remaining work that is **intentionally not** this product

- E-way bill, e-invoice IRN, GSTR-1 auto-file
- Managed category/unit master APIs (free-text + UI presets today)
- Product images
- WhatsApp / native apps / OCR
- Multi-warehouse
- Split of large sales/purchases page modules (works; maintainability only)

If a pilot shop needs one of those, treat it as a new epic in [03-product-backlog.md](03-product-backlog.md).

---

## Operator checklist (shared host)

- [ ] `.env` uses `SPRING_PROFILES_ACTIVE=prod` and a unique `APP_JWT_SECRET`
- [ ] SMTP delivers verify, invite, and reset mail
- [ ] `APP_PUBLIC_APP_URL` matches the HTTPS origin
- [ ] `./scripts/deploy.sh` with `APP_DOMAIN` + `ACME_EMAIL`
- [ ] `./scripts/backup.sh` on a schedule (cron)
- [ ] Postgres password is not the example value
