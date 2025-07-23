@echo off
REM =============================================================================
REM Development Environment Setup Script for FHIR Bridge (Windows)
REM =============================================================================

setlocal enabledelayedexpansion

REM Check if Docker and Docker Compose are installed
echo [INFO] Checking prerequisites...
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed. Please install Docker first.
    exit /b 1
)

docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose is not installed. Please install Docker Compose first.
    exit /b 1
)

echo [SUCCESS] Prerequisites check passed

REM Create necessary directories
echo [INFO] Creating necessary directories...
if not exist "logs" mkdir logs
if not exist "target\test-results" mkdir target\test-results
if not exist "target\coverage-reports" mkdir target\coverage-reports
if not exist "target\integration-test-results" mkdir target\integration-test-results
if not exist "target\performance-results" mkdir target\performance-results
echo [SUCCESS] Directories created

REM Handle command line arguments
set COMMAND=%1
if "%COMMAND%"=="" set COMMAND=setup

if "%COMMAND%"=="setup" goto setup
if "%COMMAND%"=="start" goto start
if "%COMMAND%"=="stop" goto stop
if "%COMMAND%"=="restart" goto restart
if "%COMMAND%"=="test" goto test
if "%COMMAND%"=="logs" goto logs
if "%COMMAND%"=="cleanup" goto cleanup
if "%COMMAND%"=="help" goto help

echo [ERROR] Unknown command: %COMMAND%
goto help

:setup
echo [INFO] Building the application...
if exist "mvnw.cmd" (
    call mvnw.cmd clean compile -DskipTests
    echo [SUCCESS] Application built successfully
) else (
    echo [WARNING] Maven wrapper not found, skipping build
)

:start
echo [INFO] Starting development environment...

REM Stop any existing containers
docker-compose down --remove-orphans

REM Build and start services
docker-compose up --build -d

echo [INFO] Waiting for services to be ready...
timeout /t 30 /nobreak >nul

REM Check if services are running
docker-compose ps | findstr "Up" >nul
if errorlevel 1 (
    echo [ERROR] Some services failed to start properly
    docker-compose logs
    exit /b 1
) else (
    echo [SUCCESS] Development environment started successfully
    echo [INFO] Services available at:
    echo   - FHIR Bridge API: http://localhost:8080
    echo   - Management/Actuator: http://localhost:8081
    echo   - pgAdmin: http://localhost:5050 (admin@fhirbridge.local / admin123)
    echo   - Redis Commander: http://localhost:8082 (admin / admin123)
    echo   - Mailhog: http://localhost:8025
    echo   - Nginx Proxy: http://localhost:80
)
goto end

:stop
echo [INFO] Stopping development environment...
docker-compose down --remove-orphans
echo [SUCCESS] Development environment stopped
goto end

:restart
call :stop
call :start
goto end

:test
echo [INFO] Running tests...
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
if errorlevel 1 (
    echo [ERROR] Tests failed
    exit /b 1
) else (
    echo [SUCCESS] Tests completed successfully
)
goto end

:logs
echo [INFO] Showing application logs...
docker-compose logs -f fhir-bridge
goto end

:cleanup
echo [INFO] Cleaning up development environment...
docker-compose down --remove-orphans --volumes
docker system prune -f
echo [SUCCESS] Cleanup completed
goto end

:help
echo FHIR Bridge Development Environment Setup
echo.
echo Usage: %0 [COMMAND]
echo.
echo Commands:
echo   setup     - Set up and start the development environment
echo   start     - Start the development environment
echo   stop      - Stop the development environment
echo   restart   - Restart the development environment
echo   test      - Run tests
echo   logs      - Show application logs
echo   cleanup   - Clean up all containers and volumes
echo   help      - Show this help message
echo.
goto end

:end
endlocal