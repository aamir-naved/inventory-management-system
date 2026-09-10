#!/usr/bin/env bash
# Run Playwright shop-loop E2E against a live Compose stack (UI on :3000).
# Requires open registration so the test can create a fresh shop owner.
set -euo pipefail
cd "$(dirname "$0")/.."

BASE_URL="${E2E_BASE_URL:-http://127.0.0.1:3000}"
HEALTH_URL="${E2E_HEALTH_URL:-${BASE_URL%/}/api/actuator/health}"

export APP_OPEN_REGISTRATION="${APP_OPEN_REGISTRATION:-true}"
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

if ! curl -sf --max-time 5 "$HEALTH_URL" >/dev/null; then
  echo "Stack not healthy at ${HEALTH_URL}. Starting Compose with open registration ..."
  # Avoid colliding with other local Postgres binds when .env sets a busy port.
  export POSTGRES_PORT="${POSTGRES_PORT:-15432}"
  docker compose up --build -d
  for i in $(seq 1 90); do
    if curl -sf --max-time 5 "$HEALTH_URL" >/dev/null; then
      break
    fi
    sleep 2
  done
fi

if ! curl -sf --max-time 5 "$HEALTH_URL" >/dev/null; then
  echo "API health check failed at ${HEALTH_URL}" >&2
  docker compose ps >&2 || true
  exit 1
fi

(cd frontend && npm ci && npx playwright install --with-deps chromium && E2E_BASE_URL="$BASE_URL" npm run test:e2e)
