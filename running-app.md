# Running the app on your machine

This is the operator cheat sheet: one command starts Postgres, the API, and the shop UI. Product docs stay in [README.md](README.md).

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
| Start the whole stack | `./scripts/start.sh` |
| Rebuild + restart after code changes | `./scripts/restart.sh` |
| Stop (keeps shop data) | `./scripts/stop.sh` |
| Are things up? | `./scripts/app.sh status` or `make status` |
| Follow logs | `./scripts/app.sh logs` or `make logs` |
| Run tests | `./scripts/test.sh` or `make test` |

`Ctrl+C` in the logs command only stops **following** logs. The app keeps running.

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
