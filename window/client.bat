@echo off
setlocal EnableExtensions EnableDelayedExpansion

for %%I in ("%~dp0..") do set "ROOT_DIR=%%~fI"
set "CLIENT_JAR=%ROOT_DIR%\client-module\target\client-1.0-SNAPSHOT.jar"

call :wait_for_port 8080 60 || exit /b 1

if not exist "%CLIENT_JAR%" (
    pushd "%ROOT_DIR%"
    echo Building client jar...
    mvn -q -pl client-module -am package -DskipTests
    if errorlevel 1 (
        popd
        exit /b 1
    )
    popd
)

if not exist "%CLIENT_JAR%" (
    echo Client jar not found: %CLIENT_JAR%
    exit /b 1
)

echo Starting client...
pushd "%ROOT_DIR%"
start "DauGia Client" java -jar "%CLIENT_JAR%"
popd
echo Launch client.bat again to open another client window.
exit /b 0

:wait_for_port
set "PORT=%~1"
set "MAX_RETRIES=%~2"
set /a COUNT=0

:wait_loop
netstat -ano | findstr /R /C:":%PORT% .*LISTENING" >nul 2>&1
if not errorlevel 1 exit /b 0

set /a COUNT+=1
if !COUNT! geq !MAX_RETRIES! exit /b 1

timeout /t 2 /nobreak >nul
goto wait_loop
