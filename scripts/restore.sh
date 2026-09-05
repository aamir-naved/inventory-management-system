#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
file="${1:-}"
if [[ -z "$file" ]]; then
  echo "Usage: scripts/restore.sh backups/inventory-YYYYMMDDTHHMMSSZ.sql.gz"
  exit 1
fi
gunzip -c "$file" | docker compose exec -T postgres psql -U "${POSTGRES_USER:-inventory_user}" "${POSTGRES_DB:-inventory_management}"
echo "Restore complete."
