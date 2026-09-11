# Platform Super Admin

This page is for the **host operator** — the person who runs the VPS, creates shops, and keeps backups. Shop owners, managers, and clerks should use [First day](first-day.md) instead.

You log in at **`/platform`**. Shop owners never use this console and cannot see another shop’s data.

Production default: **closed registration**. You create each shop. Self-serve OTP → shop name stays available when `APP_OPEN_REGISTRATION=true` (local / demo only).

Env, Docker, and deploy commands: project [README](../../README.md). Numbered ops notes: [09-platform-admin.md](../09-platform-admin.md).

---

## Goal for day one (you, the host)

By the end of this page you should have:

1. Production `.env` with Super Admin email/password, SMTP, JWT secret, public URL, backups, and uptime phone
2. Signed in at `/platform`
3. Created the first shop
4. Sent the owner the HTTPS URL plus how to sign in
5. Confirmed they can open **Counter** (they follow [First day](first-day.md))

---

## One-time setup

In `.env` for `SPRING_PROFILES_ACTIVE=prod`:

```bash
APP_OPEN_REGISTRATION=false
APP_PLATFORM_ADMIN_EMAIL=you@example.com
APP_PLATFORM_ADMIN_PASSWORD=choose-a-long-password
```

Also required in prod (the API will not start without them): a JWT secret of at least 32 characters (not the dev default), `SPRING_MAIL_HOST`, `APP_PUBLIC_APP_URL`, a strong `POSTGRES_PASSWORD` (not `inventory_password`, at least 12 characters), `BACKUP_COPY_DIR` off this VPS disk, and `UPTIME_ALERT_PHONE`.

On first boot the API creates that user as `PLATFORM_ADMIN` (no shop membership). To rotate the password later: set `APP_PLATFORM_ADMIN_RESET=true`, restart once, then turn it off.

Deploy: `./scripts/deploy.sh`. Full table of variables: project README.

---

## Sign in

1. Open the shop URL and choose **Email** (not OTP unless you also stored a phone on this admin user).
2. Use `APP_PLATFORM_ADMIN_EMAIL` and the password from `.env`.
3. You land on `/platform`. The badge reads **PLATFORM_ADMIN**.

If you are sent to Counter or Start, you signed in as a shop user, not the Super Admin.

**Sign out** in the top bar ends this login (and other sessions for this account).

Too many failed passwords: wait a minute (`Too many login attempts…`).

---

## Screens

| Menu | Address | What it is for |
|------|---------|----------------|
| **Dashboard** | `/platform` | Counts across every shop on this host |
| **Shops** | `/platform/shops` | List, search, filter active/suspended |
| **Add shop** | `/platform/shops/new` | Create a tenant and its owner |
| *(shop detail)* | `/platform/shops/:id` | Suspend, activate, reset owner password |
| **Users** | `/platform/users` | Directory; disable a login |
| **Settings** | `/platform/settings` | Read-only flags from the server environment |

Phone: tap **Menu**. Shop data stays isolated — you do not send `X-Business-Id`, and you cannot open a shop’s Counter as this user.

---

## Dashboard

Cards (platform-wide, not one shop’s dashboard):

| Card | Meaning |
|------|---------|
| **Shops** | How many tenants exist |
| **Active** | Can sign in and bill |
| **Suspended** | Blocked at the tenant filter |
| **Users** | Owners, staff, and admins |
| **Sales today** | Non-cancelled invoices (India date) |
| **Today amount** | Sum of those invoice totals |

**Add shop** is a shortcut to the create form.

---

## Add a shop

1. **Shops** → **Add shop** (or the button on Dashboard)
2. Fill:

| Field | Required | What to type |
|-------|----------|----------------|
| **Shop name** | Yes | Printed on their invoices. Example: `Khan General Store` |
| **Owner name** | Yes | The person who will run the shop |
| **Email** | Yes | Where credentials and later invites go |
| **Phone** | Yes | 10-digit number they can use for OTP |
| **Temporary password** | No | Leave blank to generate one. If you type it, at least 8 characters. **Never shown in the API response** — it is emailed |
| **Add Walk-in customer and starter hardware items** | Default on | Cement, TMT, paint, switch, PVC pipe plus Walk-in. Turn **off** only if they already have their own list |

