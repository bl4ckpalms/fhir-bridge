#!/bin/bash
# =============================================================================
# Database Restore Script for FHIR Bridge
# =============================================================================
# This script restores PostgreSQL database from encrypted backups
# Usage: ./restore-database.sh [environment] [backup-file]
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/opt/backups/postgresql}"
LOG_FILE="${LOG_FILE:-/var/log/fhir-bridge/restore.log}"
ENCRYPTION_KEY="${ENCRYPTION_KEY:-}"
S3_BUCKET="${S3_BACKUPS_BUCKET:-}"
AWS_REGION="${AWS_REGION:-us-east-1}"

# Default values
ENVIRONMENT="${1:-production}"
BACKUP_FILE="${2:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

error_exit() {
    log "${RED}ERROR: $1${NC}"
    exit 1
}

# Check dependencies
check_dependencies() {
    local deps=("psql" "pg_restore" "gunzip" "aws" "gpg")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            error_exit "Required dependency '$dep' not found"
        fi
    done
}

# Get database connection details
get_db_config() {
    case $ENVIRONMENT in
        "local")
            DB_HOST="${DB_HOST:-localhost}"
            DB_PORT="${DB_PORT:-5432}"
            DB_NAME="${DB_NAME:-fhir_bridge}"
            DB_USER="${DB_USER:-fhir_user}"
            DB_PASSWORD="${DB_PASSWORD:-fhir_password}"
            ;;
        "staging")
            DB_HOST="${STAGING_DB_HOST}"
            DB_PORT="${STAGING_DB_PORT:-5432}"
            DB_NAME="${STAGING_DB_NAME:-fhirbridge_staging}"
            DB_USER="${STAGING_DB_USER}"
            DB_PASSWORD="${STAGING_DB_PASSWORD}"
            ;;
        "production")
            DB_HOST="${PRODUCTION_DB_HOST}"
            DB_PORT="${PRODUCTION_DB_PORT:-5432}"
            DB_NAME="${PRODUCTION_DB_NAME:-fhirbridge}"
            DB_USER="${PRODUCTION_DB_USER}"
            DB_PASSWORD="${PRODUCTION_DB_PASSWORD}"
            ;;
        *)
            error_exit "Unknown environment: $ENVIRONMENT"
            ;;
    esac

    # Validate required variables
    [[ -z "$DB_HOST" ]] && error_exit "DB_HOST not set for environment: $ENVIRONMENT"
    [[ -z "$DB_NAME" ]] && error_exit "DB_NAME not set for environment: $ENVIRONMENT"
    [[ -z "$DB_USER" ]] && error_exit "DB_USER not set for environment: $ENVIRONMENT"
    [[ -z "$DB_PASSWORD" ]] && error_exit "DB_PASSWORD not set for environment: $ENVIRONMENT"
}

# Find backup file
find_backup_file() {
    if [[ -z "$BACKUP_FILE" ]]; then
        log "Finding latest backup..."
        
        # Check local backups
        local latest_local=$(find "$BACKUP_DIR" -name "fhir_bridge_${ENVIRONMENT}_*.sql.gz*" -type f -printf '%T@ %p\n' | sort -n | tail -1 | cut -d' ' -f2-)
        
        # Check S3 backups
        if [[ -n "$S3_BUCKET" ]]; then
            local latest_s3=$(aws s3 ls "s3://${S3_BUCKET}/postgresql/${ENVIRONMENT}/" --region "$AWS_REGION" | \
                grep "fhir_bridge_${ENVIRONMENT}_" | \
                awk '{print $4}' | \
                sort -r | \
                head -1)
            
            if [[ -n "$latest_s3" ]]; then
                log "Downloading latest backup from S3: $latest_s3"
                aws s3 cp "s3://${S3_BUCKET}/postgresql/${ENVIRONMENT}/$latest_s3" "$BACKUP_DIR/"
                BACKUP_FILE="$BACKUP_DIR/$latest_s3"
            elif [[ -n "$latest_local" ]]; then
                BACKUP_FILE="$latest_local"
            else
                error_exit "No backup files found"
            fi
        elif [[ -n "$latest_local" ]]; then
            BACKUP_FILE="$latest_local"
        else
            error_exit "No backup files found"
        fi
    fi
    
    [[ ! -f "$BACKUP_FILE" ]] && error_exit "Backup file not found: $BACKUP_FILE"
    log "Using backup file: $BACKUP_FILE"
}

# Decrypt backup if necessary
decrypt_backup() {
    if [[ "$BACKUP_FILE" == *.gpg ]]; then
        log "Decrypting backup..."
        local decrypted_file="${BACKUP_FILE%.gpg}"
        gpg --batch --decrypt --passphrase "$ENCRYPTION_KEY" "$BACKUP_FILE" > "$decrypted_file"
        BACKUP_FILE="$decrypted_file"
        log "Backup decrypted: $BACKUP_FILE"
    fi
}

# Pre-restore checks
pre_restore_checks() {
    log "Performing pre-restore checks..."
    
    # Check database connectivity
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1 || \
        error_exit "Database connection failed"
    
    # Check if database exists
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
        log "Database $DB_NAME exists"
        
        # Check if tables exist
        local table_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc \
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';")
        
        if [[ $table_count -gt 0 ]]; then
            log "WARNING: Database contains $table_count tables that will be dropped"
            read -p "Continue with restore? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                log "Restore cancelled by user"
                exit 0
            fi
        fi
    else
        log "Database $DB_NAME does not exist, will be created"
    fi
}

# Create database if it doesn't exist
create_database() {
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -lqt | cut -d \| -f 1 | grep -qw "$DB_NAME"; then
        log "Creating database $DB_NAME..."
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -c "CREATE DATABASE $DB_NAME;"
    fi
}

# Restore database
restore_database() {
    log "Starting database restore..."
    
    local psql_args=(
        "-h" "$DB_HOST"
        "-p" "$DB_PORT"
        "-U" "$DB_USER"
        "-d" "$DB_NAME"
        "--no-password"
        "--quiet"
    )
    
    if [[ "$BACKUP_FILE" == *.gz ]]; then
        log "Restoring from compressed backup..."
        gunzip -c "$BACKUP_FILE" | PGPASSWORD="$DB_PASSWORD" psql "${psql_args[@]}"
    elif [[ "$BACKUP_FILE" == *.sql ]]; then
        log "Restoring from SQL backup..."
        PGPASSWORD="$DB_PASSWORD" psql "${psql_args[@]}" < "$BACKUP_FILE"
    else
        error_exit "Unsupported backup file format: $BACKUP_FILE"
    fi
    
    log "Database restore completed"
}

# Post-restore verification
post_restore_verification() {
    log "Performing post-restore verification..."
    
    # Check table counts
    local audit_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT COUNT(*) FROM audit_events;")
    local consent_count=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT COUNT(*) FROM consent_records;")
    
    log "Verification results:"
    log "  - audit_events: $audit_count records"
    log "  - consent_records: $consent_count records"
    
    # Run health check
    local health_result=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -tAc \
        "SELECT health_check();")
    log "  - Health check: $health_result"
    
    log "Post-restore verification completed"
}

# Main execution
main() {
    log "Starting database restore process..."
    
    check_dependencies
    get_db_config
    find_backup_file
    decrypt_backup
    pre_restore_checks
    create_database
    restore_database
    post_restore_verification
    
    log "${GREEN}Database restore completed successfully${NC}"
}

# Execute main function
main "$@"