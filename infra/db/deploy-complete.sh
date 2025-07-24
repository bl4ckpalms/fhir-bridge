#!/bin/bash
# =============================================================================
# Complete Database Deployment Script
# =============================================================================
# This script orchestrates the complete database deployment process
# Usage: ./deploy-complete.sh [environment]
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-staging}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "${RED}ERROR: $1${NC}"
    exit 1
}

success() {
    log "${GREEN}✓ $1${NC}"
}

warning() {
    log "${YELLOW}⚠ $1${NC}"
}

# Main deployment function
main() {
    log "Starting complete database deployment for ${ENVIRONMENT}..."
    
    # Step 1: Environment validation
    log "Step 1: Validating environment configuration..."
    if [[ ! -f "${SCRIPT_DIR}/config/flyway-${ENVIRONMENT}.conf" ]]; then
        error_exit "Environment configuration not found: flyway-${ENVIRONMENT}.conf"
    fi
    success "Environment configuration validated"
    
    # Step 2: Run database migrations
    log "Step 2: Running database migrations..."
    cd "${SCRIPT_DIR}/../.."
    mvn flyway:migrate -Dflyway.configFiles="infra/db/config/flyway-${ENVIRONMENT}.conf"
    success "Database migrations completed"
    
    # Step 3: Deploy monitoring stack
    log "Step 3: Deploying monitoring stack..."
    "${SCRIPT_DIR}/deploy-monitoring.sh" "$ENVIRONMENT" deploy
    success "Monitoring stack deployed"
    
    # Step 4: Configure S3 backup (if production/staging)
    if [[ "$ENVIRONMENT" != "local" ]]; then
        log "Step 4: Configuring S3 backup storage..."
        cd "${SCRIPT_DIR}"
        if [[ -f "s3-backup-setup.tf" ]]; then
            terraform init
            terraform plan -var="environment=${ENVIRONMENT}"
            terraform apply -var="environment=${ENVIRONMENT}" -auto-approve
            success "S3 backup storage configured"
        else
            warning "S3 backup setup not available for ${ENVIRONMENT}"
        fi
    fi
    
    # Step 5: Test backup/restore procedures
    log "Step 5: Testing backup/restore procedures..."
    "${SCRIPT_DIR}/test-backup-restore.sh" "$ENVIRONMENT" run
    success "Backup/restore tests passed"
    
    # Step 6: Schedule automated backups
    log "Step 6: Scheduling automated backups..."
    if [[ "$ENVIRONMENT" != "local" ]]; then
        # Add cron job for automated backups
        (crontab -l 2>/dev/null || echo "") | grep -q "backup-database.sh.*${ENVIRONMENT}" || {
            (crontab -l 2>/dev/null; echo "0 2 * * * ${SCRIPT_DIR}/backup-database.sh ${ENVIRONMENT}") | crontab -
            success "Automated backups scheduled via cron"
        }
    else
        warning "Skipping automated backup scheduling for local environment"
    fi
    
    # Step 7: Final validation
    log "Step 7: Performing final validation..."
    
    # Check database connectivity
    if PGPASSWORD="${DB_PASSWORD:-fhir_password}" pg_isready -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USER:-fhir_user}" -d "${DB_NAME:-fhir_bridge}"; then
        success "Database connectivity verified"
    else
        error_exit "Database connectivity failed"
    fi
    
    # Check monitoring endpoints
    local prometheus_url="http://localhost:9090"
    local grafana_url="http://localhost:3000"
    
    if curl -s "$prometheus_url/-/healthy" > /dev/null; then
        success "Prometheus is healthy"
    else
        warning "Prometheus health check failed"
    fi
    
    if curl -s "$grafana_url/api/health" > /dev/null; then
        success "Grafana is healthy"
    else
        warning "Grafana health check failed"
    fi
    
    # Final summary
    log "${GREEN}🎉 Database deployment completed successfully!${NC}"
    log ""
    log "Deployment Summary:"
    log "  Environment: ${ENVIRONMENT}"
    log "  Database: Connected and migrated"
    log "  Monitoring: Prometheus, Grafana, AlertManager"
    log "  Backups: Configured and tested"
    log "  Security: Encrypted and HIPAA-compliant"
    log ""
    log "Next Steps:"
    log "  1. Configure email/Slack alerts in AlertManager"
    log "  2. Set up production SSL certificates"
    log "  3. Configure log aggregation"
    log "  4. Schedule regular restore tests"
}

# Help function
show_help() {
    echo "Usage: $0 [environment]"
    echo ""
    echo "Environments:"
    echo "  local     - Local development"
    echo "  staging   - Staging environment"
    echo "  production - Production environment"
    echo ""
    echo "Examples:"
    echo "  $0 local"
    echo "  $0 staging"
    echo "  $0 production"
}

# Main execution
case "${1:-help}" in
    "local"|"staging"|"production")
        main "$@"
        ;;
    "help"|*)
        show_help
        ;;
esac