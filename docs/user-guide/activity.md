# Activity

**Menu:** Activity · **Address:** `/audit`  
**Who:** Owner and manager. Hidden for clerks.

---

## Why this screen exists

A simple log of **important shop actions**: who did what, in words. It is not a full accounting audit trail of every field edit. Use it when you need “did someone invite X?” or “when was this sale created?”

If business is missing: **Finish business setup first.**

---

## What you see

Title: **Who changed what, and when.**

A list of cards, newest first, 25 per page (**Previous** / **Next**).

Each card:

- **Summary** — human sentence (example: `Sale SAL/2025-26/000001 for Apex Builders`, `Invited aisha@shop.com as CLERK`)
- **Action** code · **date and time** (formatted per Settings)

There is no search box. Scroll or page through.

---

## Actions you may see

| Action (typical) | Meaning |
|------------------|---------|
| `SALE_CREATED` | A sale was recorded (including Counter) |
| Sale cancelled (similar cancel action) | An invoice was voided |
| Purchase created / cancelled | Buy-side equivalents |
| `STAFF_INVITED` | Owner sent an invite |
| `STAFF_INVITE_REVOKED` | Invite link cancelled |
| `STAFF_DEACTIVATED` | **Remove access** was used |

Exact wording follows the summary line on the card.

This log does **not** list every product name change or every stock adjustment. For product quantity history, open the product on [Inventory](inventory.md).

---

## Related

- Staff: [Team](team.md)
- Bills: [Sales](sales.md) / [Purchases](purchases.md)
