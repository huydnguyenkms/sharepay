#!/usr/bin/env bash
#
# Build and (re)start the production stack. Run from anywhere inside the repo on the server.
# Requires a .env file at the repo root (copy .env.prod.example -> .env first).
#
# Compose file selection:
#   - Override explicitly:  deploy.sh docker-compose.prod.yml   (or set COMPOSE_FILE=...)
#   - Otherwise auto-detect: if DOMAIN is set in .env, use the HTTPS stack (Caddy + TLS);
#     if not, fall back to the plain HTTP stack on port 80.
set -euo pipefail

# cd to repo root (two levels up from this script)
cd "$(dirname "$0")/../.."

if [ ! -f .env ]; then
  echo "ERROR: .env not found at repo root." >&2
  echo "       cp .env.prod.example .env  &&  edit it with real secrets." >&2
  exit 1
fi

# Pick the compose file: explicit arg/env wins, otherwise auto-detect from .env.
COMPOSE_FILE="${1:-${COMPOSE_FILE:-}}"
if [ -z "$COMPOSE_FILE" ]; then
  # DOMAIN present and non-empty in .env => HTTPS stack.
  if grep -qE '^[[:space:]]*DOMAIN[[:space:]]*=[[:space:]]*[^[:space:]]' .env; then
    COMPOSE_FILE="docker-compose.https.yml"
  else
    COMPOSE_FILE="docker-compose.prod.yml"
  fi
fi

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "ERROR: compose file '$COMPOSE_FILE' not found at repo root." >&2
  exit 1
fi

echo ">> Using $COMPOSE_FILE"
echo ">> Building and starting containers..."
docker compose -f "$COMPOSE_FILE" up -d --build

echo ">> Pruning old build layers..."
docker image prune -f >/dev/null || true

echo ""
docker compose -f "$COMPOSE_FILE" ps
echo ""
if [ "$COMPOSE_FILE" = "docker-compose.https.yml" ]; then
  echo ">> Up. The app is served over HTTPS on your DOMAIN (ports 80 + 443)."
else
  echo ">> Up. The app is served on port 80 (http://<this-server>/)."
fi
echo "   Logs:  docker compose -f $COMPOSE_FILE logs -f backend"
