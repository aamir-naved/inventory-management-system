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

* [ ] User Registration
* [ ] Email Verification
* [ ] Login
* [ ] Logout
* [ ] Forgot Password
* [ ] Reset Password
* [ ] JWT Authentication
* [ ] Refresh Token
* [ ] User Profile

---

# EPIC 2 — Business Setup

Priority: P0

### Features

* [ ] Create Business
* [ ] Update Business
* [ ] Business Settings
* [ ] Currency
* [ ] Time Zone
* [ ] Business Logo

---

# EPIC 3 — Product Management

Priority: P0

### Features

* [ ] Create Product
* [ ] Edit Product
* [ ] Archive Product
* [ ] Search Product
* [ ] Product Categories
* [ ] Product Units
* [ ] Cost Price
* [ ] Selling Price
* [ ] Opening Stock
* [ ] Product Image

---

# EPIC 4 — Inventory

Priority: P0

### Features

* [ ] Current Stock
* [ ] Stock Adjustment
* [ ] Stock Movement History
* [ ] Inventory Valuation
* [ ] Low Stock Alert
* [ ] Stock Search

---

# EPIC 5 — Supplier Management

Priority: P0

### Features

* [ ] Create Supplier
* [ ] Update Supplier
* [ ] Archive Supplier
* [ ] Supplier Search
* [ ] Supplier Purchase History

---

# EPIC 6 — Customer Management

Priority: P0

### Features

* [ ] Create Customer
* [ ] Update Customer
* [ ] Archive Customer
* [ ] Customer Search
* [ ] Outstanding Balance
* [ ] Purchase History

---

# EPIC 7 — Purchase Management

Priority: P0

### Features

* [ ] Create Purchase
* [ ] Purchase Details
* [ ] Purchase History
* [ ] Purchase Notes
* [ ] Purchase Payment Status
* [ ] Purchase Cancellation

Business Rule:

Creating a Purchase increases stock.

---

# EPIC 8 — Sales Management

Priority: P0

### Features

* [ ] Create Sale
* [ ] Sale Details
* [ ] Sale History
* [ ] Sale Notes
* [ ] Sale Payment Status
* [ ] Sale Cancellation

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

* [ ] Today's Sales
* [ ] Today's Purchases
* [ ] Revenue
* [ ] Total Products
* [ ] Inventory Value
* [ ] Low Stock
* [ ] Outstanding Customers
* [ ] Outstanding Suppliers

---

# EPIC 11 — Reports

Priority: P0

### Features

* [ ] Inventory Report
* [ ] Purchase Report
* [ ] Sales Report
* [ ] Customer Outstanding
* [ ] Supplier Outstanding

---

# EPIC 12 — Settings

Priority: P1

### Features

* [ ] Business Preferences
* [ ] Low Stock Threshold
* [ ] Negative Stock Setting
* [ ] Currency Format
* [ ] Date Format

---

# EPIC 13 — Audit Log

Priority: P1

### Features

* [ ] View Activity Log
* [ ] Search Activity
* [ ] User Activity Timeline

---

# EPIC 14 — Notifications

Priority: P1

### Features

* [ ] Low Stock Notification
* [ ] Payment Reminder
* [ ] Dashboard Alerts

---

# FUTURE EPICS (Not MVP)

Priority: P2 / P3

## Barcode

* [ ] Barcode Generation
* [ ] Barcode Scanning

## Multi-Warehouse

* [ ] Warehouses
* [ ] Stock Transfer

## GST

* [ ] GST Invoices
* [ ] GST Reports
* [ ] E-Way Bill

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

## Employee Management

* [ ] Roles
* [ ] Permissions
* [ ] Attendance

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

**Replace the paper inventory register with a simple, reliable digital system that a real shop owner can use every day.**

## One improvement before coding

I suggest we split this backlog into **Business Features** and **Technical Foundation**. For example:

* Business Features: Products, Purchases, Sales, Inventory.
* Technical Foundation: Authentication, Security, Database, API Standards, Logging, Testing, Deployment.

This separation makes sprint planning much easier because you'll always know whether you're building user value or infrastructure.

After this, I recommend we **skip any more high-level documentation** and move to **Document 4: System Architecture**, followed immediately by database design and project setup. That gets us into implementation quickly while still keeping the project organized.
