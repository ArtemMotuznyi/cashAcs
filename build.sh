#!/bin/bash
set -euo pipefail

# ==== Settings ====
COMPOSE_PROJECT_NAME="cashacs"
COMPOSE_CMD="${COMPOSE_CMD:-docker-compose}"   # або підстав 'docker compose' якщо ти на v2
DETACH="no"
DEBUG="no"

usage() {
  echo "Usage: $0 [--dev] [--detach]"
  echo "  --dev   run with JDWP (port 5005) using docker-compose.dev.yml"
  echo "  --detach  run docker-compose up -d"
  exit 1
}

# ==== Parse args ====
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dev)  DEBUG="yes"; shift ;;
    --detach|-d) DETACH="yes"; shift ;;
    -h|--help) usage ;;
    *) echo "Unknown arg: $1"; usage ;;
  esac
done

# ==== Build ====
./gradlew clean build
echo "Build completed successfully. Artifacts are in the build/libs directory."

# ==== Compose down ====
export COMPOSE_PROJECT_NAME
$COMPOSE_CMD down --remove-orphans
echo "Old containers stopped and removed."

# ==== Compose up ====
echo "Building and starting new containers..."

COMPOSE_FILES="-f docker-compose.yml"
if [[ "$DEBUG" == "yes" ]]; then
  COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.dev.yml"
  echo "Debug mode ON (JDWP on :5005)."
fi

if [[ "$DETACH" == "yes" ]]; then
  # build + up -d
  # shellcheck disable=SC2086
  $COMPOSE_CMD $COMPOSE_FILES up --build -d
  echo "Docker Compose started (detached)."
else
  # build + up (foreground)
  # shellcheck disable=SC2086
  $COMPOSE_CMD $COMPOSE_FILES up --build
  echo "Docker Compose started with the new build."
fi
