# Team

**Menu:** Team · **Address:** `/team`  
**Who:** Owners see invite + roster + pending invites. Managers can **view** people with access but cannot invite or remove. Clerks do not see this menu.

---

## Why this screen exists

So the counter does **not** share the owner password. Each person gets their own login and a **role**.

If business is missing: **Finish business setup before inviting staff.**

---

## Roles you can invite

You never invite another **Owner**. There is one owner: the person who registered.

| Role in the dropdown | Choose when | They can |
|----------------------|-------------|---------|
| **Clerk — sales counter** | Billing person, helper | Counter, Sales (no cancel), Customers, view products/stock, Profile, Dashboard |
| **Manager — shop operations** | Person who buys, sets prices, runs reports | Everything except inviting/removing staff and owner-only team actions |

Details: [Around the workspace](workspace.md).

---

## Invite a teammate (owner)

| Field | What to type / choose |
|-------|------------------------|
| **Email** | Their real inbox. Required. The invite is sent here. |
| **Role** | Clerk or Manager (see above). Default is Clerk. |

Click **Send invite**. Success: **Invite sent.** The email appears under **Pending invites**.

The link **expires in 7 days**. If they wait longer, revoke if it is still listed, and send a new invite.

They open the link, set name and password, and join. Guide for them: [Accept invite](sign-in-and-account.md#accept-invite).

If mail is not configured, copy the invite URL from server logs and send it yourself.

Errors: invalid email, or the server message if they already have a pending invite / membership.

---

## People with access

Each member: full name, email, role, and **(removed)** if deactivated.

**Remove access** (owner, not on the owner row): they can no longer use this shop. You cannot remove the owner. You cannot remove yourself.

Removed people stay listed as inactive so you remember who had access.

---

## Pending invites (owner)

Shows email and role until they join.

**Revoke** invalidates the link. Use this if you invited the wrong email or wrong role — then send a new invite.

---

## What not to do

- Do not invite a clerk and expect them to enter purchases — they cannot open Purchases.
- Do not use **Create account** on the login page for staff — that creates a **separate owner** identity, not a seat on this shop. Always invite from Team.
- If they already have a login with that email, they should type **that** password on Accept invite.
- There is no “change role” button. To turn a clerk into a manager: **Remove access**, then invite the same email as Manager. They join again with their existing password.

---

## Related

- Join flow: [Sign-in and account](sign-in-and-account.md)
- What was invited: [Activity](activity.md)
