#!/bin/bash
# Check bundle resolution and Java linkage without changing the Capella installation.
set -euo pipefail

if [ "$#" -ne 4 ]; then
    echo "Usage: $0 <Capella Eclipse directory> <extra plugins directory> <beautifier.jar> <resolved|unresolved>" >&2
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OSGI_JARS=("$1"/plugins/org.eclipse.osgi_*.jar)
if [ "${#OSGI_JARS[@]}" -ne 1 ] || [ ! -f "${OSGI_JARS[0]}" ]; then
    echo "ERROR: Expected one Equinox framework JAR in $1/plugins" >&2
    exit 1
fi
CHECK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/capella-beautify-check.XXXXXX")"
trap 'rm -rf "$CHECK_DIR"' EXIT

javac --release 17 -cp "${OSGI_JARS[0]}" -d "$CHECK_DIR/classes" \
    "$SCRIPT_DIR/compatibility/BundleCompatibilityCheck.java"
java -cp "$CHECK_DIR/classes:${OSGI_JARS[0]}" BundleCompatibilityCheck \
    "$1/plugins" "$2" "$3" "$CHECK_DIR/framework" "$4"
