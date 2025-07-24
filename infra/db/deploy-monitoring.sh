#!/bin/bash
# =============================================================================
# Database Monitoring Stack Deployment Script
# =============================================================================
# This script deploys the complete monitoring stack for PostgreSQL
# Usage: ./deploy-monitoring.sh [environment]
# =============================================================================

set -euo pipefail

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENVIRONMENT="${1:-local}"
COMPOSE_FILE="${SCRIPT_DIR}/monitoring/docker-compose.monitoring.yml"
ENV_FILE="${SCRIPT_DIR}/monitoring/.env.${ENVIRONMENT}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Logging function
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "${RED}ERROR: $1${NC}"
    exit 1
}

# Check dependencies
check_dependencies() {
    local deps=("docker" "docker-compose")
    for dep in "${deps[@]}"; do
        if ! command -v "$dep" &> /dev/null; then
            error_exit "Required dependency '$dep' not found"
        fi
    done
}

# Create environment file
create_env_file() {
    log "Creating environment file for ${ENVIRONMENT}..."
    
    cat > "$ENV_FILE" << EOF
# Environment configuration for ${ENVIRONMENT}
ENVIRONMENT=${ENVIRONMENT}

# Database configuration
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-fhir_bridge}
DB_USER=${DB_USER:-fhir_user}
DB_PASSWORD=${DB_PASSWORD:-fhir_password}

# PostgreSQL Exporter configuration
POSTGRES_EXPORTER_USER=${POSTGRES_EXPORTER_USER:-monitoring_user}
POSTGRES_EXPORTER_PASSWORD=${POSTGRES_EXPORTER_PASSWORD:-monitoring_password}

# Grafana configuration
GRAFANA_PASSWORD=${GRAFANA_PASSWORD:-admin123}

# AlertManager configuration
SMTP_HOST=${SMTP_HOST:-localhost:587}
SMTP_USERNAME=${SMTP_USERNAME:-}
SMTP_PASSWORD=${SMTP_PASSWORD:-}
ALERT_EMAIL_FROM=${ALERT_EMAIL_FROM:-alerts@fhirbridge.com}
ALERT_EMAIL_TO=${ALERT_EMAIL_TO:-devops@fhirbridge.com}
ALERT_EMAIL_CRITICAL=${ALERT_EMAIL_CRITICAL:-oncall@fhirbridge.com}
ALERT_EMAIL_WARNING=${ALERT_EMAIL_WARNING:-team@fhirbridge.com}
SLACK_WEBHOOK_URL=${SLACK_WEBHOOK_URL:-}
EOF

    log "Environment file created: $ENV_FILE"
}

# Deploy monitoring stack
deploy_monitoring() {
    log "Deploying monitoring stack for ${ENVIRONMENT}..."
    
    cd "${SCRIPT_DIR}/monitoring"
    
    # Create necessary directories
    mkdir -p grafana/dashboards grafana/datasources
    
    # Start the monitoring stack
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    
    log "Waiting for services to start..."
    sleep 30
    
    # Check service health
    check_service_health
    
    log "${GREEN}Monitoring stack deployed successfully!${NC}"
    log "Access URLs:"
    log "  - Prometheus: http://localhost:9090"
    log "  - Grafana: http://localhost:3000 (admin/admin123)"
    log "  - AlertManager: http://localhost:9093"
}

# Check service health
check_service_health() {
    log "Checking service health..."
    
    local services=("prometheus" "grafana" "alertmanager" "postgres-exporter")
    
    for service in "${services[@]}"; do
        if docker-compose -f "$COMPOSE_FILE" ps | grep -q "$service.*Up"; then
            log "${GREEN}✓ $service is running${NC}"
        else
            log "${RED}✗ $service is not running${NC}"
            docker-compose -f "$COMPOSE_FILE" logs "$service"
        fi
    done
}

# Stop monitoring stack
stop_monitoring() {
    log "Stopping monitoring stack..."
    cd "${SCRIPT_DIR}/monitoring"
    docker-compose -f "$COMPOSE_FILE" down
    log "${GREEN}Monitoring stack stopped${NC}"
}

# Show status
show_status() {
    log "Monitoring stack status:"
    cd "${SCRIPT_DIR}/monitoring"
    docker-compose -f "$COMPOSE_FILE" ps
}

# Main execution
main() {
    case "${2:-deploy}" in
        "deploy")
            check_dependencies
            create_env_file
            deploy_monitoring
            ;;
        "stop")
            stop_monitoring
            ;;
        "status")
            show_status
            ;;
        "restart")
            stop_monitoring
            deploy_monitoring
            ;;
        *)
            echo "Usage: $0 [environment] [deploy|stop|status|restart]"
            echo "  environment: local|staging|production (default: local)"
            echo "  action: deploy|stop|status|restart (default: deploy)"
            exit 1
            ;;
    esac
}

# Execute main function
main "$@"