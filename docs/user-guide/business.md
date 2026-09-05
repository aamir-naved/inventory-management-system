# Business

**Menu:** Business · **Address:** `/business-setup`  
**Who:** Owner and manager. Hidden for clerks.

---

## Why this screen exists

Every product, bill, and invoice belongs to **one shop profile**. New owners usually type the shop name on **Start** (`/welcome`) first. Use this screen when you need GSTIN, address, logo, or to change the name that prints on PDFs.

The same screen is used later to **change** the name, GSTIN, address, or logo that print on PDFs.

You create the business **once**. After that the button says **Save changes**.

---

## When to open it

- First day, after you already billed, when you want GSTIN and a logo
- Or instead of **Start**, if you want the full form before any sale
- Shop renamed, moved, or new mobile number
- You registered for GST and now need GSTIN on invoices
- You have a logo file to print on bills
- You need to switch between tax-inclusive and tax-exclusive prices

GST switches also appear under **Settings**. Changing them in either place updates the shop.

---

## Before you start

Have ready:

- Trading name (as customers should see it)
- Shop phone
- If GST-registered: GSTIN, state name, and 2-digit state code (the first two characters of GSTIN)

---

## What you see

Title: **Set up the business that this workspace belongs to.**

The form heading is **Create your business profile** or **Update business details**.

---

## Fields

### Always shown

| Field | Required | What to type / choose |
|-------|----------|------------------------|
| **Business name** | Yes | Shop name, max 150 characters. Example: `North Star Traders`. Printed on invoices. |
| **Business type** | Yes | Free text, max 100 characters. Example: `Hardware Store`. For your own labelling, not a legal dropdown. |
| **Address** | No | Street / market line, max 255 characters. Example: `42 Market Road`. Useful on PDFs. |
| **Mobile number** | Yes | 7–20 characters: digits, spaces, `+`, `-`, `(`, `)`. Example: `+91 9876543210`. Must not be empty. |
| **Currency** | Yes | Exactly **3 letters**, stored uppercase. Use `INR` for India. This is how money is labelled in the app. |
| **Time zone** | Yes | IANA name, default `Asia/Kolkata`. This is how **today** is calculated on the Dashboard. Do not type `IST` or `GMT+5:30` — use `Asia/Kolkata`. |

### GST block (only after you tick the box)

Tick **Enable GST on invoices and bills** if this shop charges GST.

Then fill:

| Field | What to type / choose |
|-------|------------------------|
| **GSTIN** | Up to 15 characters, forced to uppercase as you type. Example: `22AAAAA0000A1Z5`. Printed on invoices when GST is on. |
| **State code** | Up to 2 characters, uppercase. Example: `29` for Karnataka, `27` for Maharashtra. Used with interstate (IGST) vs local (CGST+SGST). |
| **State** | State name. Example: `Karnataka`. |
| **Prices include GST** | Tick if the **selling and purchase prices you type already include tax**. Leave **off** if you want the app to **add** GST on top of the rate (exclusive pricing). |

**When to tick interstate later (on a sale or purchase):** the other party is in a **different state** than **State code** here. Tick **Interstate** on that bill so IGST is used instead of CGST/SGST.

**When to tick inclusive prices:** your rack rate is `₹118` for an 18% item and you do not want the bill to become `₹118 + 18%`. If your rate is `₹100` plus 18% = `₹118` on the invoice, leave inclusive **off**.

### Logo (only after the business already exists)

| Field | Rule |
|-------|------|
| **Shop logo** | File picker. **PNG or JPEG only**, maximum **512 KB**. Appears on sale invoices and purchase bills. |

After you choose a file it uploads immediately (you do not need **Save changes** for the file itself). You should see a note that the logo will appear on invoices.

If a logo is already stored, **Remove logo** deletes it from future PDFs.

You cannot upload a logo on the very first save. Click **Create business** first, then choose the file.

---

## Buttons

| Button | What it does |
|--------|----------------|
| **Create business** | First time only. Creates the shop and attaches it to your login. |
| **Save changes** | Updates name, GST, address, and so on. |
| **Remove logo** | Clears the logo. |

While saving, the button shows **Saving...**.

---

## After you save

- Feedback: **Business details saved successfully.**
- Sidebar shows the new name.
- Badge becomes **Business ready**.
- Dashboard metrics can load.
- Other screens stop showing the empty “finish setup” page.

---

## What not to do

- Do not create a second owner account expecting a second shop in the same deployment. This product is **one shop per installation**.
- Do not put a random currency like `RS` — it must be three letters (`INR`).
- Do not enable GST and leave GSTIN blank if you need it on the PDF; fill GSTIN and state so invoices look complete.
- Changing **Prices include GST** does not rewrite old bills; it applies to **new** sales and purchases.

---

## Related

- Preferences that sit next to this: [Settings](settings.md)
- Logo on paper: [Sales](sales.md) (Print invoice) and [Purchases](purchases.md) (Print bill)
- First-day order: [First day](first-day.md)
