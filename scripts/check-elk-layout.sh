#!/bin/bash
# Test actual layout with the dependency set intended for a target Capella.
set -euo pipefail
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <Capella Eclipse directory> <extra plugins directory>" >&2
    exit 1
fi
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CHECK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/capella-elk-check.XXXXXX")"
trap 'rm -rf "$CHECK_DIR"' EXIT
CP="$2/*:$1/plugins/*"
javac --release 17 -sourcepath '' -cp "$CP" -d "$CHECK_DIR" "$SCRIPT_DIR/compatibility/ElkLayoutCheck.java"
java -cp "$CHECK_DIR:$CP" ElkLayoutCheck
