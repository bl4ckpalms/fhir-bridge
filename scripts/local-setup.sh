#!/bin/bash

# =============================================================================
# Local Development Setup Script for FHIR Bridge
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
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check if port is available
port_available() {
    ! netstat -an 2>/dev/null | grep -q ":$1 "
}

# Function to wait for service to be ready
wait_for_service() {
    local service=$1
    local port=$2
    local max_attempts=30
    local attempt=1

    print_status "Waiting for $service to be ready on port $port..."
    
    while [ $attempt -le $max_attempts ]; do
        if nc -z localhost $port 2>/dev/null; then
            print_success "$service is ready!"
            return 0
        fi
        
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    print_error "$service failed to start within $((max_attempts * 2)) seconds"
    return 1
}

# Main setup function
main() {
    echo "=============================================="
    echo "FHIR Bridge Local Development Setup"
    echo "=============================================="
    
    # Check prerequisites
    print_status "Checking prerequisites..."
    
    if ! command_exists docker; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command_exists docker-compose; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    if ! command_exists java; then
        print_error "Java is not installed. Please install Java 17 or later."
        exit 1
    fi
    
    # Check Java version
    java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$java_version" -lt 17 ]; then
        print_error "Java 17 or later is required. Current version: $java_version"
        exit 1
    fi
    
    print_success "All prerequisites are installed"
    
    # Check port availability
    print_status "Checking port availability..."
    
    ports_to_check=(5432 6379 8080 8081)
    for port in "${ports_to_check[@]}"; do
        if ! port_available $port; then
            print_warning "Port $port is already in use. You may need to stop conflicting services."
        fi
    done
    
    # Start Docker services
    print_status "Starting Docker services..."
    
    # Stop any existing services first
    docker-compose -f docker-compose.local.yml down 2>/dev/null || true
    
    # Start dependencies
    docker-compose -f docker-compose.local.yml up -d postgres redis
    
    # Wait for services to be ready
    wait_for_service "PostgreSQL" 5432
    wait_for_service "Redis" 6379
    
    # Test database connection
    print_status "Testing database connection..."
    if docker-compose -f docker-compose.local.yml exec -T postgres pg_isready -U fhir_user -d fhir_bridge >/dev/null 2>&1; then
        print_success "Database connection successful"
    else
        print_error "Database connection failed"
        exit 1
    fi
    
    # Test Redis connection
    print_status "Testing Redis connection..."
    if docker-compose -f docker-compose.local.yml exec -T redis redis-cli ping >/dev/null 2>&1; then
        print_success "Redis connection successful"
    else
        print_error "Redis connection failed"
        exit 1
    fi
    
    # Build application (optional)
    if [ "$1" = "--build" ]; then
        print_status "Building application..."
        ./mvnw clean compile -DskipTests
        print_success "Application built successfully"
    fi
    
    # Run tests (optional)
    if [ "$1" = "--test" ]; then
        print_status "Running tests..."
        ./mvnw test -Dspring.profiles.active=test
        print_success "Tests completed successfully"
    fi
    
    echo ""
    echo "=============================================="
    print_success "Local development environment is ready!"
    echo "=============================================="
    echo ""
    echo "Services running:"
    echo "  - PostgreSQL: localhost:5432"
    echo "  - Redis: localhost:6379"
    echo ""
    echo "To start the application:"
    echo "  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev"
    echo ""
    echo "To start with admin tools:"
    echo "  docker-compose -f docker-compose.local.yml --profile admin up -d"
    echo "  - pgAdmin: http://localhost:5050"
    echo "  - Redis Commander: http://localhost:8082"
    echo ""
    echo "To stop services:"
    echo "  docker-compose -f docker-compose.local.yml down"
    echo ""
    echo "For more information, see DOCKER-LOCAL.md"
}

# Handle script arguments
case "$1" in
    --help|-h)
        echo "Usage: $0 [--build|--test|--help]"
        echo ""
        echo "Options:"
        echo "  --build    Build the application after starting services"
        echo "  --test     Run tests after starting services"
        echo "  --help     Show this help message"
        exit 0
        ;;
    --build|--test)
        main "$1"
        ;;
    "")
        main
        ;;
    *)
        print_error "Unknown option: $1"
        echo "Use --help for usage information"
        exit 1
        ;;
esac