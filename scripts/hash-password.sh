#!/bin/bash
set -euo pipefail

if [ $# -ne 2 ]; then
  echo "Usage: $0 <username> <password>"
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Using script directory: $SCRIPT_DIR"
MAIN_KTS="$SCRIPT_DIR/hash-password.main.kts"

if ! command -v kotlin >/dev/null 2>&1; then
  echo "Error: kotlin CLI not found in PATH"
  exit 1
fi

chmod +x "$MAIN_KTS"
"$MAIN_KTS" "$1" "$2"