#!/bin/bash
# Execute actual ELK graph layouts with the target Capella dependency set.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec python3 "$SCRIPT_DIR/compatibility/runtime-check.py" elk "$@"
