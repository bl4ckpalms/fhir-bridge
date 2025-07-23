@echo off
REM =============================================================================
REM Local Development Setup Script for FHIR Bridge (Windows)
REM =============================================================================

setlocal enabledelayedexpansion

REM Colors for output (limited in Windows batch)
set "INFO=[INFO]"
set "SUCCESS=[SUCCESS]"
set "WARNING=[WARNING]"
set "ERROR=[ERROR]"

echo ==============================================
echo FHIR Bridge Local Development Setup
echo ==============================================

REM Check prerequisites
echo %INFO% Checking prerequisites...

REM Check Docker
docker --version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Docker is not installed. Please install Docker first.
    exit /b 1
)

REM Check Docker Compose
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Docker Compose is not installed. Please install Docker Compose first.
    exit /b 1
)

REM Check Java
java -version >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Java is not installed. Please install Java 17 or later.
    exit /b 1
)

echo %SUCCESS% All prerequisites are installed

REM Check port availability
echo %INFO% Checking port availability...

netstat -an | findstr :5432 >nul 2>&1
if not errorlevel 1 (
    echo %WARNING% Port 5432 is already in use. You may need to stop conflicting services.
)

netstat -an | findstr :6379 >nul 2>&1
if not errorlevel 1 (
    echo %WARNING% Port 6379 is already in use. You may need to stop conflicting services.
)

netstat -an | findstr :8080 >nul 2>&1
if not errorlevel 1 (
    echo %WARNING% Port 8080 is already in use. You may need to stop conflicting services.
)

REM Start Docker services
echo %INFO% Starting Docker services...

REM Stop any existing services first
docker-compose -f docker-compose.local.yml down >nul 2>&1

REM Start dependencies
docker-compose -f docker-compose.local.yml up -d postgres redis

REM Wait for services to be ready
echo %INFO% Waiting for services to be ready...
timeout /t 10 /nobreak >nul

REM Test database connection
echo %INFO% Testing database connection...
docker-compose -f docker-compose.local.yml exec -T postgres pg_isready -U fhir_user -d fhir_bridge >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Database connection failed
    exit /b 1
) else (
    echo %SUCCESS% Database connection successful
)

REM Test Redis connection
echo %INFO% Testing Redis connection...
docker-compose -f docker-compose.local.yml exec -T redis redis-cli ping >nul 2>&1
if errorlevel 1 (
    echo %ERROR% Redis connection failed
    exit /b 1
) else (
    echo %SUCCESS% Redis connection successful
)

REM Handle arguments
if "%1"=="--build" (
    echo %INFO% Building application...
    call mvnw.cmd clean compile -DskipTests
    if errorlevel 1 (
        echo %ERROR% Build failed
        exit /b 1
    ) else (
        echo %SUCCESS% Application built successfully
    )
)

if "%1"=="--test" (
    echo %INFO% Running tests...
    call mvnw.cmd test -Dspring.profiles.active=test
    if errorlevel 1 (
        echo %ERROR% Tests failed
        exit /b 1
    ) else (
        echo %SUCCESS% Tests completed successfully
    )
)

echo.
echo ==============================================
echo %SUCCESS% Local development environment is ready!
echo ==============================================
echo.
echo Services running:
echo   - PostgreSQL: localhost:5432
echo   - Redis: localhost:6379
echo.
echo To start the application:
echo   mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
echo.
echo To start with admin tools:
echo   docker-compose -f docker-compose.local.yml --profile admin up -d
echo   - pgAdmin: http://localhost:5050
echo   - Redis Commander: http://localhost:8082
echo.
echo To stop services:
echo   docker-compose -f docker-compose.local.yml down
echo.
echo For more information, see DOCKER-LOCAL.md

endlocal