#!/usr/bin/env bash
# Install a nightly cron job that runs scripts/backup.sh (requires BACKUP_COPY_DIR).
set -euo pipefail
cd "$(dirname "$0")/.."

# shellcheck disable=SC1091
source ./scripts/compose-lib.sh
load_dotenv

if [[ -z "${BACKUP_COPY_DIR:-}" ]]; then
  echo "Set BACKUP_COPY_DIR in .env (or the environment) before installing cron." >&2
  echo "Example: BACKUP_COPY_DIR=/mnt/shop-backups" >&2
  exit 1
fi

if [[ ! -d "$BACKUP_COPY_DIR" ]]; then
  mkdir -p "$BACKUP_COPY_DIR" || {
    echo "Cannot create BACKUP_COPY_DIR=$BACKUP_COPY_DIR" >&2
    exit 1
  }
fi

repo="$(pwd)"
log_dir="${BACKUP_DIR:-backups}"
mkdir -p "$log_dir"
cron_line="15 2 * * * cd ${repo} && BACKUP_COPY_DIR=${BACKUP_COPY_DIR} BACKUP_RETENTION_DAYS=${BACKUP_RETENTION_DAYS:-14} ./scripts/backup.sh >> ${repo}/${log_dir}/backup.log 2>&1"

existing="$(crontab -l 2>/dev/null || true)"
filtered="$(printf '%s\n' "$existing" | grep -v 'scripts/backup.sh' || true)"
{
  printf '%s\n' "$filtered"
  printf '%s\n' "$cron_line"
} | grep -v '^$' | crontab -

echo "Installed nightly backup cron (02:15 UTC):"
echo "  $cron_line"
echo
echo "Confirm with: crontab -l"
echo "Off-server directory: $BACKUP_COPY_DIR"
