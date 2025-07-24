#!/bin/bash
# =============================================================================
# Database Backup Script for FHIR Bridge
# =============================================================================
# This script creates encrypted backups of the PostgreSQL database
# Usage: ./backup-database.sh [environment] [backup-type]
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKUP_DIR="${BACKUP_DIR:-/opt/backups/postgresql}"
LOG_FILE="${LOG_FILE:-/var/log/fhir-bridge/backup.log}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
ENCRYPTION_KEY="${ENCRYPTION_KEY:-}"
S3_BUCKET="${S3_BACKUPS_BUCKET:-}"
AWS_REGION="${AWS_REGION:-us-east-1}"

# Default values
ENVIRONMENT="${1:-production}"
BACKUP_TYPE="${2:-full}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="${BACKUP_DIR}/fhir_bridge_${ENVIRONMENT}_${BACKUP_TYPE}_${TIMESTAMP}.sql.gz"

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
    local deps=("pg_dump" "gzip" "aws" "gpg")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            error_exit "Required dependency '$dep' not found"
        fi
    done
}

# Create backup directory
create_backup_dir() {
    if [[ ! -d "$BACKUP_DIR" ]]; then
        mkdir -p "$BACKUP_DIR" || error_exit "Failed to create backup directory: $BACKUP_DIR"
        log "Created backup directory: $BACKUP_DIR"
    fi
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

# Create database backup
create_backup() {
    log "Starting ${BACKUP_TYPE} backup for environment: ${ENVIRONMENT}"
    
    local pg_dump_args=(
        "-h" "$DB_HOST"
        "-p" "$DB_PORT"
        "-U" "$DB_USER"
        "-d" "$DB_NAME"
        "--no-password"
        "--verbose"
        "--clean"
        "--if-exists"
        "--no-owner"
        "--no-privileges"
        "--no-tablespaces"
        "--no-unlogged-table-data"
    )

    case $BACKUP_TYPE in
        "full")
            pg_dump_args+=("--schema-only" "--schema=public")
            pg_dump "${pg_dump_args[@]}" | gzip > "${BACKUP_FILE}.schema"
            
            pg_dump_args=("${pg_dump_args[@]/--schema-only/--data-only}")
            pg_dump "${pg_dump_args[@]}" | gzip > "${BACKUP_FILE}.data"
            ;;
        "schema")
            pg_dump_args+=("--schema-only" "--schema=public")
            pg_dump "${pg_dump_args[@]}" | gzip > "$BACKUP_FILE"
            ;;
        "data")
            pg_dump_args+=("--data-only")
            pg_dump "${pg_dump_args[@]}" | gzip > "$BACKUP_FILE"
            ;;
        *)
            pg_dump "${pg_dump_args[@]}" | gzip > "$BACKUP_FILE"
            ;;
    esac

    log "Backup created: $BACKUP_FILE"
}

# Encrypt backup if encryption key is provided
encrypt_backup() {
    if [[ -n "$ENCRYPTION_KEY" ]]; then
        log "Encrypting backup..."
        gpg --batch --yes --symmetric --cipher-algo AES256 --passphrase "$ENCRYPTION_KEY" "$BACKUP_FILE"
        rm "$BACKUP_FILE"
        BACKUP_FILE="${BACKUP_FILE}.gpg"
        log "Backup encrypted: $BACKUP_FILE"
    fi
}

# Upload to S3 if bucket is configured
upload_to_s3() {
    if [[ -n "$S3_BUCKET" ]]; then
        log "Uploading backup to S3..."
        aws s3 cp "$BACKUP_FILE" "s3://${S3_BUCKET}/postgresql/${ENVIRONMENT}/" --region "$AWS_REGION"
        log "Backup uploaded to S3: s3://${S3_BUCKET}/postgresql/${ENVIRONMENT}/$(basename "$BACKUP_FILE")"
    fi
}

# Clean old backups
clean_old_backups() {
    log "Cleaning backups older than ${RETENTION_DAYS} days..."
    
    # Local cleanup
    find "$BACKUP_DIR" -name "fhir_bridge_${ENVIRONMENT}_*.sql.gz*" -mtime +${RETENTION_DAYS} -delete
    
    # S3 cleanup
    if [[ -n "$S3_BUCKET" ]]; then
        aws s3 ls "s3://${S3_BUCKET}/postgresql/${ENVIRONMENT}/" --region "$AWS_REGION" | \
        awk '{print $4}' | \
        while read -r file; do
            file_date=$(echo "$file" | grep -o '[0-9]\{8\}' | head -1)
            if [[ -n "$file_date" ]]; then
                file_age=$(( ($(date +%s) - $(date -d "$file_date" +%s)) / 86400 ))
                if [[ $file_age -gt $RETENTION_DAYS ]]; then
                    aws s3 rm "s3://${S3_BUCKET}/postgresql/${ENVIRONMENT}/$file" --region "$AWS_REGION"
                    log "Deleted old S3 backup: $file"
                fi
            fi
        done
    fi
}

# Health check
health_check() {
    log "Performing health check..."
    
    # Check database connectivity
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1;" > /dev/null 2>&1 || \
        error_exit "Database connection failed"
    
    # Check backup file integrity
    if [[ -f "$BACKUP_FILE" ]]; then
        if [[ "$BACKUP_FILE" == *.gpg ]]; then
            gpg --batch --decrypt --passphrase "$ENCRYPTION_KEY" "$BACKUP_FILE" | gunzip -t > /dev/null 2>&1 || \
                error_exit "Encrypted backup file integrity check failed"
        else
            gunzip -t "$BACKUP_FILE" > /dev/null 2>&1 || \
                error_exit "Backup file integrity check failed"
        fi
    fi
    
    log "Health check passed"
}

# Main execution
main() {
    log "Starting database backup process..."
    
    check_dependencies
    create_backup_dir
    get_db_config
    health_check
    create_backup
    encrypt_backup
    upload_to_s3
    clean_old_backups
    
    log "${GREEN}Backup completed successfully: $BACKUP_FILE${NC}"
}

# Execute main function
main "$@"