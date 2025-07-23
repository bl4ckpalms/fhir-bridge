#!/bin/bash
# =============================================================================
# Development Environment Setup Script for FHIR Bridge
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

# Check if Docker and Docker Compose are installed
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    print_success "Prerequisites check passed"
}

# Create necessary directories
create_directories() {
    print_status "Creating necessary directories..."
    
    mkdir -p logs
    mkdir -p target/test-results
    mkdir -p target/coverage-reports
    mkdir -p target/integration-test-results
    mkdir -p target/performance-results
    
    print_success "Directories created"
}

# Build the application
build_application() {
    print_status "Building the application..."
    
    if [ -f "./mvnw" ]; then
        chmod +x ./mvnw
        ./mvnw clean compile -DskipTests
        print_success "Application built successfully"
    else
        print_warning "Maven wrapper not found, skipping build"
    fi
}

# Start the development environment
start_dev_environment() {
    print_status "Starting development environment..."
    
    # Stop any existing containers
    docker-compose down --remove-orphans
    
    # Build and start services
    docker-compose up --build -d
    
    print_status "Waiting for services to be ready..."
    sleep 30
    
    # Check if services are healthy
    if docker-compose ps | grep -q "Up (healthy)"; then
        print_success "Development environment started successfully"
        print_status "Services available at:"
        echo "  - FHIR Bridge API: http://localhost:8080"
        echo "  - Management/Actuator: http://localhost:8081"
        echo "  - pgAdmin: http://localhost:5050 (admin@fhirbridge.local / admin123)"
        echo "  - Redis Commander: http://localhost:8082 (admin / admin123)"
        echo "  - Mailhog: http://localhost:8025"
        echo "  - Nginx Proxy: http://localhost:80"
    else
        print_error "Some services failed to start properly"
        docker-compose logs
        exit 1
    fi
}

# Run tests
run_tests() {
    print_status "Running tests..."
    
    docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit
    
    if [ $? -eq 0 ]; then
        print_success "Tests completed successfully"
    else
        print_error "Tests failed"
        exit 1
    fi
}

# Show logs
show_logs() {
    print_status "Showing application logs..."
    docker-compose logs -f fhir-bridge
}

# Stop the development environment
stop_dev_environment() {
    print_status "Stopping development environment..."
    docker-compose down --remove-orphans
    print_success "Development environment stopped"
}

# Clean up everything
cleanup() {
    print_status "Cleaning up development environment..."
    docker-compose down --remove-orphans --volumes
    docker system prune -f
    print_success "Cleanup completed"
}

# Show help
show_help() {
    echo "FHIR Bridge Development Environment Setup"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  setup     - Set up and start the development environment"
    echo "  start     - Start the development environment"
    echo "  stop      - Stop the development environment"
    echo "  restart   - Restart the development environment"
    echo "  test      - Run tests"
    echo "  logs      - Show application logs"
    echo "  cleanup   - Clean up all containers and volumes"
    echo "  help      - Show this help message"
    echo ""
}

# Main script logic
case "${1:-setup}" in
    setup)
        check_prerequisites
        create_directories
        build_application
        start_dev_environment
        ;;
    start)
        start_dev_environment
        ;;
    stop)
        stop_dev_environment
        ;;
    restart)
        stop_dev_environment
        start_dev_environment
        ;;
    test)
        run_tests
        ;;
    logs)
        show_logs
        ;;
    cleanup)
        cleanup
        ;;
    help)
        show_help
        ;;
    *)
        print_error "Unknown command: $1"
        show_help
        exit 1
        ;;
esac