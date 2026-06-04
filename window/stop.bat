@echo off
setlocal EnableExtensions EnableDelayedExpansion

for %%I in ("%~dp0..") do set "ROOT_DIR=%%~fI"

taskkill /f /fi "WINDOWTITLE eq DauGia Client" >nul 2>&1

call :resolve_compose || exit /b 1

pushd "%ROOT_DIR%"
echo Stopping Docker containers...
call %COMPOSE_CMD% down --remove-orphans
if errorlevel 1 (
    popd
    exit /b 1
)
popd

echo System stopped.
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
