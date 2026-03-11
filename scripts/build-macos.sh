#!/bin/bash
# Build script for Capella Beautify Plugin
# Compiles against Capella 7.0.1 plugin JARs and packages as an OSGi bundle JAR

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_DIR"

CAPELLA_PLUGINS="${CAPELLA_HOME:-/Applications/Capella.app/Contents/Eclipse}/plugins"

if [ ! -d "$CAPELLA_PLUGINS" ]; then
    echo "ERROR: Capella plugins not found at $CAPELLA_PLUGINS"
    exit 1
fi

# Build classpath from Capella plugins (wildcard globs for version independence)
CP=""
for pattern in \
    "org.polarsys.capella.core.sirius.analysis_*.jar" \
    "org.eclipse.core.commands_*.jar" \
    "org.eclipse.core.runtime_*.jar" \
    "org.eclipse.equinox.registry_*.jar" \
    "org.eclipse.sirius.diagram.model_*.jar" \
    "org.eclipse.sirius.diagram.ui_*.jar" \
    "org.eclipse.sirius.diagram_7*.jar" \
    "org.eclipse.sirius.model_*.jar" \
    "org.eclipse.gmf.runtime.diagram.ui_*.jar" \
    "org.eclipse.gmf.runtime.diagram.core_*.jar" \
    "org.eclipse.gmf.runtime.notation_*.jar" \
    "org.eclipse.gmf.runtime.common.core_*.jar" \
    "org.eclipse.gef_*.jar" \
    "org.eclipse.swt_*.jar" \
    "org.eclipse.swt.cocoa.macosx.aarch64_*.jar" \
    "org.eclipse.ui.workbench_*.jar" \
    "org.eclipse.ui.views.properties.tabbed_*.jar" \
    "org.eclipse.jface_*.jar" \
    "org.eclipse.equinox.common_*.jar" \
    "org.eclipse.emf.ecore_*.jar" \
    "org.eclipse.emf.common_*.jar" \
    "org.apache.log4j_*.jar" \
    "org.eclipse.gmf.runtime.diagram.ui.actions_*.jar" \
    "org.eclipse.gmf.runtime.common.ui.action_*.jar" \
    "org.eclipse.gmf.runtime.common.ui_*.jar" \
    "org.eclipse.draw2d_*.jar" \
    "org.eclipse.osgi_*.jar" \
    "org.eclipse.emf.transaction_*.jar" \
    "org.eclipse.sirius_7*.jar" \
    "org.eclipse.sirius.common_*.jar" \
    "org.eclipse.emf.edit_*.jar"
do
    found=$(ls $CAPELLA_PLUGINS/$pattern 2>/dev/null | head -1)
    if [ -n "$found" ]; then
        CP="$CP:$found"
    else
        echo "WARNING: No match for $pattern"
    fi
done

# Remove leading colon
CP="${CP:1}"

echo "Compiling..."
rm -rf build/
mkdir -p build/ dist/

javac -source 17 -target 17 \
    -cp "$CP" \
    -d build/ \
    src/org/polarsys/capella/beautify/ElkConfigInjector.java \
    src/org/polarsys/capella/beautify/handlers/BeautifyDiagramHandler.java

echo "Packaging JAR..."
jar cfm dist/capella-beautify-1.0.1.jar META-INF/MANIFEST.MF \
    -C build/ org/ \
    -C . plugin.xml \
    -C . icons/

echo ""
echo "Built: dist/capella-beautify-1.0.1.jar"
echo ""
echo "Install: copy to <Capella>/dropins/"
