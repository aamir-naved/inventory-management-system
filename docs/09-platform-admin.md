# Platform Super Admin

You (the host operator) log in at **`/platform`**. Shop owners never use this console.

**Screen-by-screen first-day guide:** [user-guide/platform-admin.md](user-guide/platform-admin.md) — setup, sign in, add a shop, suspend, disable users, what to tell the owner.

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

Prod also refuses to start without a real JWT secret, SMTP host, public app URL, a strong database password, `BACKUP_COPY_DIR`, and `UPTIME_ALERT_PHONE`. See the project README.

---

## Console (short)

| Menu | You use it to |
|------|----------------|
| **Dashboard** | Platform-wide shop / user / today’s sales counts |
| **Shops → Add shop** | Name, owner, email, 10-digit phone, optional temp password, starter catalog |
| **Shop detail** | Suspend / activate; email a new owner password |
| **Users** | Disable or enable a login (cannot disable the last platform admin) |
| **Settings** | Read-only `APP_OPEN_REGISTRATION` flag |

Create-shop and reset-owner **never return the password in JSON**. Delivery is email (`passwordDelivery`). Disable and password reset revoke that user’s sessions.

Plans show as `standard` only. No billing yet.

---

## Add a shop (checklist)

1. **Shops** → **Add shop**
2. Shop name, owner name, email, 10-digit phone
3. Optional temporary password (otherwise generated and emailed)
4. Leave starter catalog on unless they already have their own list
5. Credentials are emailed to the owner
6. Send the owner the HTTPS URL and [user-guide/first-day.md](user-guide/first-day.md)

The owner signs in on the normal shop UI and bills from **Counter**. They cannot open `/platform` or another shop’s data.

---

## Suspend / disable

- **Suspend shop** — members keep existing passwords but every shop API with `X-Business-Id` returns 403.
- **Reset owner password** — new temp password emailed to the owner (not returned in the API); sessions end.
- **Users → Disable** — blocks that login and revokes active sessions. You cannot disable the last platform admin.

---

## Backups and rollback

Set `BACKUP_COPY_DIR` off the VPS disk, run `./scripts/install-backup-cron.sh`, and use `./scripts/restore.sh` for a stop → drop → restore → verify recovery.

After a bad deploy: `./scripts/rollback.sh` (or `make rollback`) restores the previous image tag and re-checks health.

Uptime: public `/api/actuator/health`, GitHub Actions `uptime.yml`, and optionally `./scripts/install-uptime-cron.sh` on a second host.
