#!/bin/bash
# ============================================================
#  Capella Beautify + ELK Full Installer (macOS)
#  Compatible with Capella 7.0.1
# ============================================================

set -e

CAPELLA_DIR="/Applications/Capella.app/Contents/Eclipse"
PLUGINS_DIR="$CAPELLA_DIR/plugins"
DROPINS_DIR="$CAPELLA_DIR/dropins"
CONFIG_DIR="$CAPELLA_DIR/configuration"
BUNDLES_INFO="$CONFIG_DIR/org.eclipse.equinox.simpleconfigurator/bundles.info"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ELK_JARS_DIR="${ELK_JARS_DIR:-$PROJECT_DIR/elk-jars}"

echo ""
echo "============================================"
echo " Capella Beautify + ELK Installer (macOS)"
echo "============================================"
echo ""

# Check Capella exists
if [ ! -d "$CAPELLA_DIR" ]; then
    echo "ERROR: Capella not found at $CAPELLA_DIR"
    exit 1
fi

# Check beautify JAR exists
if [ ! -f "$PROJECT_DIR/dist/capella-beautify-1.0.1.jar" ]; then
    echo "ERROR: Plugin JAR not found. Run scripts/build-macos.sh first."
    exit 1
fi

echo "[1/6] Backing up original files..."
mkdir -p "$CAPELLA_DIR/backup"
cp "$BUNDLES_INFO" "$CAPELLA_DIR/backup/bundles.info.bak" 2>/dev/null || true
echo "   Done."

echo "[2/6] Installing Beautify plugin to dropins/..."
cp "$PROJECT_DIR/dist/capella-beautify-1.0.1.jar" "$DROPINS_DIR/"
echo "   Done."

echo "[3/6] Copying ELK JARs..."
if [ -d "$ELK_JARS_DIR/jars/elk" ]; then
    cp "$ELK_JARS_DIR"/jars/elk/*.jar "$PLUGINS_DIR/"
    echo "   Copied ELK JARs."
else
    echo "   WARNING: ELK JARs not found at $ELK_JARS_DIR/jars/elk"
    echo "   Beautify will NOT work without ELK (the layout engine is missing)."
fi

echo "[4/6] Copying dependency JARs (patched Guice, xtext, aopalliance)..."
if [ -d "$ELK_JARS_DIR/jars/dependencies" ]; then
    cp "$ELK_JARS_DIR"/jars/dependencies/*.jar "$PLUGINS_DIR/"
    echo "   Copied dependency JARs."
else
    echo "   WARNING: Dependency JARs not found at $ELK_JARS_DIR/jars/dependencies"
fi

echo "[5/6] Registering bundles in bundles.info..."
if grep -q "sirius.diagram.elk" "$BUNDLES_INFO" 2>/dev/null; then
    echo "   Bundles already registered, skipping."
else
    cat >> "$BUNDLES_INFO" << 'EOF'
org.eclipse.sirius.diagram.elk,7.4.5.202411261603,plugins/org.eclipse.sirius.diagram.elk_7.4.5.202411261603.jar,4,false
org.eclipse.elk.core,0.9.0,plugins/org.eclipse.elk.core_0.9.0.jar,4,false
org.eclipse.elk.core.service,0.9.0,plugins/org.eclipse.elk.core.service_0.9.0.jar,4,false
org.eclipse.elk.graph,0.9.0,plugins/org.eclipse.elk.graph_0.9.0.jar,4,false
org.eclipse.elk.alg.layered,0.9.0,plugins/org.eclipse.elk.alg.layered_0.9.0.jar,4,false
org.eclipse.elk.alg.rectpacking,0.9.0,plugins/org.eclipse.elk.alg.rectpacking_0.9.0.jar,4,false
org.eclipse.elk.alg.common,0.9.0,plugins/org.eclipse.elk.alg.common_0.9.0.jar,4,false
com.google.inject,5.0.1.v20221112-0806,plugins/com.google.inject_5.0.1.v20221112-0806.jar,4,false
org.eclipse.xtext.xbase.lib,2.30.0.v20230227-1111,plugins/org.eclipse.xtext.xbase.lib_2.30.0.v20230227-1111.jar,4,false
org.aopalliance,1.0.0.v20220404-1927,plugins/org.aopalliance_1.0.0.v20220404-1927.jar,4,false
EOF
    echo "   Done."
fi

echo "[6/6] Cleaning OSGi cache..."
rm -rf "$CONFIG_DIR/org.eclipse.osgi"

INI_FILE="$CAPELLA_DIR/capella.ini"
if ! grep -q "^-clean$" "$INI_FILE" 2>/dev/null; then
    echo "-clean" >> "$INI_FILE"
    echo "   Added -clean flag to capella.ini."
fi
echo "   Done."

echo ""
echo "============================================"
echo " Installation complete!"
echo "============================================"
echo ""
echo " Next steps:"
echo " 1. Remove quarantine:  sudo xattr -cr /Applications/Capella.app"
echo " 2. Open Capella:       open /Applications/Capella.app"
echo " 3. Preferences > Sirius > Sirius Diagram > Enable ELK"
echo " 4. Open a diagram > click Beautify dropdown > Horizontal or Vertical"
echo ""
