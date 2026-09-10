# Platform Super Admin

You (the host operator) log in at **`/platform`**. Shop owners never use this console.

Production default: **closed registration**. You create each shop. Self-serve OTP → shop name stays available when `APP_OPEN_REGISTRATION=true` (local/demo).

---

## One-time setup

In `.env` for `SPRING_PROFILES_ACTIVE=prod`:

```bash
APP_OPEN_REGISTRATION=false
APP_PLATFORM_ADMIN_EMAIL=you@example.com
APP_PLATFORM_ADMIN_PASSWORD=choose-a-long-password
```

On first boot the API creates that user as `PLATFORM_ADMIN` (no shop membership). To rotate the password later: set `APP_PLATFORM_ADMIN_RESET=true`, restart once, then turn it off.

Sign in on the shop URL with **Email** (not OTP unless you also stored a phone on that admin user). You land on `/platform`.

---

## Add a shop

1. **Shops** → **Add shop**
2. Shop name, owner name, email, 10-digit phone
3. Optional temporary password (otherwise generated and emailed)
4. Leave starter catalog on unless they already have their own list
5. Credentials are emailed to the owner — the API never returns the password in JSON
6. Send the owner the HTTPS URL (or tell them to use OTP on that phone)

The owner signs in on the normal shop UI and bills from **Counter**. They cannot open `/platform` or another shop’s data.

---

## Suspend / disable

- **Suspend shop** — members keep existing passwords but every shop API with `X-Business-Id` returns 403.
- **Reset owner password** — new temp password emailed to the owner (not returned in the API).
- **Users → Disable** — blocks that login and revokes active sessions. You cannot disable the last platform admin.

Plans show as `standard` only. No billing yet.

Backups: set `BACKUP_COPY_DIR` off the VPS disk, run `./scripts/install-backup-cron.sh`, and use `./scripts/restore.sh` for a stop → drop → restore → verify recovery.
