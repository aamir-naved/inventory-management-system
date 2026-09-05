#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
(cd backend && mvn -q -B test)
(cd frontend && npm ci && npm test && npm run build)
