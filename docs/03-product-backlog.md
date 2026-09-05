This is the document we'll use **every day** during development. It is the master task list for the entire product.

The rule is simple:

* Every feature starts here.
* Every GitHub issue comes from here.
* Every completed task is checked off here.
* Nothing gets built unless it exists in this document.

# Inventory Management System - Product Backlog

## Purpose

This document is the single source of truth for all product work.

Tasks are organized into Epics.

Each Epic contains Features.

Each Feature can later be broken into Development Tasks.

For what is already built in code, see [06-as-built-technical.md](06-as-built-technical.md).
For the gap to a pilot-usable product (including ops), see [07-path-to-usable-product.md](07-path-to-usable-product.md).

Priority Levels:

* P0 = Critical (Must Have)
* P1 = Important
* P2 = Nice to Have
* P3 = Future

Status:

* TODO
* IN PROGRESS
* DONE

---

# EPIC 1 — Authentication

Priority: P0

### Features

* [x] User Registration
* [x] Email Verification
* [x] Login
* [x] Logout
* [x] Forgot Password
* [x] Reset Password
* [x] JWT Authentication
* [x] Refresh Token
* [x] User Profile

---

# EPIC 2 — Business Setup

Priority: P0

### Features

* [x] Create Business
* [x] Update Business
* [x] Business Settings
* [x] Currency
* [x] Time Zone
* [x] Business Logo

---

# EPIC 3 — Product Management

Priority: P0

### Features

* [x] Create Product
* [x] Edit Product
* [x] Archive Product
* [x] Unarchive Product
* [x] Search Product
* [x] Product Categories
* [x] Product Units
* [x] Cost Price
* [x] Selling Price
* [x] Opening Stock
* [x] Excel Import
* [x] Excel Export
* [ ] Product Image

Notes:

* Categories and units are free-text fields on the product (with unit presets in the UI), not separate master-data APIs. That is the shipped shop model.
* Excel import/export is implemented for products (Phase 2 work started early).

---

# EPIC 4 — Inventory

Priority: P0

### Features

* [x] Current Stock
* [x] Stock Adjustment
* [x] Stock Movement History
* [x] Inventory Valuation
* [x] Low Stock Alert
* [x] Stock Search

---

# EPIC 5 — Supplier Management

Priority: P0

### Features

* [x] Create Supplier
* [x] Update Supplier
* [x] Archive Supplier
* [x] Unarchive Supplier
* [x] Supplier Search
* [x] Supplier Purchase History

---

# EPIC 6 — Customer Management

Priority: P0

### Features

* [x] Create Customer
* [x] Update Customer
* [x] Archive Customer
* [x] Unarchive Customer
* [x] Customer Search
* [x] Outstanding Balance
* [x] Purchase History

---

# EPIC 7 — Purchase Management

Priority: P0

### Features

* [x] Create Purchase
* [x] Purchase Details
* [x] Purchase History
* [x] Purchase Notes
* [x] Purchase Payment Status
* [x] Purchase Cancellation
* [x] Purchase Returns
* [x] Purchase Bill PDF

Business Rule:

Creating a Purchase increases stock.

---

# EPIC 8 — Sales Management

Priority: P0

### Features

* [x] Create Sale
* [x] Sale Details
* [x] Sale History
* [x] Sale Notes
* [x] Sale Payment Status
* [x] Sale Cancellation
* [x] Sale Returns
* [x] Sale Invoice PDF

Business Rule:

Creating a Sale decreases stock.

Negative stock is blocked by default.

---

# EPIC 9 — Payments

Priority: P0

### Features

Customer Payments

* [x] Full Payment
* [x] Partial Payment
* [x] Outstanding Amount

Supplier Payments

* [x] Full Payment
* [x] Partial Payment
* [x] Outstanding Amount

---

# EPIC 10 — Dashboard

Priority: P0

### Features

* [x] Today's Sales
* [x] Today's Purchases
* [x] Revenue
* [x] Total Products
* [x] Inventory Value
* [x] Low Stock
* [x] Outstanding Customers
* [x] Outstanding Suppliers

---

# EPIC 11 — Reports

Priority: P0

### Features

* [x] Inventory Report
* [x] Purchase Report
* [x] Sales Report
* [x] Customer Outstanding
* [x] Supplier Outstanding
* [x] Report File Export (Excel / PDF)

Notes:

* On-screen reports plus Excel download (inventory, sales, purchases, dues, GST). Sale/purchase PDFs are separate document endpoints.

---

# EPIC 12 — Settings

Priority: P1

### Features

* [x] Business Preferences
* [x] Low Stock Threshold
* [x] Negative Stock Setting
* [x] Currency Format
* [x] Date Format

---

# EPIC 13 — Audit Log

Priority: P1

