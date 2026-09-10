#!/usr/bin/env bash
# Probe the public health URL and alert a phone when the shop app is down.
#
# Run this from somewhere OTHER than the shop VPS (second host, laptop cron, or
# GitHub Actions). If the VPS is dead, a cron job on that same VPS cannot call you.
#
# Required:
#   HEALTH_CHECK_URL   e.g. https://shop.example.com/api/actuator/health
# And at least one alert channel:
#   UPTIME_ALERT_PHONE + APP_SMS_WEBHOOK_URL   (SMS via existing OTP webhook)
#   and/or UPTIME_ALERT_WEBHOOK_URL            (POST JSON; use for Telegram/Pushover/etc.)
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
if [[ -f "$ROOT/.env" ]]; then
  # shellcheck disable=SC1091
  set -a
  # Prefer env already exported (CI / remote host); fill gaps from local .env.
  source "$ROOT/.env"
  set +a
fi

HEALTH_CHECK_URL="${HEALTH_CHECK_URL:-}"
if [[ -z "$HEALTH_CHECK_URL" && -n "${APP_PUBLIC_APP_URL:-}" ]]; then
  HEALTH_CHECK_URL="${APP_PUBLIC_APP_URL%/}/api/actuator/health"
fi

if [[ -z "$HEALTH_CHECK_URL" ]]; then
  echo "Set HEALTH_CHECK_URL (or APP_PUBLIC_APP_URL) to the public /api/actuator/health URL." >&2
  exit 2
fi

UPTIME_FAILURES_BEFORE_ALERT="${UPTIME_FAILURES_BEFORE_ALERT:-2}"
UPTIME_STATE_DIR="${UPTIME_STATE_DIR:-${ROOT}/backups/uptime}"
UPTIME_CURL_TIMEOUT="${UPTIME_CURL_TIMEOUT:-15}"
mkdir -p "$UPTIME_STATE_DIR"
state_file="${UPTIME_STATE_DIR}/state"

has_sms=0
if [[ -n "${UPTIME_ALERT_PHONE:-}" && -n "${APP_SMS_WEBHOOK_URL:-}" ]]; then
  has_sms=1
fi
has_webhook=0
if [[ -n "${UPTIME_ALERT_WEBHOOK_URL:-}" ]]; then
  has_webhook=1
fi

if [[ "$has_sms" -eq 0 && "$has_webhook" -eq 0 ]]; then
  echo "Configure UPTIME_ALERT_PHONE + APP_SMS_WEBHOOK_URL and/or UPTIME_ALERT_WEBHOOK_URL." >&2
  exit 2
fi

read_state() {
  failures=0
  alerted=0
  if [[ -f "$state_file" ]]; then
    # shellcheck disable=SC1090
    source "$state_file"
  fi
}

write_state() {
  cat >"$state_file" <<EOF
failures=$1
alerted=$2
EOF
}

send_alerts() {
  local body="$1"
  local status="$2"
  local sms_payload webhook_payload

  json_escape_sms() {
    if command -v python3 >/dev/null 2>&1; then
      python3 -c 'import json,sys; print(json.dumps({"to": sys.argv[1], "body": sys.argv[2]}))' "$1" "$2"
    else
      printf '{"to":"%s","body":"%s"}' "$1" "$2"
    fi
  }

  json_escape_webhook() {
    if command -v python3 >/dev/null 2>&1; then
      python3 -c 'import json,sys; print(json.dumps({"status": sys.argv[1], "message": sys.argv[2], "url": sys.argv[3]}))' "$1" "$2" "$3"
    else
      printf '{"status":"%s","message":"%s","url":"%s"}' "$1" "$2" "$3"
    fi
  }

  if [[ "$has_sms" -eq 1 ]]; then
    sms_payload="$(json_escape_sms "$UPTIME_ALERT_PHONE" "$body")"
    if ! curl -fsS --max-time 20 -X POST "$APP_SMS_WEBHOOK_URL" \
      -H "Content-Type: application/json" \
      -d "$sms_payload" \
      >/dev/null; then
      echo "SMS alert webhook failed" >&2
    else
      echo "SMS alert sent to $UPTIME_ALERT_PHONE"
    fi
  fi

  if [[ "$has_webhook" -eq 1 ]]; then
    webhook_payload="$(json_escape_webhook "$status" "$body" "$HEALTH_CHECK_URL")"
    if ! curl -fsS --max-time 20 -X POST "$UPTIME_ALERT_WEBHOOK_URL" \
      -H "Content-Type: application/json" \
      -d "$webhook_payload" \
      >/dev/null; then
      echo "Alert webhook failed" >&2
    else
      echo "Webhook alert sent"
    fi
  fi
}

probe() {
  local tmp http_code body
  tmp="$(mktemp)"
  http_code="$(
    curl -s -o "$tmp" -w "%{http_code}" --max-time "$UPTIME_CURL_TIMEOUT" \
      -H "Accept: application/json" \
      "$HEALTH_CHECK_URL" 2>/dev/null || true
  )"
  if [[ -z "$http_code" ]]; then
    http_code="000"
  fi
  body="$(cat "$tmp" 2>/dev/null || true)"
  rm -f "$tmp"

  if [[ "$http_code" != "200" ]]; then
    echo "DOWN http=${http_code}"
    return 1
  fi

  if printf '%s' "$body" | grep -Eqi '"status"[[:space:]]*:[[:space:]]*"UP"'; then
    echo "UP"
    return 0
  fi

  echo "DOWN unexpected body: ${body:0:200}"
  return 1
}

read_state

if probe; then
  if [[ "${alerted:-0}" -eq 1 ]]; then
    send_alerts "IMS recovered: ${HEALTH_CHECK_URL}" "UP"
  fi
  write_state 0 0
  exit 0
fi

failures=$(( ${failures:-0} + 1 ))
echo "Consecutive failures: ${failures}/${UPTIME_FAILURES_BEFORE_ALERT}"

if [[ "$failures" -ge "$UPTIME_FAILURES_BEFORE_ALERT" && "${alerted:-0}" -eq 0 ]]; then
  send_alerts "IMS DOWN: ${HEALTH_CHECK_URL}" "DOWN"
  write_state "$failures" 1
  exit 1
fi

write_state "$failures" "${alerted:-0}"
exit 1
