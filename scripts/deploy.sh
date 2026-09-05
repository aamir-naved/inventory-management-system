#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "Copy .env.example to .env and set SPRING_PROFILES_ACTIVE=prod, APP_JWT_SECRET, SMTP, APP_PUBLIC_APP_URL, and APP_DOMAIN."
  exit 1
fi

# shellcheck disable=SC1091
set -a
source .env
set +a

if [[ "${SPRING_PROFILES_ACTIVE:-}" != "prod" ]]; then
  echo "Set SPRING_PROFILES_ACTIVE=prod in .env before deploying."
  exit 1
fi
if [[ -z "${APP_JWT_SECRET:-}" || ${#APP_JWT_SECRET} -lt 32 ]]; then
  echo "APP_JWT_SECRET must be at least 32 characters."
  exit 1
fi
if [[ -z "${SPRING_MAIL_HOST:-}" || -z "${APP_PUBLIC_APP_URL:-}" || -z "${APP_DOMAIN:-}" || -z "${ACME_EMAIL:-}" ]]; then
  echo "Set SPRING_MAIL_HOST, APP_PUBLIC_APP_URL, APP_DOMAIN, and ACME_EMAIL in .env."
  exit 1
fi

mkdir -p backups
docker compose -f docker-compose.yml -f docker-compose.prod.yml up --build -d
echo "Stack is up. HTTPS is served on ${APP_DOMAIN} (ports 80/443). Postgres is not published on the host."
echo "Take backups with ./scripts/backup.sh"
