@echo off
REM =============================================================================
REM Docker Build Script for FHIR Bridge (Windows)
REM Supports multiple build targets and environments
REM =============================================================================

setlocal enabledelayedexpansion

REM Default values
set BUILD_TARGET=production
set IMAGE_TAG=latest
set PUSH_IMAGE=false
set REGISTRY=
set BUILD_ARGS=

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :build_start
if "%~1"=="-t" (
    set BUILD_TARGET=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--target" (
    set BUILD_TARGET=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-i" (
    set IMAGE_TAG=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--image" (
    set IMAGE_TAG=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-p" (
    set PUSH_IMAGE=true
    shift
    goto :parse_args
)
if "%~1"=="--push" (
    set PUSH_IMAGE=true
    shift
    goto :parse_args
)
if "%~1"=="-r" (
    set REGISTRY=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="--registry" (
    set REGISTRY=%~2
    shift
    shift
    goto :parse_args
)
if "%~1"=="-h" goto :show_usage
if "%~1"=="--help" goto :show_usage

echo [ERROR] Unknown option: %~1
goto :show_usage

:show_usage
echo Usage: %~nx0 [OPTIONS]
echo.
echo Build Docker images for FHIR Bridge application
echo.
echo OPTIONS:
echo     -t, --target TARGET     Build target (production, development) [default: production]
echo     -i, --image TAG         Image tag [default: latest]
echo     -p, --push              Push image to registry after build
echo     -r, --registry URL      Registry URL for pushing
echo     -h, --help              Show this help message
echo.
echo EXAMPLES:
echo     # Build production image
echo     %~nx0 --target production --image fhir-bridge:1.0.0
echo.
echo     # Build development image
echo     %~nx0 --target development --image fhir-bridge:dev
echo.
echo     # Build and push to registry
echo     %~nx0 --target production --image fhir-bridge:1.0.0 --push --registry myregistry.com
goto :eof

:build_start
REM Validate build target
if not "%BUILD_TARGET%"=="production" if not "%BUILD_TARGET%"=="development" (
    echo [ERROR] Invalid build target: %BUILD_TARGET%. Must be 'production' or 'development'
    exit /b 1
)

REM Set Dockerfile based on target
if "%BUILD_TARGET%"=="development" (
    set DOCKERFILE=Dockerfile.dev
) else (
    set DOCKERFILE=Dockerfile
)

REM Construct full image name
if not "%REGISTRY%"=="" (
    set FULL_IMAGE_NAME=%REGISTRY%/%IMAGE_TAG%
) else (
    set FULL_IMAGE_NAME=%IMAGE_TAG%
)

echo [INFO] Starting Docker build...
echo [INFO] Build target: %BUILD_TARGET%
echo [INFO] Dockerfile: %DOCKERFILE%
echo [INFO] Image name: %FULL_IMAGE_NAME%

REM Check if Dockerfile exists
if not exist "%DOCKERFILE%" (
    echo [ERROR] Dockerfile not found: %DOCKERFILE%
    exit /b 1
)

REM Build the image
echo [INFO] Building Docker image...
docker build -f "%DOCKERFILE%" -t "%FULL_IMAGE_NAME%" %BUILD_ARGS% .
if errorlevel 1 (
    echo [ERROR] Docker build failed
    exit /b 1
)

echo [SUCCESS] Docker image built successfully: %FULL_IMAGE_NAME%

REM Show image information
echo [INFO] Image information:
docker images "%FULL_IMAGE_NAME%"

REM Push image if requested
if "%PUSH_IMAGE%"=="true" (
    if "%REGISTRY%"=="" (
        echo [WARNING] Registry not specified, skipping push
    ) else (
        echo [INFO] Pushing image to registry...
        docker push "%FULL_IMAGE_NAME%"
        if errorlevel 1 (
            echo [ERROR] Failed to push image
            exit /b 1
        )
        echo [SUCCESS] Image pushed successfully: %FULL_IMAGE_NAME%
    )
)

REM Run basic validation
echo [INFO] Running basic validation...
docker run --rm "%FULL_IMAGE_NAME%" java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Image validation failed
    exit /b 1
)

echo [SUCCESS] Image validation passed
echo [SUCCESS] Build process completed successfully!

echo.
echo Next Steps:
echo 1. Test the image locally:
echo    docker run --rm -p 8080:8080 -p 8081:8081 %FULL_IMAGE_NAME%
echo.
echo 2. Run with docker-compose:
echo    docker-compose up
echo.
echo 3. Check application health:
echo    curl http://localhost:8081/actuator/health

:eof