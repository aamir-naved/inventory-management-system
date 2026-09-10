#!/usr/bin/env bash
# Dump Postgres, copy off this machine, prune old dumps.
# Prod refuses to keep the only copy on the VPS disk.
set -euo pipefail
cd "$(dirname "$0")/.."

# shellcheck disable=SC1091
source ./scripts/compose-lib.sh
load_dotenv
require_postgres

if [[ -z "${BACKUP_COPY_DIR:-}" ]]; then
  if [[ "${SPRING_PROFILES_ACTIVE:-}" == "prod" ]]; then
    cat <<'EOF' >&2
BACKUP_COPY_DIR is required in prod so dumps leave this machine.

  # Example — USB / second disk mounted on the VPS:
  # BACKUP_COPY_DIR=/mnt/shop-backups

  Set it in .env, then:
  ./scripts/install-backup-cron.sh
EOF
    exit 1
  fi
  echo "WARNING: BACKUP_COPY_DIR unset — dump stays on this machine only. Set it before going live." >&2
fi

mkdir -p "$BACKUP_DIR"
stamp="$(date -u +%Y%m%dT%H%M%SZ)"
base="inventory-${stamp}.sql.gz"
file="${BACKUP_DIR}/${base}"
checksum_file="${file}.sha256"

echo "Dumping $(pg_db) ..."
compose exec -T postgres \
  pg_dump -U "$(pg_user)" --clean --if-exists --no-owner --no-acl "$(pg_db)" \
  | gzip -c >"$file"

if [[ ! -s "$file" ]]; then
  echo "Backup file is empty: $file" >&2
  rm -f "$file"
  exit 1
fi

if command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$file" | awk '{print $1}' >"$checksum_file"
elif command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$file" | awk '{print $1}' >"$checksum_file"
else
  echo "Neither shasum nor sha256sum found; skipping checksum." >&2
  checksum_file=""
fi

echo "Wrote $file ($(du -h "$file" | awk '{print $1}'))"

if [[ -n "${BACKUP_COPY_DIR:-}" ]]; then
  mkdir -p "$BACKUP_COPY_DIR"
  cp "$file" "$BACKUP_COPY_DIR/"
  if [[ -n "$checksum_file" && -f "$checksum_file" ]]; then
    cp "$checksum_file" "$BACKUP_COPY_DIR/"
  fi
  remote="${BACKUP_COPY_DIR}/${base}"
  if [[ ! -s "$remote" ]]; then
    echo "Off-server copy failed: $remote" >&2
    exit 1
  fi
  if [[ -n "$checksum_file" && -f "$checksum_file" ]]; then
    expected="$(cat "$checksum_file")"
    if command -v shasum >/dev/null 2>&1; then
      actual="$(shasum -a 256 "$remote" | awk '{print $1}')"
    else
      actual="$(sha256sum "$remote" | awk '{print $1}')"
    fi
    if [[ "$expected" != "$actual" ]]; then
      echo "Checksum mismatch after copy to $BACKUP_COPY_DIR" >&2
      exit 1
    fi
  fi
  echo "Copied to $BACKUP_COPY_DIR"
fi

prune_dir() {
  local dir="$1"
  [[ -d "$dir" ]] || return 0
  find "$dir" -maxdepth 1 -type f \( -name 'inventory-*.sql.gz' -o -name 'inventory-*.sql.gz.sha256' \) \
    -mtime "+${BACKUP_RETENTION_DAYS}" -print -delete 2>/dev/null || true
}

echo "Pruning dumps older than ${BACKUP_RETENTION_DAYS} days ..."
prune_dir "$BACKUP_DIR"
if [[ -n "${BACKUP_COPY_DIR:-}" ]]; then
  prune_dir "$BACKUP_COPY_DIR"
fi

echo "Backup complete."
