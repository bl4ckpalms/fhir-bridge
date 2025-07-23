@echo off
REM =============================================================================
REM FHIR Bridge Test Environment Management Script (Windows)
REM Manages Docker Compose testing environment
REM =============================================================================

setlocal enabledelayedexpansion

echo.
echo ========================================
echo FHIR Bridge Test Environment Manager
echo ========================================
echo.

REM Check if Docker is available
docker --version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker is not installed or not in PATH
    echo Please install Docker Desktop and try again
    exit /b 1
)

REM Check if Docker Compose is available
docker compose version >nul 2>&1
if errorlevel 1 (
    echo ❌ Docker Compose is not available
    echo Please ensure Docker Desktop is running
    exit /b 1
)

if "%1"=="" (
    goto :show_help
)

if "%1"=="start" goto :start_environment
if "%1"=="stop" goto :stop_environment
if "%1"=="restart" goto :restart_environment
if "%1"=="logs" goto :show_logs
if "%1"=="status" goto :show_status
if "%1"=="clean" goto :clean_environment
if "%1"=="test" goto :run_tests
if "%1"=="generate-data" goto :generate_data
if "%1"=="simulate" goto :simulate_messages
if "%1"=="help" goto :show_help

echo ❌ Unknown command: %1
goto :show_help

:start_environment
echo 🚀 Starting FHIR Bridge test environment...
echo.

REM Start core services (database and cache)
echo 📊 Starting core services (PostgreSQL and Redis)...
docker compose -f docker-compose.test.yml up -d postgres-test redis-test

REM Wait for services to be healthy
echo ⏳ Waiting for services to be ready...
timeout /t 10 /nobreak >nul

REM Start the application
echo 🏥 Starting FHIR Bridge application...
docker compose -f docker-compose.test.yml up -d fhir-bridge-test

echo.
echo ✅ Test environment started successfully!
echo.
echo 🔗 Access points:
echo    • FHIR Bridge API: http://localhost:8083
echo    • Health Check: http://localhost:8084/actuator/health
echo    • PostgreSQL: localhost:5433 (user: fhir_test_user, db: fhir_bridge_test)
echo    • Redis: localhost:6380
echo.
echo 💡 Use 'test-environment.bat status' to check service health
echo 💡 Use 'test-environment.bat logs' to view application logs
goto :end

:stop_environment
echo 🛑 Stopping FHIR Bridge test environment...
docker compose -f docker-compose.test.yml down
echo ✅ Test environment stopped
goto :end

:restart_environment
echo 🔄 Restarting FHIR Bridge test environment...
docker compose -f docker-compose.test.yml restart
echo ✅ Test environment restarted
goto :end

:show_logs
if "%2"=="" (
    echo 📋 Showing logs for all services...
    docker compose -f docker-compose.test.yml logs -f
) else (
    echo 📋 Showing logs for %2...
    docker compose -f docker-compose.test.yml logs -f %2
)
goto :end

:show_status
echo 📊 Test environment status:
echo.
docker compose -f docker-compose.test.yml ps
echo.
echo 🏥 Service health checks:
echo.

REM Check PostgreSQL
echo Checking PostgreSQL...
docker compose -f docker-compose.test.yml exec -T postgres-test pg_isready -U fhir_test_user -d fhir_bridge_test >nul 2>&1
if errorlevel 1 (
    echo    ❌ PostgreSQL: Not ready
) else (
    echo    ✅ PostgreSQL: Ready
)

REM Check Redis
echo Checking Redis...
docker compose -f docker-compose.test.yml exec -T redis-test redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo    ❌ Redis: Not ready
) else (
    echo    ✅ Redis: Ready
)

REM Check FHIR Bridge
echo Checking FHIR Bridge...
curl -f http://localhost:8084/actuator/health >nul 2>&1
if errorlevel 1 (
    echo    ❌ FHIR Bridge: Not ready
) else (
    echo    ✅ FHIR Bridge: Ready
)
goto :end

:clean_environment
echo 🧹 Cleaning up test environment...
echo.
echo ⚠️  This will remove all containers, volumes, and test data!
set /p confirm="Are you sure? (y/N): "
if /i not "%confirm%"=="y" (
    echo Cancelled
    goto :end
)

docker compose -f docker-compose.test.yml down -v --remove-orphans
docker system prune -f
echo ✅ Test environment cleaned up
goto :end

:run_tests
echo 🧪 Running integration tests...
docker compose -f docker-compose.test.yml --profile integration-tests up --build integration-tests
goto :end

:generate_data
echo 📝 Generating test data...
docker compose -f docker-compose.test.yml --profile test-data up --build test-data-generator
goto :end

:simulate_messages
echo 📤 Simulating HL7 messages...
if "%2"=="--continuous" (
    echo 🔄 Starting continuous simulation mode...
    docker compose -f docker-compose.test.yml --profile simulator run --rm hl7-simulator sh -c "apk add --no-cache curl jq && /scripts/simulate-hl7-messages.sh --continuous"
) else (
    docker compose -f docker-compose.test.yml --profile simulator run --rm hl7-simulator sh -c "apk add --no-cache curl jq && /scripts/simulate-hl7-messages.sh"
)
goto :end

:show_help
echo.
echo Usage: test-environment.bat [COMMAND] [OPTIONS]
echo.
echo Commands:
echo   start           Start the test environment
echo   stop            Stop the test environment  
echo   restart         Restart the test environment
echo   status          Show status of all services
echo   logs [service]  Show logs (optionally for specific service)
echo   clean           Clean up all containers and volumes
echo   test            Run integration tests
echo   generate-data   Generate test data
echo   simulate        Simulate HL7 message sending
echo   help            Show this help message
echo.
echo Examples:
echo   test-environment.bat start
echo   test-environment.bat logs fhir-bridge-test
echo   test-environment.bat simulate --continuous
echo.
echo Services:
echo   • fhir-bridge-test    - Main application
echo   • postgres-test       - PostgreSQL database
echo   • redis-test          - Redis cache
echo.
echo Optional services (use --profile flag):
echo   • pgadmin-test        - PostgreSQL admin interface
echo   • redis-commander-test - Redis admin interface
echo   • integration-tests   - Test runner
echo   • test-data-generator - Test data generator
echo   • hl7-simulator       - HL7 message simulator
echo.

:end
echo.
endlocal