# Running the app on your machine

This is the operator cheat sheet: one command starts Postgres, the API, and the shop UI. Product docs stay in [README.md](README.md). After the UI is up, shop screens: [docs/user-guide/first-day.md](docs/user-guide/first-day.md). If you host shops for others: [docs/user-guide/platform-admin.md](docs/user-guide/platform-admin.md).

You need **Docker Desktop** (or Docker Engine + Compose v2). Open Docker and wait until it is idle before the first start.

---

## Start everything (run this once)

From the repo root:

```bash
./scripts/start.sh
```

The same thing: `./scripts/app.sh start` or `make start`.

What it does:

1. Copies `.env.example` to `.env` if you do not already have `.env`
2. Builds the backend and frontend images
3. Starts **Postgres**, the **Spring Boot API**, and the **nginx UI**
4. Waits until `http://localhost:3000/api/actuator/health` is healthy

Then open **http://localhost:3000**

| What | Where |
|------|--------|
| Shop UI | http://localhost:3000 |
| API | http://localhost:8080/api (also `http://localhost:3000/api`) |
| Health | http://localhost:3000/api/actuator/health |
| Postgres | `localhost:5432` unless you changed `POSTGRES_PORT` in `.env` |

The first build can take several minutes. Later starts reuse cached images and are much faster.

Local / demo (`dev` profile) has **open registration**: a new mobile number can create the owner account, then name the shop on Start. Production is the opposite — you create shops at `/platform` (see the Super Admin guide).

---

## First bill on this machine

1. Open **http://localhost:3000**. **Mobile OTP** is the default.
2. Type a 10-digit number → **Send OTP**. SMS is usually not connected locally, so copy the 6-digit code from logs:

   ```bash
   make logs
   ```

   Look for the OTP log line (the code is not printed in production SMS mode).
3. **Verify and enter** → type the **shop name** on Start → **Open the counter**.
4. On **Counter**, type `CEM-001` and press Enter (sample cement), **Complete sale**, then **Share bill** or **Print**.

Email verify / invite / password-reset links also go to logs when SMTP is unset (`Mail not configured; logging outbound message`).

Full click-through: [docs/user-guide/first-day.md](docs/user-guide/first-day.md).

---

## After you change the code

A browser refresh is **not** enough. The UI is a built SPA inside the frontend image, so source changes only appear after a rebuild.

```bash
./scripts/restart.sh
```

The same thing: `./scripts/app.sh restart` or `make restart`.

Then hard-refresh the browser so it does not keep an old `index.html`:

- Mac: **Cmd+Shift+R**
- Windows/Linux: **Ctrl+Shift+R**

Use this after dashboard, backend, or any other app change.

---

## Everyday commands

| What you want | Command |
|---------------|---------|
| Start the whole stack | `./scripts/start.sh` or `make start` |
| Rebuild + restart after code changes | `./scripts/restart.sh` or `make restart` |
| Stop (keeps shop data) | `./scripts/stop.sh` or `make stop` |
| Are things up? | `./scripts/app.sh status` or `make status` |
| Follow logs | `./scripts/app.sh logs` or `make logs` |
| Backend + frontend unit tests | `./scripts/test.sh` or `make test` |
| Counter shop-loop E2E (Playwright) | `./scripts/e2e.sh` or `make e2e` |

`Ctrl+C` in the logs command only stops **following** logs. The app keeps running.

E2E needs the stack healthy and **open registration**. If Compose is not up, `e2e.sh` starts it (and may use Postgres host port `15432` to avoid a clash).

Prod deploy / rollback / backups are not this file — see the README (`./scripts/deploy.sh`, `make rollback`, `./scripts/backup.sh`).

---

## Stop, data, and a full wipe

```bash
./scripts/stop.sh
```

That is `docker compose down`. Containers go away. **The Postgres volume stays**, so shops, stock, and sales are still there the next time you start.

To throw the database away and start empty (cannot be undone):

```bash
docker compose down -v
./scripts/start.sh
```

---

## If start fails

1. **Docker is not running** — open Docker Desktop and wait until it is idle.
2. **Port already in use** — something else is bound to 3000, 8080, or 5432. Stop that process, or change `FRONTEND_PORT`, `BACKEND_PORT`, or `POSTGRES_PORT` in `.env` and run `./scripts/restart.sh`.
3. **Health check timed out** — run `./scripts/app.sh logs` and look at the `backend` service. The first Java boot after a rebuild can take a minute.
4. **UI looks old after restart** — hard-refresh the browser (Cmd+Shift+R). If it is still old, run `./scripts/restart.sh` again.
5. **OTP never arrives** — there is no SMS locally unless you set `APP_SMS_WEBHOOK_URL`. Copy the code from `make logs`.
6. **Too many login / OTP attempts** — wait about a minute; auth is rate-limited.

---

## Host processes (optional, no Compose UI)

Use this only if you want Vite hot reload on **http://localhost:5173**. You still need Postgres (Compose Postgres is enough).

```bash
# terminal 1 — keep the database
docker compose up -d postgres

# terminal 2 — API at http://localhost:8080/api
cd backend
DB_URL=jdbc:postgresql://localhost:${POSTGRES_PORT:-5432}/inventory_management \
DB_USERNAME=inventory_user \
DB_PASSWORD=inventory_password \
mvn spring-boot:run

# terminal 3 — UI at http://localhost:5173
cd frontend
npm install
npm run dev
```

If Compose published Postgres on another host port (see `./scripts/app.sh status`), put that port in `DB_URL`.

The Vite app calls `http://localhost:8080/api` unless you set `VITE_API_URL`. CORS already allows `http://localhost:5173`.

For day-to-day product work, prefer `./scripts/start.sh` and `./scripts/restart.sh` so UI, API, and database stay in sync.
