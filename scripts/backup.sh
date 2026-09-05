#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p backups
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="backups/inventory-${stamp}.sql.gz"
docker compose exec -T postgres pg_dump -U "${POSTGRES_USER:-inventory_user}" "${POSTGRES_DB:-inventory_management}" | gzip > "$file"
echo "Wrote $file"

# Optional: copy off this machine so a dead VPS is not the only copy.
# Example: BACKUP_COPY_DIR=/mnt/backup-disk ./scripts/backup.sh
# Example cron (on the VPS, not the shop PC):  15 2 * * * cd /opt/inventory-management-system && ./scripts/backup.sh
if [[ -n "${BACKUP_COPY_DIR:-}" ]]; then
  mkdir -p "$BACKUP_COPY_DIR"
  cp "$file" "$BACKUP_COPY_DIR/"
  echo "Copied to $BACKUP_COPY_DIR"
fi
