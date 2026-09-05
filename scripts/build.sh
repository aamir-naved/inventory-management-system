#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
(cd backend && mvn -q -B package -DskipTests)
(cd frontend && npm ci && npm run build)
echo "Backend jar: backend/target/*.jar"
echo "Frontend dist: frontend/dist"
