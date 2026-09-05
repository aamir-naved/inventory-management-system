# Sign-in and account screens

These screens appear **before** the shop menu (except Profile, which is inside the app).

| Address | When you see it |
|---------|-----------------|
| `/login` | Sign in or create the **owner** account |
| `/forgot-password` | Request a reset email |
| `/reset-password?token=…` | Set a new password from the email link |
| `/verify-email?token=…` | Confirm your email from the email link |
| `/accept-invite?token=…` | Join the shop after an owner invites you |

If you are already signed in and you open `/login` or `/forgot-password`, the app sends you to **Counter** (or **Start** if the shop is not named yet). Platform Super Admins go to `/platform`.

On a hosted shop, **Email signup** may be hidden. Then only the operator can create shops; you sign in with the email or mobile they issued.

---

## Sign in / Create account (`/login`)

### Why this screen exists

There is no “guest shop”. The owner signs in with a **mobile OTP** (default) or with email/password. The first successful OTP on a new number creates the owner account. Then you name the shop.

Staff do **not** use **Email signup**. They use the invite link (see [Accept invite](#accept-invite) below).

### What you see

Left: why the shop data lives on a hosted URL, not a counter PC.  
Right: **Mobile OTP**, **Email**, and **Email signup** (signup is hidden when the host has closed public registration).

### Mobile OTP (new shops)

1. **Mobile OTP** is selected by default.
2. **Mobile number** — 10-digit Indian number is enough (example: `9876543210`).
3. Click **Send OTP**.
4. **6-digit code** — from SMS, or from server logs while SMS is not connected.
5. Click **Verify and enter**.

New shops go to **Start** to type the shop name. Returning shops go to **Counter**.

### Sign in (existing email user)

1. Click **Email**.
2. **Email** — the address you registered or were invited with.
3. **Password** — your current password.
4. Eye button — show or hide the password.
5. Click **Enter shop**.

On success you go toward **Counter** (or **Start** if the business is missing). Staff who just accepted an invite also land on Counter.

**Forgot password?** is under the button. Use it if you cannot log in. Password reset is email-only.

#### If sign-in fails

- Wrong email or password — try again; passwords are case-sensitive.
- After a reset, the page may already show **Password reset successful. Sign in with your new password.**

### Email signup (optional)

1. Click **Email signup**.
2. Fill:

| Field | Required | What to type |
|-------|----------|----------------|
| **Full name** | Yes | Your name, max 120 characters. Example: `Priya Sharma` |
| **Email** | Yes | A valid email you can open. |
| **Password** | Yes | **8 to 100** characters. The hint says “Use at least 8 characters.” |

3. Click **Create account** (the button shows **Creating account...** while it works).

You are signed in immediately. Name the shop on **Start**, or use full [Business](business.md) setup if you need GSTIN on day one.

Use a password only you know. Do not reuse the shop Wi-Fi password.

---

## Forgot password (`/forgot-password`)

### Why this screen exists

You cannot see the old password. The app emails a one-time reset link if that email exists.

### What to do

1. From Sign in, click **Forgot password?**
2. **Email** — the account email.
3. Click **Send reset link**.

On success you see a confirmation message and **Back to sign in**. For privacy, the app may say instructions were sent **if the account exists** — you will not get a list of whether the email is registered.

Open the email, click the link. It opens **Choose a new password**.

**Back to sign in** returns to `/login` without sending mail.

If mail is not configured on a local computer, the person who runs Docker must copy the reset URL from backend logs.

---

## Reset password (`/reset-password`)

### Why this screen exists

The email link includes a secret **token**. This page sets a new password and then sends you to Sign in.

### If you see “Reset link is missing”

You opened `/reset-password` without the token (for example you typed the address by hand). Click **Forgot password** and request a new email.

### What to type

| Field | Rule |
|-------|------|
| **New password** | Required, at least 8 characters |

Click **Reset password**. You return to Sign in with the success message. Sign in with the **new** password.

If the token expired or was already used, request a new forgot-password email.

---

## Verify email (`/verify-email`)

### Why this screen exists

The app can send invoices and invites from your identity. Verification proves you control the inbox. You may still run the shop while unverified; the yellow banner stays until this succeeds.

### What happens

You normally arrive by clicking the link in the email. The page shows **Verifying...**, then **Email verified** or **Verification failed**.

- If you are signed in: **Go to the counter**
- If not: **Sign in**

**Verification link is missing or invalid** means the address has no `token`. Use **Resend email** from the banner while signed in, and click the new link.

---

## Accept invite (`/accept-invite`)

### Why this screen exists

The owner (from **Team**) emails a join link. You become a **manager** or **clerk** of **that** shop. You do not create a second business.

### If you see “Invite link is missing”

The URL has no `token`. Ask the owner to send the invite again.

### If the invite is invalid or expired

**This invite is invalid or has expired.** Ask the owner to invite you again (and not to click **Revoke**).

### What to do when the invite loads

The heading looks like: **Join North Star Traders as clerk.**

The text tells you the email the invite was sent to. Use **that** inbox’s password if you already have an account with the same email; otherwise create a password now.

| Field | What to type |
|-------|----------------|
| **Your name** | How you want to appear (required) |
| **Password** | At least 8 characters (required). If you already registered with this email, use your **existing** password. |

Click **Join shop**. You are signed in and sent to **Counter**.

The invite link expires **7 days** after the owner sent it. Ask for a new invite if you see that it is invalid or expired.

Then:

- Clerks stay on Counter / Sales / Customers
- Managers can open Products, Purchases, Reports, and so on

---

## Related

- After owner signup: [Business](business.md)
- Inviting people: [Team](team.md)
- Changing name/password later: [Profile](profile.md)
