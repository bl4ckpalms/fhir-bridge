@echo off
REM =============================================================================
REM Docker Compose Setup Validation Script
REM Validates that the Docker Compose testing environment is properly configured
REM =============================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================
echo FHIR Bridge Docker Setup Validation
echo ========================================
echo.

REM Check Docker installation
echo 🔍 Checking Docker installation...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed or not in PATH
    echo Please install Docker Desktop and try again
    exit /b 1
) else (
    echo ✅ Docker is installed
    docker --version
)

echo.

REM Check Docker Compose
echo 🔍 Checking Docker Compose...
docker compose version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose is not available
    echo Please ensure Docker Desktop is running
    exit /b 1
) else (
    echo ✅ Docker Compose is available
    docker compose version
)

echo.

REM Check if Docker daemon is running
echo 🔍 Checking Docker daemon...
docker info >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker daemon is not running
    echo Please start Docker Desktop
    exit /b 1
) else (
    echo ✅ Docker daemon is running
)

echo.

REM Validate Docker Compose files
echo 🔍 Validating Docker Compose configurations...

if not exist "docker-compose.test.yml" (
    echo ❌ docker-compose.test.yml not found
    exit /b 1
) else (
    echo ✅ docker-compose.test.yml found
)

if not exist "docker-compose.local.yml" (
    echo ❌ docker-compose.local.yml not found
    exit /b 1
) else (
    echo ✅ docker-compose.local.yml found
)

if not exist "Dockerfile.dev" (
    echo ❌ Dockerfile.dev not found
    exit /b 1
) else (
    echo ✅ Dockerfile.dev found
)

echo.

REM Validate Docker Compose syntax
echo 🔍 Validating Docker Compose syntax...
docker compose -f docker-compose.test.yml config >nul 2>&1
if errorlevel 1 (
    echo ❌ docker-compose.test.yml has syntax errors
    docker compose -f docker-compose.test.yml config
    exit /b 1
) else (
    echo ✅ docker-compose.test.yml syntax is valid
)

docker compose -f docker-compose.local.yml config >nul 2>&1
if errorlevel 1 (
    echo ❌ docker-compose.local.yml has syntax errors
    docker compose -f docker-compose.local.yml config
    exit /b 1
) else (
    echo ✅ docker-compose.local.yml syntax is valid
)

echo.

REM Check required directories
echo 🔍 Checking required directories...

if not exist "infra\db" (
    echo ❌ infra\db directory not found
    exit /b 1
) else (
    echo ✅ infra\db directory exists
)

if not exist "infra\redis" (
    echo ❌ infra\redis directory not found
    exit /b 1
) else (
    echo ✅ infra\redis directory exists
)

if not exist "scripts" (
    echo ❌ scripts directory not found
    exit /b 1
) else (
    echo ✅ scripts directory exists
)

if not exist "logs" (
    echo 📁 Creating logs directory...
    mkdir logs
    echo ✅ logs directory created
) else (
    echo ✅ logs directory exists
)

echo.

REM Check required configuration files
echo 🔍 Checking configuration files...

if not exist "infra\redis\redis-local.conf" (
    echo ❌ Redis local configuration not found
    exit /b 1
) else (
    echo ✅ Redis local configuration exists
)

if not exist "infra\db\pgadmin-servers.json" (
    echo ❌ PgAdmin configuration not found
    exit /b 1
) else (
    echo ✅ PgAdmin configuration exists
)

echo.

REM Check port availability
echo 🔍 Checking port availability...

netstat -an | findstr ":5433" >nul 2>&1
if not errorlevel 1 (
    echo ⚠️  Port 5433 (PostgreSQL test) is already in use
) else (
    echo ✅ Port 5433 (PostgreSQL test) is available
)

netstat -an | findstr ":6380" >nul 2>&1
if not errorlevel 1 (
    echo ⚠️  Port 6380 (Redis test) is already in use
) else (
    echo ✅ Port 6380 (Redis test) is available
)

netstat -an | findstr ":8083" >nul 2>&1
if not errorlevel 1 (
    echo ⚠️  Port 8083 (FHIR Bridge test) is already in use
) else (
    echo ✅ Port 8083 (FHIR Bridge test) is available
)

echo.

REM Test Docker Compose dry run
echo 🔍 Testing Docker Compose dry run...
docker compose -f docker-compose.test.yml up --dry-run >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose dry run failed
    echo Check the configuration and try again
    exit /b 1
) else (
    echo ✅ Docker Compose dry run successful
)

echo.
echo ========================================
echo ✅ Docker setup validation completed!
echo ========================================
echo.
echo 🚀 You can now start the test environment:
echo    scripts\test-environment.bat start
echo.
echo 📚 For more information, see TESTING.md
echo.

endlocal