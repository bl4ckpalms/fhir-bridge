#!/bin/bash
# =============================================================================
# Docker Setup Validation Script for FHIR Bridge
# =============================================================================

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}feat: Complete FHIR Bridge Implementation with Consent Management, Docker, and Infrastructure

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Validation functions
validate_docker() {
    print_status "Validating Docker installation..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        return 1
    fi
    
    if ! docker info &> /dev/null; then
        print_error "Docker daemon is not running"
        return 1
    fi
    
    print_success "Docker is installed and running"
}

validate_docker_compose() {
    print_status "Validating Docker Compose installation..."
    
    if ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not available"
        return 1
    fi
    
    print_success "Docker Compose is available"
}

validate_compose_files() {
    print_status "Validating Docker Compose configurations..."
    
    # Get the project root directory
    local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    
    # Validate main compose file
    if ! docker compose -f "${project_root}/docker-compose.yml" config &> /dev/null; then
        print_error "Main docker-compose.yml is invalid"
        return 1
    fi
    print_success "Main docker-compose.yml is valid"
    
    # Validate test compose file
    if ! docker compose -f "${project_root}/docker-compose.test.yml" config &> /dev/null; then
        print_error "docker-compose.test.yml is invalid"
        return 1
    fi
    print_success "docker-compose.test.yml is valid"
    
    # Validate local compose file
    if ! docker compose -f "${project_root}/docker-compose.local.yml" config &> /dev/null; then
        print_error "docker-compose.local.yml is invalid"
        return 1
    fi
    print_success "docker-compose.local.yml is valid"
}

validate_directories() {
    print_status "Validating required directories..."
    
    # Get the project root directory
    local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    
    local required_dirs=(
        "infra/db/init"
        "infra/redis"
        "infra/nginx"
        "infra/performance"
        "scripts"
        "logs"
    )
    
    for dir in "${required_dirs[@]}"; do
        if [ ! -d "${project_root}/${dir}" ]; then
            print_error "Required directory missing: $dir"
            return 1
        fi
    done
    
    print_success "All required directories exist"
}

validate_config_files() {
    print_status "Validating configuration files..."
    
    # Get the project root directory
    local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    
    local required_files=(
        "src/main/resources/application.yml"
        "src/main/resources/application-dev.yml"
        "src/main/resources/application-test.yml"
        "infra/db/init/01-init-database.sql"
        "infra/redis/redis-dev.conf"
        "infra/redis/redis-local.conf"
        "infra/db/pgadmin-servers.json"
        "docker-compose.yml"
        "docker-compose.test.yml"
        "docker-compose.local.yml"
        "Dockerfile"
        "Dockerfile.dev"
    )
    
    for file in "${required_files[@]}"; do
        if [ ! -f "${project_root}/${file}" ]; then
            print_error "Required file missing: $file"
            return 1
        fi
    done
    
    print_success "All required configuration files exist"
}

validate_ports() {
    print_status "Checking port availability..."
    
    local ports=(8080 8081 5432 6379 5050 8082 8025 80)
    local busy_ports=()
    
    for port in "${ports[@]}"; do
        if netstat -tuln 2>/dev/null | grep -q ":$port "; then
            busy_ports+=($port)
        fi
    done
    
    if [ ${#busy_ports[@]} -gt 0 ]; then
        print_warning "The following ports are already in use: ${busy_ports[*]}"
        print_warning "You may need to stop other services or modify port mappings"
    else
        print_success "All required ports are available"
    fi
}

validate_maven() {
    print_status "Validating Maven setup..."
    
    # Get the project root directory
    local project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    
    if [ -f "${project_root}/mvnw" ]; then
        if [ ! -x "${project_root}/mvnw" ]; then
            print_warning "Maven wrapper is not executable, attempting to fix..."
            chmod +x "${project_root}/mvnw" 2>/dev/null || print_warning "Could not make mvnw executable"
        fi
        print_success "Maven wrapper found"
    else
        print_error "Maven wrapper (mvnw) not found"
        return 1
    fi
    
    if [ ! -f "${project_root}/pom.xml" ]; then
        print_error "pom.xml not found"
        return 1
    fi
    
    print_success "Maven configuration is valid"
}

# Main validation
main() {
    print_status "Starting FHIR Bridge Docker setup validation..."
    echo
    
    local validation_failed=false
    
    validate_docker || validation_failed=true
    validate_docker_compose || validation_failed=true
    validate_compose_files || validation_failed=true
    validate_directories || validation_failed=true
    validate_config_files || validation_failed=true
    validate_ports || true  # Don't fail on port conflicts, just warn
    validate_maven || validation_failed=true
    
    echo
    if [ "$validation_failed" = true ]; then
        print_error "Validation failed! Please fix the issues above before proceeding."
        exit 1
    else
        print_success "All validations passed! Your Docker setup is ready."
        echo
        print_status "Next steps:"
        echo "  1. Run './scripts/dev-setup.sh setup' to start the development environment (or dev-setup.bat on Windows)"
        echo "  2. Run './scripts/dev-setup.sh test' to run tests (or dev-setup.bat test on Windows)"
        echo "  3. Access the application at http://localhost:8080"
    fi
}

# Run main function
main "$@"