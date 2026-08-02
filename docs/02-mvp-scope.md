The second document is the most important one. It defines **exactly what we are building first** and, equally importantly, what we are **not** building.

# Inventory Management System - MVP Scope (Version 1.0)

## Purpose

This document defines the exact scope of Version 1.0 (MVP).

The objective is **not** to build the most feature-rich inventory system.

The objective is to deliver a product that solves the core inventory problems for a small business owner and is ready for real-world usage.

Every feature added to the MVP must support the Product Vision.

---

# MVP Goals

By the end of Version 1.0, a business owner should be able to:

* Create a business
* Add products
* Manage inventory
* Record purchases
* Record sales
* Manage customers
* Manage suppliers
* View inventory status
* Track payments
* View basic reports

without using paper registers.

---

# User Roles

## Business Owner

Full access to the system.

This is the only user role supported in Version 1.0.

Employee roles will be introduced later.

---

# Functional Scope

## Module 1 — Authentication

### Features

* Sign Up
* Login
* Logout
* Forgot Password
* Email Verification

---

## Module 2 — Business Setup

### Features

* Business Name
* Business Type
* Business Address
* Mobile Number
* Currency
* Time Zone

A user may own multiple businesses in the future, but Version 1.0 supports one business per account.

---

## Module 3 — Product Management

### Features

* Create Product
* Update Product
* Delete Product
* Archive Product
* Search Product
* Product Categories
* Product Units
* Opening Stock
* Cost Price
* Selling Price
* Current Stock

Each product automatically maintains its stock quantity.

---

## Module 4 — Inventory

### Features

* Current Stock
* Manual Stock Adjustment
* Stock History
* Inventory Valuation
* Low Stock Alert

Every stock change must be recorded.

Stock history can never be edited.

---

## Module 5 — Supplier Management

### Features

* Add Supplier
* Edit Supplier
* Delete Supplier
* Supplier Contact Details
* Purchase History

---

## Module 6 — Customer Management

### Features

* Add Customer
* Edit Customer
* Delete Customer
* Customer Contact Details
* Purchase History
* Outstanding Balance

---

## Module 7 — Purchase Management

### Features

* Record Purchase
* Select Supplier
* Add Products
* Purchase Price
* Quantity
* Purchase Date
* Notes
* Payment Status

Recording a purchase automatically increases inventory.

---

## Module 8 — Sales Management

### Features

* Record Sale
* Select Customer
* Add Products
* Quantity
* Selling Price
* Sale Date
* Payment Status
* Notes

Recording a sale automatically decreases inventory.

The system must prevent negative stock unless explicitly allowed in settings.

---

## Module 9 — Payments

### Customer Payments

* Mark as Paid
* Mark as Partial
* Outstanding Balance

### Supplier Payments

* Paid
* Partial
* Pending

No accounting or double-entry bookkeeping in Version 1.0.

---

## Module 10 — Dashboard

Display:

* Today's Sales
* Today's Purchases
* Total Revenue
* Total Products
* Current Stock Value
* Low Stock Products
* Pending Customer Payments
* Pending Supplier Payments

The dashboard should answer the owner's most common questions in under one minute.

---

## Module 11 — Reports

### Inventory Report

### Purchase Report

### Sales Report

### Customer Outstanding Report

### Supplier Outstanding Report

Reports should support PDF and Excel export in a future release.

---

# Non-Functional Requirements

## Simplicity

Every workflow should be understandable without training.

---

## Performance

Common operations should complete in under 2 seconds under normal conditions.

---

## Mobile Friendly

All pages must work well on phones and tablets.

---

## Cloud Native

Accessible from anywhere.

Automatic backups.

---

## Security

Encrypted passwords.

Role-based authorization.

Secure APIs.

Audit logging for important business events.

---

# Out of Scope (Not in MVP)

The following features are intentionally excluded:

## Accounting

* Double Entry
* Journal
* Ledger
* Balance Sheet
* Profit & Loss

---

## GST

* GST Filing
* E-Way Bill
* Tax Reports

---

## AI

* AI Assistant
* Smart Analytics
* Demand Prediction
* Chat Interface

---

## OCR

Invoice Scanning

---

## Voice

Voice Commands

Speech-to-Text

---

## Barcode

Barcode Generation

Barcode Scanning

---

## WhatsApp

Notifications

Invoice Sharing

Automation

---

## Multi-Warehouse

Warehouse Transfers

Warehouse Management

---

## Mobile Apps

Android

iOS

The MVP will be delivered as a responsive web application.

---

## Multi-User Permissions

Cashier

Manager

Store Keeper

Accountant

---

## Offline Mode

---

## API Integrations

Payment Gateways

Shipping Providers

Third-party Accounting Software

---

# MVP Success Criteria

Version 1.0 is considered successful when a real business can completely replace its paper inventory register with the application.

The owner should be able to operate the business for an entire day using only the software.

---

# Release Checklist

Before Version 1.0 is released:

* Authentication complete
* Business setup complete
* Product management complete
* Inventory tracking complete
* Purchase workflow complete
* Sales workflow complete
* Customer management complete
* Supplier management complete
* Dashboard complete
* Reports complete
* Basic testing completed
* Production deployment completed

---

# Definition of Done

The MVP is complete when a small business owner can:

1. Register an account.
2. Set up the business.
3. Add products.
4. Record purchases.
5. Record sales.
6. Track stock automatically.
7. Track customer and supplier dues.
8. View the dashboard.
9. Run the business without maintaining a paper inventory register.

At that point, Version 1.0 is ready for deployment and onboarding of the first real customers.

## One change I'd make before we start development

I would remove one feature from the MVP:

* ❌ Delete Product

Instead, implement only:

* ✅ Archive Product

Inventory systems should generally preserve historical records. If a product has ever been purchased or sold, deleting it can break reports and audit history. Archiving keeps the data intact while hiding inactive products from daily use.

After this, we'll create **Document 3 – Product Backlog**, which will become our master task list. Every development task, sprint, and GitHub issue will originate from that document.
