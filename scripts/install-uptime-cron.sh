#!/usr/bin/env bash
# Install a cron probe on THIS machine (use a watchdog host, not only the shop VPS).
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ -f .env ]]; then
  # shellcheck disable=SC1091
  set -a
  source .env
  set +a
fi

HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-}"
if [[ -z "$HEALTH_CHECK_URL" && -n "${APP_PUBLIC_APP_URL:-}" ]]; then
  HEALTH_CHECK_URL="${APP_PUBLIC_APP_URL%/}/api/actuator/health"
fi

if [[ -z "$HEALTH_CHECK_URL" ]]; then
  echo "Set HEALTH_CHECK_URL or APP_PUBLIC_APP_URL first." >&2
  exit 1
fi

if [[ -z "${UPTIME_ALERT_PHONE:-}" || -z "${APP_SMS_WEBHOOK_URL:-}" ]]; then
  if [[ -z "${UPTIME_ALERT_WEBHOOK_URL:-}" ]]; then
    echo "Set UPTIME_ALERT_PHONE + APP_SMS_WEBHOOK_URL (or UPTIME_ALERT_WEBHOOK_URL)." >&2
    exit 1
  fi
fi

repo="$(pwd)"
mkdir -p backups/uptime
interval="${UPTIME_CRON_SCHEDULE:-*/5 * * * *}"
cron_line="${interval} cd ${repo} && ./scripts/uptime-check.sh >> ${repo}/backups/uptime/check.log 2>&1"

existing="$(crontab -l 2>/dev/null || true)"
filtered="$(printf '%s\n' "$existing" | grep -v 'scripts/uptime-check.sh' || true)"
{
  printf '%s\n' "$filtered"
  printf '%s\n' "$cron_line"
} | grep -v '^$' | crontab -

cat <<EOF
Installed uptime cron:
  $cron_line

Health URL: $HEALTH_CHECK_URL

IMPORTANT: Prefer a second machine or GitHub Actions (.github/workflows/uptime.yml).
A probe on the same VPS cannot alert you if that VPS is offline.

Also configure a free external monitor (UptimeRobot / Better Stack) against:
  $HEALTH_CHECK_URL
with SMS or phone-push alerts as a second path.
EOF
