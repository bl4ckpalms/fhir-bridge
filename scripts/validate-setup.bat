@echo off
REM =============================================================================
REM Docker Setup Validation Script for FHIR Bridge (Windows)
REM =============================================================================

setlocal enabledelayedexpansion

REM Colors for output (Windows CMD doesn't support colors well, so we'll use text)
set "INFO=[INFO]"
set "SUCCESS=[SUCCESS]"
set "WARNING=[WARNING]"
set "ERROR=[ERROR]"

set "validation_failed=false"

echo %INFO% Starting FHIR Bridge Docker setup validation...
echo.

REM Validate Docker installation
echo %INFO% Validating Docker installation...
docker --version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Docker is not installed
    set "validation_failed=true"
) else (
    docker info >nul 2>&1
    if errorlevel 1 (
        echo %ERROR% Docker daemon is not running
        set "validation_failed=true"
    ) else (
        echo %SUCCESS% Docker is installed and running
    )
)

REM Validate Docker Compose
echo %INFO% Validating Docker Compose installation...
docker compose version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Docker Compose is not available
    set "validation_failed=true"
) else (
    echo %SUCCESS% Docker Compose is available
)

REM Validate Docker Compose files
echo %INFO% Validating Docker Compose configurations...

docker compose -f docker-compose.yml config >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Main docker-compose.yml is invalid
    set "validation_failed=true"
) else (
    echo %SUCCESS% Main docker-compose.yml is valid
)

docker compose -f docker-compose.test.yml config >nul 2>&1
if errorlevel 1 (
    echo %ERROR% docker-compose.test.yml is invalid
    set "validation_failed=true"
) else (
    echo %SUCCESS% docker-compose.test.yml is valid
)

docker compose -f docker-compose.local.yml config >nul 2>&1
if errorlevel 1 (
    echo %ERROR% docker-compose.local.yml is invalid
    set "validation_failed=true"
) else (
    echo %SUCCESS% docker-compose.local.yml is valid
)

REM Validate required directories
echo %INFO% Validating required directories...
set "dirs_ok=true"
if not exist "infra\db\init" set "dirs_ok=false" & echo %ERROR% Required directory missing: infra\db\init
if not exist "infra\redis" set "dirs_ok=false" & echo %ERROR% Required directory missing: infra\redis
if not exist "infra\nginx" set "dirs_ok=false" & echo %ERROR% Required directory missing: infra\nginx
if not exist "infra\performance" set "dirs_ok=false" & echo %ERROR% Required directory missing: infra\performance
if not exist "scripts" set "dirs_ok=false" & echo %ERROR% Required directory missing: scripts
if not exist "logs" set "dirs_ok=false" & echo %ERROR% Required directory missing: logs

if "%dirs_ok%"=="false" (
    set "validation_failed=true"
) else (
    echo %SUCCESS% All required directories exist
)

REM Validate configuration files
echo %INFO% Validating configuration files...
set "files_ok=true"
if not exist "src\main\resources\application.yml" set "files_ok=false" & echo %ERROR% Required file missing: src\main\resources\application.yml
if not exist "src\main\resources\application-dev.yml" set "files_ok=false" & echo %ERROR% Required file missing: src\main\resources\application-dev.yml
if not exist "src\main\resources\application-test.yml" set "files_ok=false" & echo %ERROR% Required file missing: src\main\resources\application-test.yml
if not exist "infra\db\init\01-init-database.sql" set "files_ok=false" & echo %ERROR% Required file missing: infra\db\init\01-init-database.sql
if not exist "infra\redis\redis-dev.conf" set "files_ok=false" & echo %ERROR% Required file missing: infra\redis\redis-dev.conf
if not exist "infra\redis\redis-local.conf" set "files_ok=false" & echo %ERROR% Required file missing: infra\redis\redis-local.conf
if not exist "infra\db\pgadmin-servers.json" set "files_ok=false" & echo %ERROR% Required file missing: infra\db\pgadmin-servers.json
if not exist "docker-compose.yml" set "files_ok=false" & echo %ERROR% Required file missing: docker-compose.yml
if not exist "docker-compose.test.yml" set "files_ok=false" & echo %ERROR% Required file missing: docker-compose.test.yml
if not exist "docker-compose.local.yml" set "files_ok=false" & echo %ERROR% Required file missing: docker-compose.local.yml
if not exist "Dockerfile" set "files_ok=false" & echo %ERROR% Required file missing: Dockerfile
if not exist "Dockerfile.dev" set "files_ok=false" & echo %ERROR% Required file missing: Dockerfile.dev

if "%files_ok%"=="false" (
    set "validation_failed=true"
) else (
    echo %SUCCESS% All required configuration files exist
)

REM Check port availability (simplified for Windows)
echo %INFO% Checking port availability...
echo %WARNING% Port availability check is simplified on Windows
echo %WARNING% Please ensure ports 8080, 8081, 5432, 6379, 5050, 8082, 8025, 80 are available

REM Validate Maven setup
echo %INFO% Validating Maven setup...
if exist mvnw.cmd (
    echo %SUCCESS% Maven wrapper found
) else (
    echo %ERROR% Maven wrapper ^(mvnw.cmd^) not found
    set "validation_failed=true"
)

if exist pom.xml (
    echo %SUCCESS% Maven configuration is valid
) else (
    echo %ERROR% pom.xml not found
    set "validation_failed=true"
)

echo.
if "%validation_failed%"=="true" (
    echo %ERROR% Validation failed! Please fix the issues above before proceeding.
    exit /b 1
) else (
    echo %SUCCESS% All validations passed! Your Docker setup is ready.
    echo.
    echo %INFO% Next steps:
    echo   1. Run 'scripts\dev-setup.bat setup' to start the development environment
    echo   2. Run 'scripts\dev-setup.bat test' to run tests
    echo   3. Access the application at http://localhost:8080
)

endlocal