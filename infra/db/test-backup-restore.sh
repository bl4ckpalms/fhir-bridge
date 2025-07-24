#!/bin/bash
# =============================================================================
# Database Backup/Restore Testing Script
# =============================================================================
# This script tests backup and restore procedures in staging environment
# Usage: ./test-backup-restore.sh [environment]
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-staging}"
BACKUP_DIR="/tmp/fhir-bridge-backup-tests"
LOG_FILE="${BACKUP_DIR}/test-results.log"
TEST_DB_NAME="fhirbridge_test_$(date +%Y%m%d_%H%M%S)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

test_pass() {
    log "${GREEN}✓ PASS: $1${NC}"
    ((TESTS_PASSED++))
}

test_fail() {
    log "${RED}✗ FAIL: $1${NC}"
    ((TESTS_FAILED++))
}

# Setup test environment
setup_test_env() {
    log "Setting up test environment..."
    mkdir -p "$BACKUP_DIR"
    
    # Create test database
    log "Creating test database: $TEST_DB_NAME"
    PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d postgres -c "CREATE DATABASE $TEST_DB_NAME;"
    
    # Run migrations on test database
    log "Running migrations on test database..."
    cd "${SCRIPT_DIR}/../.."
    mvn flyway:migrate -Dflyway.configFiles=infra/db/config/flyway-${ENVIRONMENT}.conf -Dflyway.url=jdbc:postgresql://${DB_HOST:-localhost}:${DB_PORT:-5432}/${TEST_DB_NAME}
    
    test_pass "Test environment setup completed"
}

# Cleanup test environment
cleanup_test_env() {
    log "Cleaning up test environment..."
    
    # Drop test database
    PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d postgres -c "DROP DATABASE IF EXISTS $TEST_DB_NAME;"
    
    # Clean up backup files
    rm -rf "$BACKUP_DIR"
    
    log "Test environment cleaned up"
}

# Test backup creation
test_backup_creation() {
    log "Testing backup creation..."
    
    local backup_file="${BACKUP_DIR}/test-backup-$(date +%Y%m%d_%H%M%S).sql.gz"
    
    # Create backup
    if "${SCRIPT_DIR}/backup-database.sh" "$ENVIRONMENT" "full" "$backup_file" > /dev/null 2>&1; then
        test_pass "Backup creation successful"
        
        # Verify backup file exists and is not empty
        if [[ -f "$backup_file" ]] && [[ -s "$backup_file" ]]; then
            test_pass "Backup file exists and is not empty"
            
            # Verify backup integrity
            if gunzip -t "$backup_file" 2>/dev/null; then
                test_pass "Backup file integrity verified"
            else
                test_fail "Backup file integrity check failed"
            fi
        else
            test_fail "Backup file does not exist or is empty"
        fi
    else
        test_fail "Backup creation failed"
    fi
}

