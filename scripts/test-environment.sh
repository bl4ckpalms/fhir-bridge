#!/bin/bash
# =============================================================================
# FHIR Bridge Test Environment Management Script (Linux/macOS)
# Manages Docker Compose testing environment
# =============================================================================

set -e

echo ""
echo "========================================"
echo "FHIR Bridge Test Environment Manager"
echo "========================================"
echo ""

# Check if Docker is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed or not in PATH"
    echo "Please install Docker and try again"
    exit 1
fi

# Check if Docker Compose is available
if ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose is not available"
    echo "Please ensure Docker is running"
    exit 1
fi

show_help() {
    echo ""
    echo "Usage: $0 [COMMAND] [OPTIONS]"
    echo ""
    echo "Commands:"
    echo "  start           Start the test environment"
    echo "  stop            Stop the test environment"
    echo "  restart         Restart the test environment"
    echo "  status          Show status of all services"
    echo "  logs [service]  Show logs (optionally for specific service)"
    echo "  clean           Clean up all containers and volumes"
    echo "  test            Run integration tests"
    echo "  generate-data   Generate test data"
    echo "  simulate        Simulate HL7 message sending"
    echo "  help            Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 start"
    echo "  $0 logs fhir-bridge-test"
    echo "  $0 simulate --continuous"
    echo ""
    echo "Services:"
    echo "  • fhir-bridge-test    - Main application"
    echo "  • postgres-test       - PostgreSQL database"
    echo "  • redis-test          - Redis cache"
    echo ""
    echo "Optional services (use --profile flag):"
    echo "  • pgadmin-test        - PostgreSQL admin interface"
    echo "  • redis-commander-test - Redis admin interface"
    echo "  • integration-tests   - Test runner"
    echo "  • test-data-generator - Test data generator"
    echo "  • hl7-simulator       - HL7 message simulator"
    echo ""
}

start_environment() {
    echo "🚀 Starting FHIR Bridge test environment..."
    echo ""
    
    # Start core services (database and cache)
    echo "📊 Starting core services (PostgreSQL and Redis)..."
    docker compose -f docker-compose.test.yml up -d postgres-test redis-test
    
    # Wait for services to be healthy
    echo "⏳ Waiting for services to be ready..."
    sleep 10
    
    # Start the application
    echo "🏥 Starting FHIR Bridge application..."
    docker compose -f docker-compose.test.yml up -d fhir-bridge-test
    
    echo ""
    echo "✅ Test environment started successfully!"
    echo ""
    echo "🔗 Access points:"
    echo "   • FHIR Bridge API: http://localhost:8083"
    echo "   • Health Check: http://localhost:8084/actuator/health"
    echo "   • PostgreSQL: localhost:5433 (user: fhir_test_user, db: fhir_bridge_test)"
    echo "   • Redis: localhost:6380"
    echo ""
    echo "💡 Use '$0 status' to check service health"
    echo "💡 Use '$0 logs' to view application logs"
}

stop_environment() {
    echo "🛑 Stopping FHIR Bridge test environment..."
    docker compose -f docker-compose.test.yml down
    echo "✅ Test environment stopped"
}

restart_environment() {
    echo "🔄 Restarting FHIR Bridge test environment..."
    docker compose -f docker-compose.test.yml restart
    echo "✅ Test environment restarted"
}

show_logs() {
    if [ -z "$2" ]; then
        echo "📋 Showing logs for all services..."
        docker compose -f docker-compose.test.yml logs -f
    else
        echo "📋 Showing logs for $2..."
        docker compose -f docker-compose.test.yml logs -f "$2"
    fi
}

show_status() {
    echo "📊 Test environment status:"
    echo ""
    docker compose -f docker-compose.test.yml ps
    echo ""
    echo "🏥 Service health checks:"
    echo ""
    
    # Check PostgreSQL
    echo "Checking PostgreSQL..."
    if docker compose -f docker-compose.test.yml exec -T postgres-test pg_isready -U fhir_test_user -d fhir_bridge_test &> /dev/null; then
        echo "   ✅ PostgreSQL: Ready"
    else
        echo "   ❌ PostgreSQL: Not ready"
    fi
    
    # Check Redis
    echo "Checking Redis..."
    if docker compose -f docker-compose.test.yml exec -T redis-test redis-cli ping &> /dev/null; then
        echo "   ✅ Redis: Ready"
    else
        echo "   ❌ Redis: Not ready"
    fi
    
    # Check FHIR Bridge
    echo "Checking FHIR Bridge..."
    if curl -f http://localhost:8084/actuator/health &> /dev/null; then
        echo "   ✅ FHIR Bridge: Ready"
    else
        echo "   ❌ FHIR Bridge: Not ready"
    fi
}

clean_environment() {
    echo "🧹 Cleaning up test environment..."
    echo ""
    echo "⚠️  This will remove all containers, volumes, and test data!"
    read -p "Are you sure? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        return
    fi
    
    docker compose -f docker-compose.test.yml down -v --remove-orphans
    docker system prune -f
    echo "✅ Test environment cleaned up"
}

run_tests() {
    echo "🧪 Running integration tests..."
    docker compose -f docker-compose.test.yml --profile integration-tests up --build integration-tests
}

generate_data() {
    echo "📝 Generating test data..."
    docker compose -f docker-compose.test.yml --profile test-data up --build test-data-generator
}

simulate_messages() {
    echo "📤 Simulating HL7 messages..."
    if [ "$2" = "--continuous" ]; then
        echo "🔄 Starting continuous simulation mode..."
        docker compose -f docker-compose.test.yml --profile simulator run --rm hl7-simulator sh -c "apk add --no-cache curl jq && /scripts/simulate-hl7-messages.sh --continuous"
    else
        docker compose -f docker-compose.test.yml --profile simulator run --rm hl7-simulator sh -c "apk add --no-cache curl jq && /scripts/simulate-hl7-messages.sh"
    fi
}

# Main command handling
case "$1" in
    start)
        start_environment
        ;;
    stop)
        stop_environment
        ;;
    restart)
        restart_environment
        ;;
    logs)
        show_logs "$@"
        ;;
    status)
        show_status
        ;;
    clean)
        clean_environment
        ;;
    test)
        run_tests
        ;;
    generate-data)
        generate_data
        ;;
    simulate)
        simulate_messages "$@"
        ;;
    help|--help|-h)
        show_help
        ;;
    "")
        show_help
        ;;
    *)
        echo "❌ Unknown command: $1"
        show_help
        exit 1
        ;;
esac

echo ""