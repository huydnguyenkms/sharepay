#!/usr/bin/env bash
#
# Build and (re)start the production stack. Run from anywhere inside the repo on the server.
# Requires a .env file at the repo root (copy .env.prod.example -> .env first).
set -euo pipefail

# cd to repo root (two levels up from this script)
cd "$(dirname "$0")/../.."

if [ ! -f .env ]; then
  echo "ERROR: .env not found at repo root." >&2
  echo "       cp .env.prod.example .env  &&  edit it with real secrets." >&2
  exit 1
fi

echo ">> Building and starting containers..."
docker compose -f docker-compose.prod.yml up -d --build

echo ">> Pruning old build layers..."
docker image prune -f >/dev/null || true

echo ""
docker compose -f docker-compose.prod.yml ps
echo ""
echo ">> Up. The app is served on port 80 (http://<this-server>/)."
echo "   Logs:  docker compose -f docker-compose.prod.yml logs -f backend"
