#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# shellcheck disable=SC1091
source ./scripts/compose-lib.sh
load_dotenv

DEPLOY_DIR=".deploy"
mkdir -p "$DEPLOY_DIR"

if [[ ! -f .env ]]; then
  echo "Copy .env.example to .env and set SPRING_PROFILES_ACTIVE=prod, APP_JWT_SECRET, SMTP, APP_PUBLIC_APP_URL, and APP_DOMAIN."
  exit 1
fi

if [[ "${SPRING_PROFILES_ACTIVE:-}" != "prod" ]]; then
  echo "Set SPRING_PROFILES_ACTIVE=prod in .env before deploying."
  exit 1
fi
if [[ -z "${APP_JWT_SECRET:-}" || ${#APP_JWT_SECRET} -lt 32 ]]; then
  echo "APP_JWT_SECRET must be at least 32 characters."
  exit 1
fi
if [[ -z "${POSTGRES_PASSWORD:-}" || "${POSTGRES_PASSWORD}" == "inventory_password" ]]; then
  echo "Set a strong POSTGRES_PASSWORD in .env (not the example value inventory_password)."
  exit 1
fi
if [[ ${#POSTGRES_PASSWORD} -lt 12 ]]; then
  echo "POSTGRES_PASSWORD must be at least 12 characters."
  exit 1
fi
if [[ -z "${SPRING_MAIL_HOST:-}" || -z "${APP_PUBLIC_APP_URL:-}" || -z "${APP_DOMAIN:-}" || -z "${ACME_EMAIL:-}" ]]; then
  echo "Set SPRING_MAIL_HOST, APP_PUBLIC_APP_URL, APP_DOMAIN, and ACME_EMAIL in .env."
  exit 1
fi
if [[ -z "${BACKUP_COPY_DIR:-}" ]]; then
  echo "Set BACKUP_COPY_DIR in .env to a path off this VPS disk (USB mount, second volume, or synced folder)."
  echo "Then run: ./scripts/install-backup-cron.sh"
  exit 1
fi
if [[ ! -d "${BACKUP_COPY_DIR}" ]]; then
  mkdir -p "${BACKUP_COPY_DIR}" || {
    echo "Cannot create BACKUP_COPY_DIR=${BACKUP_COPY_DIR}"
    exit 1
  }
fi
if [[ -z "${UPTIME_ALERT_PHONE:-}" ]]; then
  echo "Set UPTIME_ALERT_PHONE in .env (E.164, e.g. +9198XXXXXXXX) so downtime SMS can reach you."
  echo "Also enable GitHub Actions secrets for ./scripts/uptime-check.sh (see README) or a free external monitor on /api/actuator/health."
  exit 1
fi

PREVIOUS_TAG=""
if [[ -f "$DEPLOY_DIR/current" ]]; then
  PREVIOUS_TAG="$(cat "$DEPLOY_DIR/current")"
fi

IMAGE_TAG="$(date -u +%Y%m%dT%H%M%SZ)"
export IMAGE_TAG

echo "Building and starting stack with IMAGE_TAG=${IMAGE_TAG}..."
compose up --build -d

HEALTH_URL="${APP_PUBLIC_APP_URL%/}/api/actuator/health"
echo "Waiting for healthy: ${HEALTH_URL}"
healthy=0
for _ in $(seq 1 36); do
  if curl -fsS "$HEALTH_URL" >/dev/null 2>&1; then
    healthy=1
    break
  fi
  sleep 5
done

if [[ "$healthy" -ne 1 ]]; then
  echo "Health check failed after deploy."
  if [[ -n "$PREVIOUS_TAG" ]]; then
    echo "Rolling back to IMAGE_TAG=${PREVIOUS_TAG}..."
    IMAGE_TAG="$PREVIOUS_TAG" compose up -d --no-build
    echo "Rollback started. Re-check ${HEALTH_URL}."
  else
    echo "No previous IMAGE_TAG recorded; inspect logs with: docker compose -f docker-compose.yml -f docker-compose.prod.yml logs"
  fi
  exit 1
fi

if [[ -n "$PREVIOUS_TAG" ]]; then
  printf '%s\n' "$PREVIOUS_TAG" >"$DEPLOY_DIR/previous"
fi
printf '%s\n' "$IMAGE_TAG" >"$DEPLOY_DIR/current"

echo "Stack is up on tag ${IMAGE_TAG}. HTTPS is served on ${APP_DOMAIN} (ports 80/443). Postgres is not published on the host."
echo "Rollback: ./scripts/rollback.sh"
echo "Install nightly off-server backups: ./scripts/install-backup-cron.sh"
echo "Manual backup: ./scripts/backup.sh"
echo "Restore (destructive): ./scripts/restore.sh backups/inventory-YYYYMMDDTHHMMSSZ.sql.gz"
echo "Uptime: set GitHub Actions secrets (HEALTH_CHECK_URL, UPTIME_ALERT_PHONE, APP_SMS_WEBHOOK_URL) or run install-uptime-cron.sh on a second host."
echo "Health URL: ${HEALTH_URL}"
echo "Runtime: memory limits + JVM MaxRAMPercentage + log rotation are in docker-compose.prod.yml."
