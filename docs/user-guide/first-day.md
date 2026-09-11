# First day in the shop software

Use this page on the day you open the app for the first time. Follow the steps in order. After this, the rest of the guide is for daily work.

Open the app in a browser on your **phone or any PC**: the HTTPS address your operator gave you, or **http://localhost:3000** on a local install. You do not install Postgres or Docker on the counter computer.

Hindi for the menu and Counter: after you sign in, open the left menu (on a phone, tap **Menu**). At the bottom, **Language** → **हिन्दी**. Other screens stay in English. The choice is remembered on this browser.

---

## Goal for day one

By the end of this page you should be able to:

1. Sign in with the **credentials your operator sent**, or with **mobile OTP** if your shop allows it
2. Open **Counter** (Walk-in and sample hardware items are already there when the shop was set up that way)
3. Record one sale
4. **Share or print** the bill

GST, logo, and a full product list can wait until after the first bill.

**Hosted shops (registration closed):** the operator creates your shop. You do **not** create an account with a new phone number. Use the email and temporary password they sent, or OTP on the phone they stored. Skip Step 2 if you already land on Counter.

**Local / demo (registration open):** a new number can create the owner account. You then type the shop name on Start.

---

## Step 1 — Sign in

### Email (typical for operator-created shops)

1. On the login screen, open **Email**.
2. Type the email and temporary password you were given.
3. Click **Enter shop**. You should land on **Counter**. Change the password from **Profile** when you can.

If you see **Too many login attempts… Try again in a minute**, wait about a minute. The app limits failed passwords so a stranger cannot guess yours.

### Mobile OTP

1. You land on **Enter your mobile number**.
2. Type the 10-digit shop phone (example: `9876543210`).
3. Click **Send OTP**.
4. Type the 6-digit code. On a hosted shop it arrives by SMS when SMS is connected. On a local install, the person who runs the server copies the code from backend logs (`SMS not configured; logging OTP message`).
5. Click **Verify and enter**.

If registration is closed, an unknown number is refused. Ask the operator to add your shop first.

**Email signup** is hidden when registration is closed. Staff still accept invites by email.

Full field help: [Sign-in and account](sign-in-and-account.md).

---

## Step 2 — Name the shop (self-serve only)

Skip this if the operator already created your shop and you landed on Counter.

Until the shop exists, the app sends you to **Start** (`/welcome`). If registration is closed and you have no shop, the screen tells you to contact the operator.

1. **Shop name** — printed on invoices (`Ram Hardware`)
2. **Shop mobile** — usually already filled from your login
3. Click **Open the counter**

That creates the business with GST **off**, a **Walk-in** customer, and five starter items with stock (Cement, TMT bar, paint, switch, PVC pipe). Barcodes match the SKUs (`CEM-001`, `TMT-001`, `PNT-001`, `ELC-001`, `SAN-001`).

The top-right badge should change from **Setup pending** to **Business ready**. You land on **Counter**.

GSTIN, logo, and address: [Business](business.md) when you are ready. GST checklist: [GST on invoices](gst.md).

---

## Step 3 — Optional: shop preferences and your own products

You can bill with the starter items immediately. When you have time:

- **Settings** — default low-stock number; leave **Allow negative stock** off unless you sell before the purchase is entered
- **Products** — add your real catalog, barcodes, HSN, GST %. Opening stock is quantity already in the shop
- Replace or archive starter items you do not sell
- Named credit customers: add them on [Customers](customers.md). If you charge GST and they are in another state, fill **State code** (example `27` for Maharashtra) so the bill uses IGST automatically. Walk-in cash sales stay local tax.

Details: [Settings](settings.md) and [Products](products.md).

**Stock already in the godown?** Put it in **Opening stock** on each product.  
**Empty shop, waiting for a supplier lorry?** Leave opening stock `0`, then record a [Purchase](purchases.md).

---

## Step 4 — Make the first sale

If the Counter shows **You are offline**, reconnect Wi‑Fi or mobile data first. **Complete sale** stays disabled until the phone can reach the server. Do not write the bill on paper and hope it saved — it did not.

### Fast path (cash / barcode)

1. You are on **Counter**. **Walk-in** is selected.
2. Click the **Barcode** box. Scan, or type `CEM-001` and press **Enter** for the sample cement bag.
3. Repeat for more items. Scanning the same barcode again adds 1 to quantity.
4. **Amount paid** — leave blank if they paid the full total; type a smaller number for credit.
5. Click **Complete sale**.
6. Click **Share bill** (phone share sheet or WhatsApp with a downloaded PDF). **Print** if you have a printer.

Success looks like **Saved SAL/2025-26/000001** (the year part follows the Indian financial year, 1 April–31 March). Lines clear for the next customer.

If GST is on, Counter **shows** whether this bill is local (CGST/SGST) or interstate (IGST). There is no tick box. Tax follows the customer’s **State code** vs the shop’s state on [Business](business.md). Walk-in with no state is treated as local.

If share/print fails, open **Sales**, find the invoice, **Share bill** or **Print invoice**.

Details: [Counter](counter.md).

### Full path (credit invoice, many lines, notes)

Use **Sales** instead. Details: [Sales](sales.md).

Named credit parties: create them on [Customers](customers.md) (Walk-in already exists after Step 2).

---

## Step 5 — Confirm it worked

1. **Dashboard** — **Today’s sales** should move (it refreshes right after a Counter sale)
2. **Inventory** — sold product quantity should be lower
3. **Sales** — a sale number such as `SAL/2025-26/000001` appears with status **PAID** or **PARTIAL** / **PENDING**

Restart the phone or PC and open the same URL. The shop is still there because data lives on the server, not on that device.

---

## If you buy stock today (optional on day one)

1. Open **Suppliers**, create the company you buy from (set **State code** if GST is on and they are out of state)
2. Open **Purchases**, choose that supplier, **Add item**, enter quantity and purchase price, then **Record purchase**

That **increases** stock. The product’s **cost price** becomes a weighted average of old stock and this bill, so inventory value stays honest. Details: [Suppliers](suppliers.md) and [Purchases](purchases.md).

---

## If someone else will bill at the counter

Owner only: open **Team**, invite their **email** as **Clerk — sales counter**. They receive a link, set a password, and land on Counter.

Details: [Team](team.md).

---

## Common first-day blocks

| What you see | What to do |
|--------------|------------|
| Sent back to shop name | Complete Step 2 on **Start** |
| OTP never arrives | Wait and retry; on local install read backend logs |
| Number is not registered | Ask the operator to add your shop; public signup may be closed |
| Too many login / OTP attempts | Wait about a minute, then try again |
| Counter: “Choose a customer” | Wait for the list to load, or create `Walk-in` on [Customers](customers.md) |
| Counter: no product for that barcode | Type a starter SKU (`CEM-001`) or put the barcode on the product in [Products](products.md) |
| Counter: **You are offline** | Reconnect, then complete the sale |
| Sale refused because of stock | Buy stock, raise opening stock, or turn on negative stock in [Settings](settings.md) |
| Looking for an Interstate tick box | There isn’t one. Set **State code** on the customer (or supplier) |
| Yellow verify-email banner | Email accounts only — Resend, or copy the link from server logs |
| Menu is missing Products / Purchases | You are logged in as a **clerk** — that is expected |
| **This screen crashed** | Click **Reload page** or **Go home**. Your saved bills are on the server |

When day one is done, keep [Everyday jobs](everyday-jobs.md) bookmarked for “how do I collect payment / return bags / print GST”.
