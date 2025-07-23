#!/bin/sh
# =============================================================================
# Performance Testing Script for FHIR Bridge
# =============================================================================

# Configuration
BASE_URL="http://fhir-bridge:8080"
RESULTS_DIR="/results"
CONCURRENT_USERS=10
TOTAL_REQUESTS=1000
TEST_DURATION=60

# Create results directory
mkdir -p "$RESULTS_DIR"

# Function to wait for service to be ready
wait_for_service() {
    echo "Waiting for FHIR Bridge to be ready..."
    for i in $(seq 1 30); do
        if curl -f "$BASE_URL/actuator/health" >/dev/null 2>&1; then
            echo "Service is ready!"
            return 0
        fi
        echo "Attempt $i/30: Service not ready, waiting..."
        sleep 2
    done
    echo "Service failed to become ready"
    return 1
}

# Wait for the service
if ! wait_for_service; then
    exit 1
fi

echo "Starting performance tests..."

# Test 1: Health Check Endpoint
echo "Test 1: Health Check Performance"
ab -n 100 -c 5 -g "$RESULTS_DIR/health_check.tsv" "$BASE_URL/actuator/health" > "$RESULTS_DIR/health_check.txt"

# Test 2: API Endpoint Load Test (if available)
echo "Test 2: API Endpoint Load Test"
ab -n $TOTAL_REQUESTS -c $CONCURRENT_USERS -g "$RESULTS_DIR/api_load.tsv" "$BASE_URL/api/v1/health" > "$RESULTS_DIR/api_load.txt" 2>/dev/null || echo "API endpoint not available for testing"

# Test 3: Sustained Load Test
echo "Test 3: Sustained Load Test"
ab -t $TEST_DURATION -c $CONCURRENT_USERS -g "$RESULTS_DIR/sustained_load.tsv" "$BASE_URL/actuator/health" > "$RESULTS_DIR/sustained_load.txt"

# Generate summary report
cat > "$RESULTS_DIR/performance_summary.txt" << EOF
FHIR Bridge Performance Test Summary
====================================
Date: $(date)
Base URL: $BASE_URL
Concurrent Users: $CONCURRENT_USERS
Total Requests: $TOTAL_REQUESTS
Test Duration: ${TEST_DURATION}s

Test Results:
EOF

# Extract key metrics from each test
for test_file in "$RESULTS_DIR"/*.txt; do
    if [ -f "$test_file" ]; then
        test_name=$(basename "$test_file" .txt)
        echo "" >> "$RESULTS_DIR/performance_summary.txt"
        echo "$test_name:" >> "$RESULTS_DIR/performance_summary.txt"
        grep -E "(Requests per second|Time per request|Transfer rate)" "$test_file" >> "$RESULTS_DIR/performance_summary.txt" 2>/dev/null || echo "  No metrics available" >> "$RESULTS_DIR/performance_summary.txt"
    fi
done

echo "Performance tests completed. Results saved to $RESULTS_DIR"
echo "Summary:"
cat "$RESULTS_DIR/performance_summary.txt"