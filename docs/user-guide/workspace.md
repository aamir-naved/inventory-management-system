# Around the workspace

This page is the **shell** around every inner screen: the left menu, the top bar, alerts, and controls that look the same everywhere.

You see this layout only **after** you are signed in.

---

## Why this exists

So you can move between jobs without hunting. The menu is the shop’s map. The top bar is where you sign out, see alerts, and notice whether the business profile exists yet.

---

## Layout

### Phone and small tablets (narrow window)

1. Tap **Menu** (top left) to open the sidebar.
2. Tap a page name.
3. Tap **Close menu**, or tap the dim area beside the menu, to close it.

### Computer and wide windows

The sidebar stays on the left. Click a name to switch screens. The active page is highlighted.

---

## Left menu (what each item is)

Each row has a **label** and a small **detail** line.

| Label | Detail | Open this when you want to… |
|-------|--------|-----------------------------|
| **Counter** | Barcode sale | Bill quickly with a scanner |
| **Sales** | Invoices | Credit sales, returns, collect later, print or share invoice |
| **Customers** | Parties | Add buyers, see what they owe |
| **Dashboard** | Quick pulse | See today’s sales/purchases, stock value, dues |
| **Products** | Catalog | Add items, barcodes, prices, Excel list *(not clerks)* |
| **Inventory** | Stock | Check quantity, fix counts, see history |
| **Purchases** | Bills | Record supplier bills *(not clerks)* |
| **Suppliers** | Parties | Add vendors *(not clerks)* |
| **Reports** | Insights | Excel for accountant, GST, dues *(not clerks)* |
| **Team** | Staff | Invite people *(owners invite; managers can view)* |
| **Activity** | Log | See important actions *(not clerks)* |
| **Business** | Workspace | Shop name, GSTIN, logo *(not clerks)* |
| **Settings** | Prefs | Currency, dates, GST, stock rules *(not clerks)* |
| **Profile** | Account | Your name and password |

Items you are **not allowed** to use are **hidden**. If you type a forbidden address as a clerk, the app sends you to **Counter**.

The bottom of the sidebar shows:

- Shop name, or **No business configured yet**
- Your role in lowercase (`owner access`, `manager access`, `clerk access`)

---

## Top bar

Left side:

- **Welcome back, &lt;first name&gt;**
- A short line: keep today’s inventory decisions visible

Right side:

| Control | What it does |
|---------|----------------|
| **Alerts (N)** | Opens or closes a list of low-stock and overdue-payment warnings. `N` is how many alerts exist. Refreshes about every minute. |
| **Business ready** / **Setup pending** | Whether a business profile exists. Setup pending means go to **Business**. |
| **Sign out** | Ends this session on this browser. You must sign in again. |

### Alerts panel

Click **Alerts**. You may see:

| Kind | Typical title | What you should do |
|------|----------------|--------------------|
| Low stock | “Ultra Cement is low” | Open **Inventory** or **Purchases** and restock |
| Overdue sale | “Payment due from Apex Builders” | Sale date is **more than 7 days ago** and money is still due. Open **Sales**, find the invoice, **Record payment** |
| Overdue purchase | “Payment due to Shakti Cements Ltd” | Same idea for a supplier bill. Open **Purchases** |

If there are none: **No low-stock or overdue payment alerts.**

The panel shows up to 12 lines. Click **Alerts** again to hide it. This is a reminder list, not a separate inbox you “mark as read”.

---

## Email verification banner

If your email is not verified yet, a banner appears above every page:

- **Please verify your email.**
- It names the address the link was sent to.
- You **can keep using the shop** meanwhile.
- **Resend email** sends another link. The button shows **Sending...** while it works.

Open the link on any device. After success, the banner disappears on the next refresh/session update.

---

## Who sees which screens

### Owner

Sees every menu item. Can invite staff, change business and settings, buy, sell, and report.

### Manager

Sees the same operational screens as the owner, including Products, Purchases, Reports, Activity, Business, and Settings.

Does **not** see the **Invite a teammate** form. Can still open **Team** to look at who has access.

Cannot invite, revoke invites, or remove members.

### Clerk

Sees: Dashboard, Counter, Sales, Customers, Products (look up only), Inventory (look up / history only), Profile.

Does **not** see: Purchases, Suppliers, Reports, Activity, Business, Settings. Team is hidden.

**Clerks can:**

- Create and edit customers
- Record sales (including Counter)
- Record customer payments and **sale returns**
- Search products and stock
- Change their own profile

**Clerks cannot:**

- Create or edit products (the create form is hidden)
- Adjust stock counts
- Record purchases
- Cancel a sale (the server refuses it)
- Open reports or the activity log
- Change GST, logo, or shop settings

If a clerk tries a blocked action, they may see an error such as **Your role cannot perform this action**.

---

## Controls you will see on many screens

### Search boxes

Typing filters the list as you pause. Search is usually by **name**, and often also **SKU**, **mobile**, **invoice number**, or **notes** — each screen’s placeholder text tells you.

### Previous / Next

Long lists are split into pages of **25** items. Use **Previous** and **Next**. The label `1–25 of 80` means you are looking at the first twenty-five of eighty records.

### Archive and Restore

**Archive** hides a product, customer, or supplier from the default list. Old invoices still show the name. It does **not** delete history.

Tick **Show archived** to find hidden records, then **Restore** to put them back on the default list.

Do not archive a product you still sell. Archive when you no longer stock it but want history to remain.

### Search-and-select (entity picker)

On Sales and Purchases, **Customer**, **Supplier**, and **Product** are not a simple dropdown of the whole catalog.

1. Click the box.
2. Type part of the name (or SKU for products).
3. Click the matching row.
4. Click outside the list to close it.

If you see **No matches**, create the party on the left of that page, or add the product under **Products**.

### Money and dates

Rupees and date layout follow **Settings** (currency code and date format). Examples: `₹1,234.50` and `31/12/2026`.

### Red error text under a field

That field is invalid. Fix it (required name, phone format, unique SKU, and so on) and save again.

### Green or grey notes after save

Messages such as **Product created.** mean it worked. Read them before clicking away.

### Empty states

If a screen says **Finish business setup**, the business profile is missing. Nothing can be saved until Step 2 in [First day](first-day.md) is done.

---

## Signing out

Click **Sign out** in the top bar when you leave the counter computer, especially if clerks share a machine. The next person should sign in with **their** email.

Do not share the owner password. Invite a **Clerk** instead.

---

## Related

- First-time path: [First day](first-day.md)
- “How do I…?” list: [Everyday jobs](everyday-jobs.md)
