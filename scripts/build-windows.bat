@echo off
REM Requires JDK 17+ and Python 3 on PATH.
python "%~dp0build.py"
exit /b %errorlevel%
