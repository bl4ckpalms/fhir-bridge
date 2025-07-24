@echo off
REM =============================================================================
REM Database Backup Script for FHIR Bridge (Windows)
REM =============================================================================
REM This script creates backups of the PostgreSQL database
REM Usage: backup-database.bat [environment] [backup-type]
REM =============================================================================

setlocal enabledelayedexpansion

REM Configuration
set BACKUP_DIR=%BACKUP_DIR%||C:\opt\backups\postgresql
set LOG_FILE=%LOG_FILE%||C:\var\log\fhir-bridge\backup.log
set RETENTION_DAYS=%RETENTION_DAYS%||30
set TIMESTAMP=%date:~-4%%date:~4,2%%date:~7,2%_%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%

REM Default values
set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=production
set BACKUP_TYPE=%2
if "%BACKUP_TYPE%"=="" set BACKUP_TYPE=full

REM Create directories
if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"
if not exist "%LOG_FILE%" (
    for %%F in ("%LOG_FILE%") do mkdir "%%~dpF" 2>nul
)

REM Logging function
:log
echo [%date% %time%] %~1 >> "%LOG_FILE%"
echo [%date% %time%] %~1
goto :eof

REM Error handling
:error_exit
call :log "ERROR: %~1"
exit /b 1

REM Check dependencies
:check_dependencies
where pg_dump >nul 2>nul || call :error_exit "pg_dump not found in PATH"
where gzip >nul 2>nul || call :error_exit "gzip not found in PATH"
goto :eof

REM Get database configuration
:get_db_config
if "%ENVIRONMENT%"=="local" (
    set DB_HOST=%DB_HOST%||localhost
    set DB_PORT=%DB_PORT%||5432
    set DB_NAME=%DB_NAME%||fhir_bridge
    set DB_USER=%DB_USER%||fhir_user
    set DB_PASSWORD=%DB_PASSWORD%||fhir_password
) else if "%ENVIRONMENT%"=="staging" (
    set DB_HOST=%STAGING_DB_HOST%
    set DB_PORT=%STAGING_DB_PORT%||5432
    set DB_NAME=%STAGING_DB_NAME%||fhirbridge_staging
    set DB_USER=%STAGING_DB_USER%
    set DB_PASSWORD=%STAGING_DB_PASSWORD%
) else if "%ENVIRONMENT%"=="production" (
    set DB_HOST=%PRODUCTION_DB_HOST%
    set DB_PORT=%PRODUCTION_DB_PORT%||5432
    set DB_NAME=%PRODUCTION_DB_NAME%||fhirbridge
    set DB_USER=%PRODUCTION_DB_USER%
    set DB_PASSWORD=%PRODUCTION_DB_PASSWORD%
) else (
    call :error_exit "Unknown environment: %ENVIRONMENT%"
)

if "%DB_HOST%"=="" call :error_exit "DB_HOST not set for environment: %ENVIRONMENT%"
if "%DB_NAME%"=="" call :error_exit "DB_NAME not set for environment: %ENVIRONMENT%"
if "%DB_USER%"=="" call :error_exit "DB_USER not set for environment: %ENVIRONMENT%"
if "%DB_PASSWORD%"=="" call :error_exit "DB_PASSWORD not set for environment: %ENVIRONMENT%"
goto :eof

REM Create backup
:create_backup
call :log "Starting %BACKUP_TYPE% backup for environment: %ENVIRONMENT%"
set BACKUP_FILE=%BACKUP_DIR%\fhir_bridge_%ENVIRONMENT%_%BACKUP_TYPE%_%TIMESTAMP%.sql

if "%BACKUP_TYPE%"=="full" (
    set PG_DUMP_ARGS=--verbose --clean --if-exists --no-owner --no-privileges --no-tablespaces
) else if "%BACKUP_TYPE%"=="schema" (
    set PG_DUMP_ARGS=--verbose --schema-only --no-owner --no-privileges --no-tablespaces
) else if "%BACKUP_TYPE%"=="data" (
    set PG_DUMP_ARGS=--verbose --data-only --no-owner --no-privileges
) else (
    set PG_DUMP_ARGS=--verbose --clean --if-exists --no-owner --no-privileges --no-tablespaces
)

set PGPASSWORD=%DB_PASSWORD%
pg_dump -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% %PG_DUMP_ARGS% > "%BACKUP_FILE%" 2>> "%LOG_FILE%"
if errorlevel 1 call :error_exit "Backup failed"

REM Compress backup
gzip "%BACKUP_FILE%"
set BACKUP_FILE=%BACKUP_FILE%.gz

call :log "Backup created: %BACKUP_FILE%"
goto :eof

REM Clean old backups
:clean_old_backups
call :log "Cleaning backups older than %RETENTION_DAYS% days..."
forfiles /p "%BACKUP_DIR%" /m fhir_bridge_%ENVIRONMENT%_*.sql.gz /d -%RETENTION_DAYS% /c "cmd /c del @path"
goto :eof

REM Health check
:health_check
call :log "Performing health check..."
set PGPASSWORD=%DB_PASSWORD%
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT 1;" >nul 2>nul
if errorlevel 1 call :error_exit "Database connection failed"
call :log "Health check passed"
goto :eof

REM Main execution
:main
call :log "Starting database backup process..."
call :check_dependencies
call :get_db_config
call :health_check
call :create_backup
call :clean_old_backups
call :log "Backup completed successfully: %BACKUP_FILE%"
goto :eof

call :main