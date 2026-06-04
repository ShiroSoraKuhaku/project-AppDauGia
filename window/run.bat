@echo off
setlocal EnableExtensions EnableDelayedExpansion

for %%I in ("%~dp0..") do set "ROOT_DIR=%%~fI"

call :resolve_compose || exit /b 1

pushd "%ROOT_DIR%"
echo Starting MySQL and server containers...
call %COMPOSE_CMD% up -d --build db server
if errorlevel 1 (
    popd
    exit /b 1
)
popd

echo Waiting for server on localhost:8080...
call :wait_for_port 8080 60 || exit /b 1

echo System started. Run client.bat to open one or more client windows.
exit /b 0

:resolve_compose
docker compose version >nul 2>&1
if not errorlevel 1 (
    set "COMPOSE_CMD=docker compose"
    exit /b 0
)

docker-compose version >nul 2>&1
if not errorlevel 1 (
    set "COMPOSE_CMD=docker-compose"
    exit /b 0
)

echo Khong tim thay Docker Compose. Hay mo Docker Desktop truoc khi chay.
exit /b 1

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