3. Click **Create shop**.
4. You see a success message and **Open shop**. The password is **not** on screen. Delivery is **email**.
5. Send the owner the HTTPS URL (or tell them to use OTP on that phone). Point them at [First day](first-day.md).

The owner signs in on the normal shop UI and bills from **Counter**. They cannot open `/platform` or another shop’s data.

If mail is not working, fix SMTP before onboarding more shops. Prod will not boot without a mail host; local/dev logs a message instead of sending.

---

## Shop list and detail

**Shops** lists each tenant: Active or Suspended, plan (`standard` — billing is not wired), owner name and email.

Search by **name or mobile**. Filter **All / Active / Suspended**. Open a row.

### On the shop page

- Status, suspend reason, mobile, owner name / email / phone, staff seats (including owner)
- **Suspend reason** — optional note (`Non-payment`, `Requested pause`)
- **Suspend shop** — members keep existing passwords but every shop API for that business returns 403. They cannot bill until you activate again.
- **Activate shop** — restores access
- **Reset owner password** — a new temporary password is **emailed** to the owner (not returned in the API). Their other sessions end.

Plans show as `standard` only. No billing yet.

---

## Users

**Users** is a directory of everyone on this host: platform admins and shop members.

Search by name, email, or phone.

| Button | What it does |
|--------|----------------|
| **Disable** | Blocks that login and revokes active sessions. History stays. You cannot disable the last platform admin. |
| **Enable** | Restores the login |

Disable a shop user when you need to cut access without deleting invoices. Prefer **Suspend shop** when the whole tenant should stop billing.

---

## Settings (read-only)

Shows whether **Open registration** is on (self-serve shops) or off (you create shops).

Change it by setting `APP_OPEN_REGISTRATION` in `.env` and restarting the API. Production default is **off**. Do not turn it on for a public URL unless you intend anyone with a phone number to create a shop.

---

## What to tell a new owner

Send a short note with:

1. The HTTPS URL
2. Email + “check your inbox for the temporary password”, or “sign in with OTP on this phone”
3. Link or reminder: after login you land on **Counter**; share or print the bill
4. Change password on **Profile**
5. Hindi: sidebar **Language** → हिन्दी (menu and Counter)

They should **not** use Email signup on a hosted shop (it is hidden when registration is closed). Staff join from **Team** invites, not a second owner account.

---

## Day-to-day ops (not in the console)

These run on the VPS, not from `/platform`.

| Job | Command / note |
|-----|----------------|
| Nightly backup | `./scripts/install-backup-cron.sh` — needs `BACKUP_COPY_DIR` off this machine |
| Manual dump | `./scripts/backup.sh` |
| Restore | `./scripts/restore.sh backups/inventory-….sql.gz` — stop → drop → restore → verify. Destructive. |
| Rollback a bad deploy | `./scripts/rollback.sh` or `make rollback` |
| Health | Public `/api/actuator/health`. Also GitHub Actions uptime + `./scripts/install-uptime-cron.sh` on a **second** host |

Full backup and uptime text: project README.

---

## Common mistakes

| What happened | What to do |
|---------------|------------|
| Created a shop but owner has no password | Check SMTP; the API never returns the temp password in JSON |
| Owner lands on “contact the operator” | Their login has no shop — create the shop first, or they used a different email/phone |
| Suspended shop still “in the list” | Correct — they stay listed as Suspended. Activate to restore billing |
| Disabled the owner instead of suspending | Enable them again, or reset owner password after Enable |
| Turned on open registration in prod | Anyone can create a shop. Set `APP_OPEN_REGISTRATION=false` and restart |
| Trying to bill as Super Admin | You have no shop membership. Sign out and use the owner login, or stay on `/platform` |

---

## Related

- Shop first day: [First day](first-day.md)
- Shop screens: [User guide home](README.md)
- Env, deploy, backups: [README](../../README.md)
- Short technical index: [09-platform-admin.md](../09-platform-admin.md)
