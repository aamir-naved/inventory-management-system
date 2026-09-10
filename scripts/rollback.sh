#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# shellcheck disable=SC1091
source ./scripts/compose-lib.sh
load_dotenv

DEPLOY_DIR=".deploy"
PREVIOUS_FILE="$DEPLOY_DIR/previous"
CURRENT_FILE="$DEPLOY_DIR/current"

if [[ ! -f "$PREVIOUS_FILE" ]]; then
  echo "No previous deploy tag in ${PREVIOUS_FILE}."
  echo "Deploy at least twice with ./scripts/deploy.sh before rolling back."
  exit 1
fi

PREVIOUS_TAG="$(cat "$PREVIOUS_FILE")"
CURRENT_TAG=""
if [[ -f "$CURRENT_FILE" ]]; then
  CURRENT_TAG="$(cat "$CURRENT_FILE")"
fi

if [[ -z "$PREVIOUS_TAG" ]]; then
  echo "Previous deploy tag is empty."
  exit 1
fi

echo "Rolling back from ${CURRENT_TAG:-unknown} to ${PREVIOUS_TAG}..."
export IMAGE_TAG="$PREVIOUS_TAG"
compose up -d --no-build

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
  echo "Health check failed after rollback. Inspect logs."
  exit 1
fi

if [[ -n "$CURRENT_TAG" ]]; then
  printf '%s\n' "$CURRENT_TAG" >"$PREVIOUS_FILE"
fi
printf '%s\n' "$PREVIOUS_TAG" >"$CURRENT_FILE"

echo "Rollback complete. Current IMAGE_TAG=${PREVIOUS_TAG}."
