# Direction: small-town cloud register

This is the working plan for making the product survive **real shops** (power cuts, lost PCs, owners who will not run Docker) and still be **faster to first bill** than a generic ERP.

It is not a feature dump. If a task does not make “open a link → sell today → data safe” true, it waits.

Companion: [01-product-vision.md](01-product-vision.md) (10-minute start, mobile-first) and [07-path-to-usable-product.md](07-path-to-usable-product.md) (self-host today).

---

## What we are becoming

**You host one always-on app. The shop owner opens a URL on a phone. They bill in minutes. You keep backups.**

We are **not** becoming Tally, Vyapar-for-every-trade, or an AI assistant. We are a **simple cloud register** for hardware / building-material style shops first.

---

## Definition of done (this plan)

A new owner can:

1. Get a link (no visit to install software on their PC).
2. Sign in with **phone OTP** (email/password still exists for existing users).
3. Type **shop name** (GST later).
4. Land on **Counter** with a **Walk-in** customer and a few sample products with stock.
5. Complete a sale and **share/print** the bill (WhatsApp/share sheet on the phone).
6. Restart their phone or PC — the shop still works because **Postgres is on the server**, with **restart policies** and a **backup script**.

Not in this plan (later): SMS vendor you must pay, Hindi UI, offline queue, WhatsApp Business API, billing/subscriptions. Super Admin (`/platform`) is in the product now; it is how production shops are onboarded.

---

## Phases

| Phase | Outcome | In this implementation |
|-------|---------|------------------------|
| A. Always-on host | Containers come back after reboot; backups are a one-command habit | Yes |
| B. Phone OTP | Owner does not need Gmail | Yes (OTP logged if no SMS webhook) |
| C. 10-minute shop | Name only → Walk-in + starter catalog → Counter | Yes |
| D. Sell-first | Home is Counter, not a metrics wall | Yes |
| E. Share bill | WhatsApp / native share of the PDF | Yes |
| F. Your cloud | One VPS, many shops via existing `business_id` | Ops docs; no per-shop Docker on the counter PC |
| G. Later | MSG91/SMS, Hindi, WhatsApp API, light offline | After 5 live pilots |

---

## What you (the product owner) still do outside the repo

- Rent **one VPS** (India), point a domain, set `.env` `prod`, SMTP (staff invites), optional `APP_SMS_WEBHOOK_URL`.
- Run `./scripts/deploy.sh` and put `./scripts/backup.sh` on **cron**, copying files **off that VPS**.
- Send shops the HTTPS URL. You do not install Postgres on their computer.

Production default is **admin-onboard**: `APP_OPEN_REGISTRATION=false`. You log into `/platform`, add a shop, and send the owner the URL plus email/password (or they use OTP on the phone you stored). Shop data stays isolated by `business_id` in one database.

Self-serve (OTP → shop name → Counter) stays behind `APP_OPEN_REGISTRATION=true` for local and demo. Operator steps: [09-platform-admin.md](09-platform-admin.md).

---

## Success test

Five shops in one trade. Same-day first invoice without you at the keyboard. Next morning they bill again without calling you. You did not SSH for a Windows restart.

---

## Status in this repo

Phases A–E plus Super Admin are implemented: Docker restart policies and `scripts/backup.sh`, phone OTP (`POST /auth/otp/request` + `/verify`), `/welcome` + `POST /businesses/quick-start` when registration is open (Walk-in + starter SKUs as barcodes), Counter as home, **Share bill** on Counter and Sales, `/platform` to create and suspend shops. SMS still logs the OTP until `APP_SMS_WEBHOOK_URL` is set.
