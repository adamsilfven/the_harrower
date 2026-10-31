@echo off
cd /d "%~dp0\.."
call gradlew.bat clean build
if errorlevel 1 exit /b %errorlevel%
python3 scripts\package_ritual.py
if errorlevel 1 exit /b %errorlevel%
python3 scripts\verify_baseline.py build\libs\harrower-neoforge-1.21.1-1.0.0.jar
exit /b %errorlevel%
