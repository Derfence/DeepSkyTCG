@echo off
setlocal EnableExtensions

set SCRIPT_DIR=%~dp0
set PROPS_FILE=%SCRIPT_DIR%gradle\wrapper\gradle-wrapper.properties

if not exist "%PROPS_FILE%" (
  echo Missing %PROPS_FILE%
  exit /b 1
)

for /f "tokens=1,* delims==" %%A in (%PROPS_FILE%) do (
  if "%%A"=="distributionUrl" set DIST_URL=%%B
)
set DIST_URL=%DIST_URL:\:=:%

if "%DIST_URL%"=="" (
  echo distributionUrl not found in gradle-wrapper.properties
  exit /b 1
)

for %%I in ("%DIST_URL%") do set DIST_FILE=%%~nxI
set DIST_NAME=%DIST_FILE:-bin.zip=%
set DIST_NAME=%DIST_NAME:-all.zip=%
set WRAPPER_DIR=%SCRIPT_DIR%.gradle-wrapper
set ZIP_PATH=%WRAPPER_DIR%\%DIST_FILE%
set INSTALL_DIR=%WRAPPER_DIR%\dist
set GRADLE_BIN=%INSTALL_DIR%\%DIST_NAME%\bin\gradle.bat

if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
if not exist "%INSTALL_DIR%" mkdir "%INSTALL_DIR%"

if not exist "%GRADLE_BIN%" if not exist "%ZIP_PATH%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%'"
)

if not exist "%GRADLE_BIN%" powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%ZIP_PATH%' -DestinationPath '%INSTALL_DIR%' -Force"

if not exist "%GRADLE_BIN%" (
  echo Unable to locate gradle.bat after extraction.
  exit /b 1
)

pushd "%SCRIPT_DIR%" >nul
call "%GRADLE_BIN%" %*
set EXIT_CODE=%ERRORLEVEL%
popd >nul
exit /b %EXIT_CODE%