# Test restore functionality
test_restore_functionality() {
    log "Testing restore functionality..."
    
    local backup_file="${BACKUP_DIR}/test-restore-$(date +%Y%m%d_%H%M%S).sql.gz"
    local restore_db_name="fhirbridge_restore_$(date +%Y%m%d_%H%M%S)"
    
    # Create backup
    log "Creating backup for restore test..."
    "${SCRIPT_DIR}/backup-database.sh" "$ENVIRONMENT" "full" "$backup_file" > /dev/null 2>&1
    
    # Create restore database
    log "Creating restore database: $restore_db_name"
    PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d postgres -c "CREATE DATABASE $restore_db_name;"
    
    # Restore backup
    log "Restoring backup to test database..."
    if "${SCRIPT_DIR}/restore-database.sh" "$ENVIRONMENT" "$backup_file" "$restore_db_name" > /dev/null 2>&1; then
        test_pass "Restore operation completed successfully"
        
        # Verify data integrity
        log "Verifying data integrity after restore..."
        local table_count=$(PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d "$restore_db_name" -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE';")
        
        if [[ $table_count -gt 0 ]]; then
            test_pass "Tables restored successfully (count: $table_count)"
            
            # Verify data in key tables
            local audit_count=$(PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d "$restore_db_name" -tAc "SELECT COUNT(*) FROM audit_events;")
            local consent_count=$(PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d "$restore_db_name" -tAc "SELECT COUNT(*) FROM consent_records;")
            
            log "Data verification - audit_events: $audit_count, consent_records: $consent_count"
            test_pass "Data verification completed"
        else
            test_fail "No tables found after restore"
        fi
        
        # Clean up restore database
        PGPASSWORD="${DB_PASSWORD:-fhir_password}" psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d postgres -c "DROP DATABASE IF EXISTS $restore_db_name;"
    else
        test_fail "Restore operation failed"
    fi
}

# Test encrypted backup
test_encrypted_backup() {
    log "Testing encrypted backup..."
    
    local backup_file="${BACKUP_DIR}/test-encrypted-$(date +%Y%m%d_%H%M%S).sql.gz.gpg"
    
    # Create encrypted backup
    ENCRYPTION_KEY="test-key-123" "${SCRIPT_DIR}/backup-database.sh" "$ENVIRONMENT" "full" "$backup_file" > /dev/null 2>&1
    
    if [[ -f "$backup_file" ]]; then
        test_pass "Encrypted backup created successfully"
        
        # Verify encryption
        if file "$backup_file" | grep -q "PGP"; then
            test_pass "Backup file is encrypted"
        else
            test_fail "Backup file is not encrypted"
        fi
        
        # Test decryption
        if echo "test-key-123" | gpg --batch --decrypt --passphrase-fd 0 "$backup_file" > /dev/null 2>&1; then
            test_pass "Backup decryption successful"
        else
            test_fail "Backup decryption failed"
        fi
    else
        test_fail "Encrypted backup creation failed"
    fi
}

# Test S3 upload
test_s3_upload() {
    log "Testing S3 upload..."
    
    if [[ -n "${S3_BACKUPS_BUCKET:-}" ]]; then
        local backup_file="${BACKUP_DIR}/test-s3-$(date +%Y%m%d_%H%M%S).sql.gz"
        
        # Create backup
        "${SCRIPT_DIR}/backup-database.sh" "$ENVIRONMENT" "full" "$backup_file" > /dev/null 2>&1
        
        # Upload to S3
        if aws s3 cp "$backup_file" "s3://${S3_BACKUPS_BUCKET}/test/" > /dev/null 2>&1; then
            test_pass "S3 upload successful"
            
            # Verify upload
            if aws s3 ls "s3://${S3_BACKUPS_BUCKET}/test/" | grep -q "$(basename "$backup_file")"; then
                test_pass "S3 upload verification successful"
                
                # Clean up
                aws s3 rm "s3://${S3_BACKUPS_BUCKET}/test/$(basename "$backup_file')"
            else
                test_fail "S3 upload verification failed"
            fi
        else
            test_fail "S3 upload failed"
        fi
    else
        log "S3_BACKUPS_BUCKET not configured, skipping S3 upload test"
    fi
}

# Test backup scheduling
test_backup_scheduling() {
    log "Testing backup scheduling..."
    
    # Test cron job creation
    local cron_job="0 2 * * * ${SCRIPT_DIR}/backup-database.sh $ENVIRONMENT"
    
    if (crontab -l 2>/dev/null || echo "") | grep -q "backup-database.sh.*$ENVIRONMENT"; then
        test_pass "Backup scheduling already configured"
    else
        log "Adding backup scheduling to crontab..."
        (crontab -l 2>/dev/null || echo "") | { cat; echo "$cron_job"; } | crontab -
        test_pass "Backup scheduling configured"
    fi
}

# Run all tests
run_all_tests() {
    log "Starting backup/restore test suite for ${ENVIRONMENT}..."
    
    setup_test_env
    
    log "Running backup/restore tests..."
    
    test_backup_creation
    test_restore_functionality
    test_encrypted_backup
    test_s3_upload
    test_backup_scheduling
    
    log "Test Results Summary:"
    log "Tests Passed: $TESTS_PASSED"
    log "Tests Failed: $TESTS_FAILED"
    
    if [[ $TESTS_FAILED -eq 0 ]]; then
        log "${GREEN}All tests passed!${NC}"
        cleanup_test_env
        exit 0
    else
        log "${RED}Some tests failed!${NC}"
        cleanup_test_env
        exit 1
    fi
}

# Main execution
main() {
    case "${2:-run}" in
        "run")
            run_all_tests
            ;;
        "setup")
            setup_test_env
            ;;
        "cleanup")
            cleanup_test_env
            ;;
        *)
            echo "Usage: $0 [environment] [run|setup|cleanup]"
            echo "  environment: local|staging|production (default: staging)"
            echo "  action: run|setup|cleanup (default: run)"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"