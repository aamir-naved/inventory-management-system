#!/usr/bin/env bash
# Shared helpers for backup/restore. Source from other scripts after cd to repo root.
# shellcheck shell=bash

load_dotenv() {
  if [[ -f .env ]]; then
    # shellcheck disable=SC1091
    set -a
    source .env
    set +a
  fi
}

compose() {
  if [[ "${SPRING_PROFILES_ACTIVE:-}" == "prod" && -f docker-compose.prod.yml ]]; then
    docker compose -f docker-compose.yml -f docker-compose.prod.yml "$@"
  else
    docker compose "$@"
  fi
}

pg_user() {
  printf '%s' "${POSTGRES_USER:-inventory_user}"
}

pg_db() {
  printf '%s' "${POSTGRES_DB:-inventory_management}"
}

require_postgres() {
  if ! compose ps --status running --services 2>/dev/null | grep -qx postgres; then
    echo "Postgres is not running. Start the stack first (./scripts/app.sh start or ./scripts/deploy.sh)."
    exit 1
  fi
}

psql_db() {
  compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$(pg_user)" -d "$(pg_db)" "$@"
}

psql_admin() {
  compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$(pg_user)" -d postgres "$@"
}
