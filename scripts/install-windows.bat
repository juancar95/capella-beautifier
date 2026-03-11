@echo off
REM ============================================================
REM  Capella Beautify + ELK Full Installer (Windows)
REM  Compatible with Capella 7.0.1
REM ============================================================

setlocal enabledelayedexpansion

set "SCRIPT_DIR=%~dp0"
set "PROJECT_DIR=%SCRIPT_DIR%.."

if defined CAPELLA_HOME (
    set "CAPELLA_DIR=%CAPELLA_HOME%"
) else (
    set "CAPELLA_DIR=C:\Program Files\Capella\eclipse"
)

set "PLUGINS_DIR=%CAPELLA_DIR%\plugins"
set "DROPINS_DIR=%CAPELLA_DIR%\dropins"
set "CONFIG_DIR=%CAPELLA_DIR%\configuration"
set "BUNDLES_INFO=%CONFIG_DIR%\org.eclipse.equinox.simpleconfigurator\bundles.info"

if defined ELK_JARS_DIR (
    set "ELK_DIR=%ELK_JARS_DIR%"
) else (
    set "ELK_DIR=%PROJECT_DIR%\elk-jars"
)

echo.
echo ============================================
echo  Capella Beautify + ELK Installer (Windows)
echo ============================================
echo.

REM Check Capella exists
if not exist "%CAPELLA_DIR%" (
    echo ERROR: Capella not found at %CAPELLA_DIR%
    echo Set CAPELLA_HOME to your Capella eclipse directory.
    exit /b 1
)

REM Check beautify JAR exists
if not exist "%PROJECT_DIR%\dist\capella-beautify-1.0.1.jar" (
    echo ERROR: Plugin JAR not found. Run scripts\build-windows.bat first.
    exit /b 1
)

echo [1/6] Backing up original files...
if not exist "%CAPELLA_DIR%\backup" mkdir "%CAPELLA_DIR%\backup"
copy "%BUNDLES_INFO%" "%CAPELLA_DIR%\backup\bundles.info.bak" >nul 2>&1
echo    Done.

echo [2/6] Installing Beautify plugin to dropins\...
if not exist "%DROPINS_DIR%" mkdir "%DROPINS_DIR%"
copy "%PROJECT_DIR%\dist\capella-beautify-1.0.1.jar" "%DROPINS_DIR%\" >nul
echo    Done.

echo [3/6] Copying ELK JARs...
if exist "%ELK_DIR%\elk" (
    copy "%ELK_DIR%\elk\*.jar" "%PLUGINS_DIR%\" >nul
    echo    Copied ELK JARs.
) else (
    echo    WARNING: ELK JARs not found at %ELK_DIR%\elk
    echo    Beautify will NOT work without ELK (the layout engine is missing).
)

echo [4/6] Copying dependency JARs...
if exist "%ELK_DIR%\dependencies" (
    copy "%ELK_DIR%\dependencies\*.jar" "%PLUGINS_DIR%\" >nul
    echo    Copied dependency JARs.
) else (
    echo    WARNING: Dependency JARs not found at %ELK_DIR%\dependencies
)

echo [5/6] Registering bundles in bundles.info...
findstr /c:"sirius.diagram.elk" "%BUNDLES_INFO%" >nul 2>&1
if %errorlevel%==0 (
    echo    Bundles already registered, skipping.
) else (
    echo org.eclipse.sirius.diagram.elk,7.4.5.202411261603,plugins/org.eclipse.sirius.diagram.elk_7.4.5.202411261603.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.elk.core,0.9.0,plugins/org.eclipse.elk.core_0.9.0.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.elk.core.service,0.9.0,plugins/org.eclipse.elk.core.service_0.9.0.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.elk.graph,0.9.0,plugins/org.eclipse.elk.graph_0.9.0.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.elk.alg.layered,0.9.0,plugins/org.eclipse.elk.alg.layered_0.9.0.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.elk.alg.rectpacking,0.9.0,plugins/org.eclipse.elk.alg.rectpacking_0.9.0.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.elk.alg.common,0.9.0,plugins/org.eclipse.elk.alg.common_0.9.0.jar,4,false>> "%BUNDLES_INFO%"
    echo com.google.inject,5.0.1.v20221112-0806,plugins/com.google.inject_5.0.1.v20221112-0806.jar,4,false>> "%BUNDLES_INFO%"
    echo org.eclipse.xtext.xbase.lib,2.30.0.v20230227-1111,plugins/org.eclipse.xtext.xbase.lib_2.30.0.v20230227-1111.jar,4,false>> "%BUNDLES_INFO%"
    echo org.aopalliance,1.0.0.v20220404-1927,plugins/org.aopalliance_1.0.0.v20220404-1927.jar,4,false>> "%BUNDLES_INFO%"
    echo    Done.
)

echo [6/6] Cleaning OSGi cache...
if exist "%CONFIG_DIR%\org.eclipse.osgi" rmdir /s /q "%CONFIG_DIR%\org.eclipse.osgi"

set "INI_FILE=%CAPELLA_DIR%\capella.ini"
findstr /c:"-clean" "%INI_FILE%" >nul 2>&1
if not %errorlevel%==0 (
    echo -clean>> "%INI_FILE%"
    echo    Added -clean flag to capella.ini.
)
echo    Done.

echo.
echo ============================================
echo  Installation complete!
echo ============================================
echo.
echo  Next steps:
echo  1. Open Capella
echo  2. Open a diagram ^> click Beautify H or Beautify V
echo.

endlocal
