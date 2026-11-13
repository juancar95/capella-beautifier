@echo off
REM Install the matching full package using Windows PowerShell.
powershell.exe -NoProfile -File "%~dp0install-windows.ps1"
exit /b %errorlevel%
