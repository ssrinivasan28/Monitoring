@echo off
setlocal enabledelayedexpansion

set PROJECT_ROOT=%~dp0
set MONITORS_DIR=%PROJECT_ROOT%target\monitors
set RESOURCES_DIR=%PROJECT_ROOT%installer\resources\monitoring-services\ShareFileMonitor
set JAR_NAME=ShareFileMonitor.jar
set ISS_FILE=%PROJECT_ROOT%ShareFileMonitor.iss
set ISCC=

echo.
echo === Build ShareFile Monitor Installer ===
echo.

:: Find Inno Setup
for %%P in (
    "C:\Program Files (x86)\Inno Setup 6\ISCC.exe"
    "C:\Program Files\Inno Setup 6\ISCC.exe"
    "C:\Program Files (x86)\Inno Setup 5\ISCC.exe"
) do (
    if exist %%P (
        set ISCC=%%P
        goto :found_inno
    )
)
echo [X] Inno Setup not found. Install from https://jrsoftware.org/isdl.php
exit /b 1
:found_inno
echo [OK] Inno Setup: %ISCC%

:: Maven build (single JAR via -pl not available in single-module; build all, we only need ShareFileMonitor)
echo.
echo =^> Running Maven build...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo [X] Maven build failed.
    exit /b 1
)

:: Verify JAR
if not exist "%MONITORS_DIR%\%JAR_NAME%" (
    echo [X] JAR not found: %MONITORS_DIR%\%JAR_NAME%
    exit /b 1
)
echo [OK] JAR built: %JAR_NAME%

:: Copy JAR to installer resources
if not exist "%RESOURCES_DIR%" mkdir "%RESOURCES_DIR%"
copy /Y "%MONITORS_DIR%\%JAR_NAME%" "%RESOURCES_DIR%\%JAR_NAME%" >nul
echo [OK] Copied JAR to installer\resources\monitoring-services\ShareFileMonitor\

:: Copy default properties files if present
for %%F in (sharefilemonitor.properties email.properties) do (
    if exist "%PROJECT_ROOT%%%F" (
        copy /Y "%PROJECT_ROOT%%%F" "%RESOURCES_DIR%\%%F" >nul
        echo [OK] Copied %%F
    )
)

:: Compile Inno Setup installer
echo.
echo =^> Compiling installer...
%ISCC% "%ISS_FILE%"
if errorlevel 1 (
    echo [X] Inno Setup compilation failed.
    exit /b 1
)

echo.
echo [OK] Done. Installer: installer\output\ShareFileMonitorSetup.exe
echo.
endlocal
