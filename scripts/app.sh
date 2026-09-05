#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

usage() {
  cat <<'EOF'
Usage: ./scripts/app.sh <command>

  start     Create .env if missing, build images, start Postgres + API + UI
  stop      Stop the stack (shop data in Postgres is kept)
  restart   Rebuild from current code and restart (use after you change the app)
  status    Show container health and URLs
  logs      Follow logs. Ctrl+C stops following; the app keeps running

Same commands via Make: make start | make stop | make restart | make status | make logs
EOF
}

env_value() {
  local key="$1"
  local default="$2"
  local raw=""
  if [[ -f .env ]]; then
    raw="$(grep -E "^[[:space:]]*${key}=" .env | tail -n 1 | cut -d= -f2- || true)"
    raw="${raw%$'\r'}"
    raw="${raw%\"}"
    raw="${raw#\"}"
    raw="${raw%\'}"
    raw="${raw#\'}"
    raw="${raw#"${raw%%[![:space:]]*}"}"
    raw="${raw%"${raw##*[![:space:]]}"}"
  fi
  if [[ -n "${raw}" ]]; then
    printf '%s' "${raw}"
  else
    printf '%s' "${default}"
  fi
}

load_ports() {
  FRONTEND_PORT="$(env_value FRONTEND_PORT 3000)"
  BACKEND_PORT="$(env_value BACKEND_PORT 8080)"
  POSTGRES_PORT="$(env_value POSTGRES_PORT 5432)"
}

print_urls() {
  load_ports
  cat <<EOF

Open the shop UI:     http://localhost:${FRONTEND_PORT}
API (also via UI):    http://localhost:${BACKEND_PORT}/api
Health:               http://localhost:${FRONTEND_PORT}/api/actuator/health
Postgres:             localhost:${POSTGRES_PORT}

EOF
}

require_docker() {
  if ! command -v docker >/dev/null 2>&1; then
    echo "Docker is not installed. Install Docker Desktop, then retry."
    exit 1
  fi
  if ! docker info >/dev/null 2>&1; then
    echo "Docker is installed but not running. Open Docker Desktop, wait until it is idle, then retry."
    exit 1
  fi
}

ensure_env() {
  if [[ ! -f .env ]]; then
    cp .env.example .env
    echo "Created .env from .env.example"
  fi
}

wait_for_stack() {
  load_ports
  local url="http://127.0.0.1:${FRONTEND_PORT}/api/actuator/health"
  echo "Waiting for the API to become healthy at ${url} ..."
  local i
  for i in $(seq 1 90); do
    if curl -sf "${url}" >/dev/null 2>&1; then
      echo "Stack is ready."
      print_urls
      return 0
    fi
    sleep 2
  done
  echo "Containers started, but the health check did not pass yet."
  echo "Check logs with: ./scripts/app.sh logs"
  docker compose ps
  return 1
}

cmd_start() {
  require_docker
  ensure_env
  echo "Building images (first run is slow) and starting Postgres, API, and UI ..."
  docker compose up --build -d
  wait_for_stack
}

cmd_stop() {
  require_docker
  docker compose down
  echo "Stopped. Postgres data is still in the docker volume. Start again with: ./scripts/app.sh start"
}

cmd_restart() {
  require_docker
  ensure_env
  echo "Rebuilding images from current code and restarting ..."
  docker compose up --build -d
  wait_for_stack
  echo "If the browser still shows the old UI, hard-refresh: Cmd+Shift+R (Mac) or Ctrl+Shift+R."
}

cmd_status() {
  require_docker
  docker compose ps
  print_urls
}

cmd_logs() {
  require_docker
  docker compose logs -f --tail=200
}

cmd="${1:-}"
case "${cmd}" in
  start) cmd_start ;;
  stop) cmd_stop ;;
  restart) cmd_restart ;;
  status) cmd_status ;;
  logs) cmd_logs ;;
  -h|--help|help|"") usage ;;
  *)
    echo "Unknown command: ${cmd}"
    echo
    usage
    exit 1
    ;;
esac
