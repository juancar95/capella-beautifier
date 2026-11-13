#!/bin/bash
# Install the version-matched release drop-in without editing native bundles or caches.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
CAPELLA_DIR="${CAPELLA_HOME:-/Applications/Capella.app/Contents/Eclipse}"
VERSION="$(awk '/^Bundle-Version:/ {print $2}' "$PROJECT_DIR/META-INF/MANIFEST.MF")"
ANALYSIS=("$CAPELLA_DIR"/plugins/org.polarsys.capella.core.sirius.analysis_*.jar)
if [ "${#ANALYSIS[@]}" -ne 1 ] || [ ! -f "${ANALYSIS[0]}" ]; then
    echo "ERROR: Expected one Capella analysis bundle in $CAPELLA_DIR/plugins" >&2
    exit 1
fi
CAPELLA_VERSION="$(basename "${ANALYSIS[0]}" | sed -E 's/.*_([0-9]+\.[0-9]+\.[0-9]+).*/\1/')"
PACKAGE="$PROJECT_DIR/dist/capella-beautifier-v$VERSION-capella-$CAPELLA_VERSION-full.zip"
if [ ! -f "$PACKAGE" ]; then
    echo "ERROR: Matching release package not found: $PACKAGE" >&2
    exit 1
fi
STAGING="$(mktemp -d "${TMPDIR:-/tmp}/capella-beautify-install.XXXXXX")"
trap 'rm -rf "$STAGING"' EXIT
unzip -q "$PACKAGE" -d "$STAGING"
PAYLOAD="$STAGING/capella-beautifier-v$VERSION-capella-$CAPELLA_VERSION-full/dropins/capella-beautifier"
if [ ! -f "$PAYLOAD/eclipse/plugins/capella-beautify-$VERSION.jar" ]; then
    echo "ERROR: Incomplete release package" >&2
    exit 1
fi
BACKUP="$CAPELLA_DIR/backup/beautifier-$(date +%Y%m%d-%H%M%S)-$$"
mkdir -p "$BACKUP" "$CAPELLA_DIR/dropins"
if [ -d "$CAPELLA_DIR/dropins/capella-beautifier" ]; then
    mv "$CAPELLA_DIR/dropins/capella-beautifier" "$BACKUP/capella-beautifier"
fi
for folder in dropins plugins; do
    while IFS= read -r -d '' old; do
        relative="${old#"$CAPELLA_DIR/"}"
        mkdir -p "$BACKUP/$(dirname "$relative")"
        mv "$old" "$BACKUP/$relative"
    done < <(find "$CAPELLA_DIR/$folder" -type f -name 'capella-beautify-*.jar' -print0)
done
cp -R "$PAYLOAD" "$CAPELLA_DIR/dropins/"
echo "Installed Beautifier $VERSION for Capella $CAPELLA_VERSION. Backup: $BACKUP"
echo "Restart Capella. Use -clean before -vmargs on the first launch if necessary."
