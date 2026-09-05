# Windows desktop app (Phase 1)

This is the **installable Windows register**: a Setup.exe that runs the existing React UI, Spring Boot API, and a private PostgreSQL on the shop PC. Billing works with **no internet**. Cloud pair/sync is **not** in this phase.

Web/VPS Compose remains the hosted product. Desktop is a second distribution of the same codebase.

## What the shop owner does

1. Download `Inventory Management_0.1.0_x64-setup.exe` from the GitHub Actions **Desktop Windows** artifact (or a release you publish).
2. Double-click the installer. Windows SmartScreen will warn until you buy a code-signing certificate — choose **More info** → **Run anyway**.
3. Open **Inventory Management** from the Start Menu or desktop shortcut.
4. Wait for **Starting the shop…**, then sign up / sign in (email + password; OTP still exists but SMS is not configured on a PC).
5. Type the shop name on Start, sell from Counter.
6. Close the window to stop the local engine. Open it again — data is still there.

Data directory (survives reinstall unless you delete it):

`%LOCALAPPDATA%\InventoryManagement`

Logs: `%LOCALAPPDATA%\InventoryManagement\logs\`

## Backup and restore

On **Settings** (owner/manager):

- **Export backup** downloads `inventory-backup-….sql.gz` (a `pg_dump` of the local database). Keep it on a USB drive.
- **Queue restore** uploads that file, then **close and reopen** the app. Restore runs before Flyway on the next start.

Do not sell from the phone cloud **and** this PC until two-way sync exists. This PC is the writer.

## What the installer contains

| Piece | Role |
|-------|------|
| Tauri 2 window (WebView2) | Splash, then `http://127.0.0.1:18080/` |
| jlink JRE 17 | Runs the Spring Boot fat JAR |
| Spring `desktop` profile | Binds localhost only, generates JWT into `desktop.properties`, serves the SPA from the JAR, no SMTP required |
| Portable PostgreSQL | `initdb` into `%LOCALAPPDATA%\InventoryManagement\pgdata`, port **54329** |

The API stays at `/api`. Static UI is served by a Tomcat valve on `/` so the window is same-origin.

## How we build the `.exe`

GitHub Actions workflow [desktop-windows.yml](../.github/workflows/desktop-windows.yml) runs on `windows-latest` and uploads **InventoryManagement-Setup**.

Locally on a Windows machine (JDK 17, Node 22, Rust, Maven):

```powershell
./scripts/package-desktop-windows.ps1
```

A Mac cannot produce the NSIS installer. For UI/engine work on macOS, with Docker Postgres already up:

```bash
./scripts/desktop-dev.sh
```

That builds the SPA into the JAR, starts the `desktop` profile against local Postgres (`IMS_SKIP_POSTGRES=1`), and opens the Tauri window.

## Later phases (not this ship)

- **Phase 2:** Pair this PC with the VPS (`/sync/pair`, snapshot, push). Cloud is backup; a new PC can restore from cloud.
- **Phase 3:** Two-way sync. Replay sales and `inventory_movements`. Never last-write-wins on `current_stock`.

See [03-product-backlog.md](03-product-backlog.md) epic **Windows desktop**.
