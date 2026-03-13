@echo off
REM Build script for Capella Beautify Plugin (Windows)
REM Compiles against Capella 7.0.1 plugin JARs and packages as a JAR

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."
cd /d "%PROJECT_DIR%"

if defined CAPELLA_HOME (
    set "CAPELLA_PLUGINS=%CAPELLA_HOME%\plugins"
) else (
    set "CAPELLA_PLUGINS=C:\Program Files\Capella\eclipse\plugins"
)

if not exist "%CAPELLA_PLUGINS%" (
    echo ERROR: Capella plugins not found at %CAPELLA_PLUGINS%
    echo Set CAPELLA_HOME to your Capella eclipse directory.
    exit /b 1
)

REM Build classpath from Capella plugins
set "CP="
for %%p in (
    "org.polarsys.capella.core.sirius.analysis_*.jar"
    "org.eclipse.core.commands_*.jar"
    "org.eclipse.core.runtime_*.jar"
    "org.eclipse.equinox.registry_*.jar"
    "org.eclipse.sirius.diagram.model_*.jar"
    "org.eclipse.sirius.diagram.ui_*.jar"
    "org.eclipse.sirius.diagram_7*.jar"
    "org.eclipse.sirius.model_*.jar"
    "org.eclipse.gmf.runtime.diagram.ui_*.jar"
    "org.eclipse.gmf.runtime.diagram.core_*.jar"
    "org.eclipse.gmf.runtime.notation_*.jar"
    "org.eclipse.gmf.runtime.common.core_*.jar"
    "org.eclipse.gef_*.jar"
    "org.eclipse.swt_*.jar"
    "org.eclipse.swt.win32.win32.x86_64_*.jar"
    "org.eclipse.ui.workbench_*.jar"
    "org.eclipse.ui.views.properties.tabbed_*.jar"
    "org.eclipse.jface_*.jar"
    "org.eclipse.equinox.common_*.jar"
    "org.eclipse.emf.ecore_*.jar"
    "org.eclipse.emf.common_*.jar"
    "org.apache.log4j_*.jar"
    "org.eclipse.gmf.runtime.diagram.ui.actions_*.jar"
    "org.eclipse.gmf.runtime.common.ui.action_*.jar"
    "org.eclipse.gmf.runtime.common.ui_*.jar"
    "org.eclipse.draw2d_*.jar"
    "org.eclipse.osgi_*.jar"
    "org.eclipse.emf.transaction_*.jar"
    "org.eclipse.sirius_7*.jar"
    "org.eclipse.sirius.common_*.jar"
    "org.eclipse.emf.edit_*.jar"
) do (
    for %%f in ("%CAPELLA_PLUGINS%\%%~p") do (
        if "!CP!"=="" (
            set "CP=%%f"
        ) else (
            set "CP=!CP!;%%f"
        )
    )
)

echo Compiling...
if exist build rmdir /s /q build
mkdir build
if not exist dist mkdir dist

javac -source 17 -target 17 ^
    -cp "%CP%" ^
    -d build ^
    src\org\polarsys\capella\beautify\ElkConfigInjector.java ^
    src\org\polarsys\capella\beautify\handlers\BeautifyDiagramHandler.java

if errorlevel 1 (
    echo ERROR: Compilation failed.
    exit /b 1
)

echo Packaging JAR...
jar cfm dist\capella-beautify-1.0.2.jar META-INF\MANIFEST.MF ^
    -C build org ^
    -C . plugin.xml ^
    -C . icons

echo.
echo Built: dist\capella-beautify-1.0.2.jar
echo.
echo Install: copy to ^<Capella^>\dropins\

endlocal
