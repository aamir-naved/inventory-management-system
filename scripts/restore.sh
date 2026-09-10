#!/usr/bin/env bash
# Destructive restore: stop writers → drop DB → restore → verify → start app.
set -euo pipefail
cd "$(dirname "$0")/.."

# shellcheck disable=SC1091
source ./scripts/compose-lib.sh
load_dotenv

usage() {
  cat <<'EOF'
Usage: scripts/restore.sh <backup.sql.gz> [--yes]

Stops the API and UI, drops the database, restores from the gzip dump,
checks that public tables exist, then starts the API and UI again.

Postgres stays up. Shop data currently in the volume is replaced.

  --yes   Skip the interactive confirmation (for automation)
EOF
}

file=""
assume_yes=0
for arg in "$@"; do
  case "$arg" in
    -h|--help|help)
      usage
      exit 0
      ;;
    --yes|-y)
      assume_yes=1
      ;;
    *)
      if [[ -z "$file" ]]; then
        file="$arg"
      else
        echo "Unexpected argument: $arg" >&2
        usage >&2
        exit 1
      fi
      ;;
  esac
done

if [[ -z "$file" ]]; then
  usage >&2
  exit 1
fi

if [[ ! -f "$file" ]]; then
  echo "Backup file not found: $file" >&2
  exit 1
fi

checksum_file="${file}.sha256"
if [[ -f "$checksum_file" ]]; then
  expected="$(tr -d '[:space:]' <"$checksum_file")"
  if command -v shasum >/dev/null 2>&1; then
    actual="$(shasum -a 256 "$file" | awk '{print $1}')"
  elif command -v sha256sum >/dev/null 2>&1; then
    actual="$(sha256sum "$file" | awk '{print $1}')"
  else
    actual=""
  fi
  if [[ -n "$actual" && "$expected" != "$actual" ]]; then
    echo "Checksum mismatch for $file" >&2
    echo "Expected: $expected" >&2
    echo "Actual:   $actual" >&2
    exit 1
  fi
  echo "Checksum OK"
fi

require_postgres

db="$(pg_db)"
user="$(pg_user)"

if [[ "$assume_yes" -ne 1 ]]; then
  cat <<EOF
This will REPLACE database "${db}" with the contents of:
  $file

The API and UI will be stopped during the restore.
EOF
  read -r -p "Type YES to continue: " confirm
  if [[ "$confirm" != "YES" ]]; then
    echo "Aborted."
    exit 1
  fi
fi

echo "Stopping API and UI (Postgres stays up) ..."
compose stop backend frontend >/dev/null

cleanup_start=1
restore_ok=0
trap 'if [[ "$cleanup_start" -eq 1 ]]; then echo "Starting API and UI ..."; compose start backend frontend >/dev/null || true; fi' EXIT

echo "Terminating open connections to ${db} ..."
psql_admin -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${db}' AND pid <> pg_backend_pid();" >/dev/null

echo "Dropping and recreating ${db} ..."
psql_admin -c "DROP DATABASE IF EXISTS \"${db}\";"
psql_admin -c "CREATE DATABASE \"${db}\" OWNER \"${user}\";"

echo "Restoring dump ..."
if ! gunzip -c "$file" | compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$user" -d "$db" >/tmp/ims-restore.log 2>&1; then
  echo "Restore failed. Last lines:" >&2
  tail -n 40 /tmp/ims-restore.log >&2 || true
  exit 1
fi

echo "Verifying restore ..."
table_count="$(
  compose exec -T postgres psql -U "$user" -d "$db" -tAc \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE';"
)"
table_count="$(echo "$table_count" | tr -d '[:space:]')"
if [[ -z "$table_count" || "$table_count" -lt 1 ]]; then
  echo "Verify failed: no public tables after restore." >&2
  exit 1
fi

flyway_count="$(
  compose exec -T postgres psql -U "$user" -d "$db" -tAc \
    "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'flyway_schema_history';" \
    2>/dev/null || echo 0
)"
flyway_count="$(echo "$flyway_count" | tr -d '[:space:]')"

echo "Verify OK: ${table_count} public tables$([ "$flyway_count" = "1" ] && echo ", flyway_schema_history present")."
restore_ok=1

echo "Starting API and UI ..."
compose start backend frontend >/dev/null
cleanup_start=0
trap - EXIT

echo "Restore complete."
