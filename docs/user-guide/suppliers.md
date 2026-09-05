# Suppliers

**Menu:** Suppliers · **Address:** `/suppliers`  
**Who:** Owner and manager. Hidden for clerks (clerks do not record purchases).

---

## Why this screen exists

A **supplier** is a party you **buy from**. You need at least one before you can save a purchase bill.

This screen stores the name and contact, and when you **Open** a supplier it shows **how much you still owe** and every purchase bill for them.

You can also create a supplier quickly from the left side of **Purchases**. Use **Suppliers** when you want the full list, archive, and outstanding view.

---

## Create / edit (left)

Heading: **Create supplier** or **Edit supplier**.

| Field | Required | What to type |
|-------|----------|----------------|
| **Supplier name** | Yes | Company or person, max 150 characters. Example: `Shakti Cements Ltd`. |
| **Contact person** | No | Who you call, max 120 characters. Example: `Rahul Mehta`. |
| **Mobile number** | No | 7–20 phone characters if filled. Example: `+91 9876543210`. |
| **Address** | No | Max 255 characters. Example: `Industrial Road`. |

Click **Create supplier** or **Save supplier**.

**New supplier** clears the form after you have opened someone.

---

## List (right)

- **Search suppliers** — name, contact, or mobile
- **Show archived** — include hidden suppliers
- Cards show name, contact, mobile, address, **Active** / **Archived**

| Button | What it does |
|--------|----------------|
| **Open** | Loads the form **and** shows outstanding + purchase history below |
| **Archive** | Hides from the default list and from new-bill pickers |
| **Restore** | Brings them back |

Empty: **No suppliers yet** — create one so you can track purchases and dues.

---

## After you Open a supplier

### Outstanding balance

- **Bills** — how many purchase bills
- **Billed** — total billed
- **Paid** — what you have paid
- **Due** — still payable

Payments are recorded on **Purchases**, not on this screen. A link reminds you.

### Purchase history

Every bill for this supplier, including **Cancelled** ones.

Each card: purchase number, date, status (`PENDING` / `PARTIAL` / `PAID` or **Cancelled**), total, returned (if any), net, paid, due.

This is a **view**. To pay or return, open **Purchases**, search the purchase number, **View details**.

---

## When to archive

Archive a supplier you no longer buy from. Do not archive if you still owe them — you can still pay from Purchases, but they disappear from the default picker.

---

## Related

- Recording bills: [Purchases](purchases.md)
- Dues Excel: [Reports](reports.md) → Supplier dues
