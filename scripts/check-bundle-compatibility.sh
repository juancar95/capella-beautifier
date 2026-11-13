#!/bin/bash
# Resolve the complete package and exercise configuration/handler linkage.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
exec python3 "$SCRIPT_DIR/compatibility/runtime-check.py" bundles "$@"
