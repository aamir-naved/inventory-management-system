#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

export SPRING_PROFILES_ACTIVE=desktop
export SERVER_PORT="${SERVER_PORT:-18080}"
export IMS_SKIP_POSTGRES=1
export DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/inventory_management}"
export DB_USERNAME="${DB_USERNAME:-inventory_user}"
export DB_PASSWORD="${DB_PASSWORD:-inventory_password}"
export DB_HOST="${DB_HOST:-127.0.0.1}"
export DB_PORT="${DB_PORT:-5432}"
export APP_PUBLIC_APP_URL="${APP_PUBLIC_APP_URL:-http://127.0.0.1:18080}"

if [[ -z "${APP_DATA_DIR:-}" ]]; then
  if [[ "$(uname -s)" == "Darwin" ]]; then
    export APP_DATA_DIR="$HOME/Library/Application Support/InventoryManagement"
  else
    export APP_DATA_DIR="${XDG_DATA_HOME:-$HOME/.local/share}/inventory-management"
  fi
fi

if [[ ! -d frontend/node_modules ]]; then
  (cd frontend && npm install)
fi
(cd frontend && VITE_API_URL=/api npm run build)
(cd backend && mvn -B -Pdesktop -DskipTests package)

jar="$(ls -1 backend/target/inventory-management-system-*.jar | grep -v original | grep -v sources | head -n 1)"
export IMS_JAR="$PWD/$jar"
if [[ -n "${JAVA_HOME:-}" ]]; then
  export IMS_JAVA_HOME="$JAVA_HOME"
fi

if [[ ! -d desktop/node_modules ]]; then
  (cd desktop && npm install)
fi

echo "Starting the desktop window. Postgres is not bundled here; Docker/local Postgres must already be running."
cd desktop
npm run tauri -- dev
