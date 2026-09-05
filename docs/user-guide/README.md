# Shop user guide

This folder is the **operator manual** for people who run the shop: the owner, a manager, or a clerk at the counter.

It is written for someone who has **never used this software before**. You do not need accounting training. If you can use a phone browser and you already keep a paper register, you can use this system.

Technical setup (Docker, SMTP, backups) lives in the project [README](../../README.md), not here. This guide assumes the shop software is already open in a browser — usually the HTTPS address your operator gave you, or **http://localhost:3000** on a local install. Do not run the database on the counter PC.

---

## What this software is

One workspace for **one shop**. You:

- Keep a product list and current stock
- Buy from suppliers (purchases / bills)
- Sell to customers (invoices and a barcode counter)
- Record payments, returns, and cancellations
- Print GST invoices (if GST is on)
- Share a bill PDF from the phone (share sheet / WhatsApp)
- Download Excel for the accountant
- Invite staff with limited access

It is **not** full accounting, not GSTR filing, not e-way bill, and not the WhatsApp Business API.

---

## How to use this guide

1. New to the shop? Start with [First day](first-day.md).
2. Lost on a screen? Open that screen’s page from the list below.
3. You already know *what* you want (collect money, return bags, print a bill)? Use [Everyday jobs](everyday-jobs.md).

On every screen page you will find:

- **Why this screen exists**
- **Who can open it** (owner / manager / clerk)
- **What to type and what to choose**
- **What happens after you save**
- **Common mistakes**

---

## Screens (every page in the app)

| In the left menu | What it is for | Guide |
|------------------|----------------|--------|
| *(Sign in / Mobile OTP)* | Log in with phone or email | [Sign-in and account](sign-in-and-account.md) |
| *(Forgot / reset password)* | Recover access by email | [Sign-in and account](sign-in-and-account.md) |
| *(Verify email)* | Confirm the email address | [Sign-in and account](sign-in-and-account.md) |
| *(Accept invite)* | Join the shop as staff | [Sign-in and account](sign-in-and-account.md) |
| Start | Name the shop (first time) | [First day](first-day.md) |
| Counter | Fast barcode sale, share or print | [Counter](counter.md) |
| Sales | Full invoices, credit, returns, cancel | [Sales](sales.md) |
| Customers | Parties you sell to, and what they owe | [Customers](customers.md) |
| Dashboard | Today’s numbers and stock health | [Dashboard](dashboard.md) |
| Products | Catalog, prices, barcode, GST %, Excel | [Products](products.md) |
| Inventory | Stock levels, value, manual count fixes | [Inventory](inventory.md) |
| Purchases | Supplier bills, pay, return, cancel | [Purchases](purchases.md) |
| Suppliers | Parties you buy from, and what you owe | [Suppliers](suppliers.md) |
| Reports | Stock, trading, dues, GST + Excel | [Reports](reports.md) |
| Team | Invite managers and clerks | [Team](team.md) |
| Activity | Who changed what | [Activity](activity.md) |
| Business | Shop name, GSTIN, logo | [Business](business.md) |
| Settings | Currency, dates, GST, negative stock | [Settings](settings.md) |
| Profile | Your name and password | [Profile](profile.md) |

Shared chrome (menu, alerts, Sign out, mobile Menu button): [Around the workspace](workspace.md).

**How do I…?** (sell, collect, return, Excel, invite staff): [Everyday jobs](everyday-jobs.md).

**GST:** what to type and which box to tick: [GST on invoices](gst.md).

---

## Roles in one sentence

| Role | Typical person | Can do |
|------|----------------|--------|
| **Owner** | Shop owner | Everything, including inviting staff |
| **Manager** | Trusted person who runs the shop | Almost everything except inviting/removing staff |
| **Clerk** | Counter / billing person | Sell, take payments, manage customers, look at stock. Cannot change catalog, purchases, reports, business, or settings |

Details: [Around the workspace](workspace.md).

---

## Suggested order after signup

If the operator created your shop, skip naming it and start at Counter.

1. [Sign in with mobile OTP or the issued email](sign-in-and-account.md)
2. [Name the shop](first-day.md) only if Start asks for it (GSTIN later on [Business](business.md) if you charge GST)
3. [Counter](counter.md) — first bill, then share or print
4. [Products](products.md) when you replace the starter items
5. [Suppliers](suppliers.md) then [Purchases](purchases.md) (or set opening stock on products if stock is already in the shop)
6. [Team](team.md) when someone else needs a login

---

## Words this app uses

| Word on screen | Meaning |
|----------------|---------|
| **Sale / invoice** | You sold goods to a customer |
| **Purchase / bill** | You bought goods from a supplier |
| **Outstanding / due** | Still unpaid |
| **PENDING** | Nothing paid yet |
| **PARTIAL** | Some money received, some still due |
| **PAID** | Fully paid (after returns, if any) |
| **Archive** | Hide from the default list; history stays |
| **Restore** | Bring an archived record back to the default list |
| **Opening stock** | Quantity already in the shop when you first add a product |
| **Low stock** | Current quantity is at or below the threshold you set |
| **Interstate (IGST)** | Customer or supplier is in a **different state** than the shop |
| **Prices include GST** | The rupee amount you type already contains tax |

---

## What this guide does not cover

- Installing Docker, SMTP, or HTTPS — see the project README
- Filing GSTR-1 on the government portal — export GST Excel from Reports and give it to your accountant
- Barcode **hardware drivers** — most USB scanners type digits into the Counter barcode box and press Enter, which is enough