### Features

* [x] View Activity Log
* [ ] Search Activity
* [x] User Activity Timeline

---

# EPIC 14 — Notifications

Priority: P1

### Features

* [x] Low Stock Notification
* [x] Payment Reminder
* [x] Dashboard Alerts

---

# EPIC 15 — Pilot packaging (ops)

Priority: P0

Not a user-facing shop feature, but it blocks a real pilot. Tracked here so it is planned like everything else. Detail: [07-path-to-usable-product.md](07-path-to-usable-product.md).

### Features

* [x] README and onboarding (env vars, Compose, host-dev, docs 01–07)
* [x] Runnable Docker Compose (Postgres + backend + frontend)
* [x] Frontend nginx SPA fallback and `/api` reverse proxy
* [x] Makefile `up` / `down` / `logs` / `test` / `backup` / `prod`
* [x] Production-safe configuration (required JWT secret, SMTP, prod profile)
* [x] CI (backend tests + frontend tests and build on PR)
* [x] First-user copy polish
* [x] HTTPS overlay (Caddy) and host-closed Postgres in prod Compose
* [x] Database backup / restore scripts
* [x] MIT license

---

# EPIC 16 — Shop volume

Priority: P1

Needed so a catalog of hundreds or thousands of SKUs does not download in one payload.

### Features

* [x] Pagination + indexed search on list APIs/UI


# FUTURE EPICS (Not MVP)

Priority: P2 / P3

## Barcode

* [x] Barcode lookup / scanning at the counter
* [ ] Barcode label generation (print sheets) — not required for daily sales

## Multi-Warehouse

* [ ] Warehouses
* [ ] Stock Transfer

## GST

* [x] GST Invoices
* [x] GST Reports
* [ ] E-Way Bill

## Employee Management

* [x] Roles
* [x] Permissions
* [ ] Attendance

## AI

* [ ] AI Assistant
* [ ] Smart Reorder Suggestions
* [ ] Sales Forecast
* [ ] Dead Stock Detection
* [ ] Natural Language Search

## OCR

* [ ] Supplier Invoice Scanner
* [ ] Product Import

## Voice

* [ ] Voice Purchase Entry
* [ ] Voice Sales Entry

## WhatsApp

* [ ] Share Invoice
* [ ] Payment Reminder
* [ ] Daily Summary

## Mobile Apps

* [ ] Android App
* [ ] iOS App

# EPIC 17 — Windows desktop

Priority: P1

Hybrid register: bill on a Windows PC with no internet, then (later) sync to the VPS. **Phase 1 is the installable local app.** Do not sell the same stock from the phone cloud and an offline PC until Phase 3.

### Features

* [x] Spring `desktop` profile (localhost API, generated JWT, SPA from JAR, no SMTP)
* [x] Tauri 2 window that starts bundled Postgres + JRE + JAR and opens http://127.0.0.1:18080/
* [x] NSIS Setup.exe via GitHub Actions `windows-latest`
* [x] Export / restore `.sql.gz` on Settings (restore applies on next launch)
* [ ] Phase 2: pair with cloud shop, snapshot pull, backup push
* [ ] Phase 3: two-way sync by replaying sales/stock movements

Operator notes: [10-windows-desktop.md](10-windows-desktop.md).

---

# Release Plan

## Version 1.0

Complete every P0 feature.

## Version 1.1

Complete P1 features.

## Version 2.0

Begin P2 features.

## Version 3.0

Introduce AI-powered capabilities.

---

# Development Rules

1. No feature starts without a backlog entry.
2. No feature is marked DONE until tested.
3. P0 work always has priority over P1, P2, and P3.
4. Every new feature must support the Product Vision.
5. Prefer shipping small, complete increments over large unfinished work.
6. Preserve data integrity; never sacrifice correctness for speed.

---

# Current Sprint Goal

**Self-hosted Indian shop product is in tree.** GST, staff roles, barcode counter, report Excel, logo PDFs, alerts, activity log, HTTPS overlay, backups, and a Phase 1 Windows desktop installer path are implemented. See [07-path-to-usable-product.md](07-path-to-usable-product.md) and [10-windows-desktop.md](10-windows-desktop.md).

As-built: [06-as-built-technical.md](06-as-built-technical.md).

## Status note (synced with codebase)

P0 shop loop, P1 staff/audit/alerts/pagination, and India GST/barcode/report-export are done. Windows desktop Phase 1 (local installer, no cloud sync) is in tree. Still open by choice: product images, e-way bill, attendance, native mobile apps, WhatsApp, AI, desktop Phase 2/3 sync. Category/unit remain free-text.

Ops: README + Compose + nginx + Caddy prod overlay + backup scripts + MIT + CI (backend tests, frontend tests + build, Windows NSIS installer).