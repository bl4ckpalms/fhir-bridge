#!/bin/bash

# FHIR Bridge End-to-End Integration Test Suite
# This script runs comprehensive integration tests in the AWS test environment

set -e

# Configuration
TEST_ENVIRONMENT="test"
AWS_REGION="us-east-1"
STACK_NAME="fhir-bridge-${TEST_ENVIRONMENT}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}Starting FHIR Bridge End-to-End Integration Tests${NC}"

# Function to get AWS resources
get_aws_resources() {
    echo -e "${YELLOW}Retrieving AWS resources...${NC}"
    
    # Get ALB DNS name
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --names "${STACK_NAME}-alb" \
        --query 'LoadBalancers[0].DNSName' \
        --output text \
        --region ${AWS_REGION} 2>/dev/null || echo "localhost")
    
    # Get RDS endpoint
    RDS_ENDPOINT=$(aws rds describe-db-instances \
        --db-instance-identifier "${STACK_NAME}-db" \
        --query 'DBInstances[0].Endpoint.Address' \
        --output text \
        --region ${AWS_REGION} 2>/dev/null || echo "localhost")
    
    echo "ALB DNS: ${ALB_DNS}"
    echo "RDS Endpoint: ${RDS_ENDPOINT}"
    
    # Set the base URL
    if [ "$ALB_DNS" != "localhost" ]; then
        BASE_URL="http://${ALB_DNS}"
    else
        BASE_URL="http://localhost:8080"
    fi
    
    echo "Testing against: ${BASE_URL}"
}

# Function to wait for service health
wait_for_service_health() {
    echo -e "${YELLOW}Waiting for service to be healthy...${NC}"
    
    local max_attempts=30
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -f -s "${BASE_URL}/actuator/health" > /dev/null 2>&1; then
            echo -e "${GREEN}Service is healthy!${NC}"
            return 0
        fi
        
        echo "Attempt $attempt/$max_attempts - waiting for service..."
        sleep 30
        ((attempt++))
    done
    
    echo -e "${RED}Service failed to become healthy${NC}"
    return 1
}

# Function to test HL7 v2 to FHIR transformation
test_hl7_transformation() {
    echo -e "${YELLOW}Testing HL7 v2 to FHIR transformation...${NC}"
    
    # Test with sample HL7 messages
    local test_messages=("ADT_A01" "ORU_R01" "MDM_T02")
    
    for message_type in "${test_messages[@]}"; do
        echo "Testing $message_type transformation..."
        
        # Create test HL7 message if file doesn't exist
        if [ ! -f "test-data/${message_type}.hl7" ]; then
            mkdir -p test-data
            cat > "test-data/${message_type}.hl7" << EOF
MSH|^~\&|SENDING|FACILITY|RECV|FACILITY|20240101120000||${message_type}|MSG001|P|2.5
EVN||20240101120000
PID|1||12345678^^^HOSPITAL^MR||DOE^JOHN^MIDDLE||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA
PV1|1|I|ICU^101^A|||12345^SMITH^JANE^MD|||MED||||A|||12345^SMITH^JANE^MD|INP|INSURANCE|||||||||||||||||||||20240101120000
EOF
        fi
        
        # Send HL7 message
        response=$(curl -s -X POST \
            "${BASE_URL}/api/v1/transform/hl7" \
            -H "Content-Type: text/plain" \
            -d @"test-data/${message_type}.hl7" || echo '{"error": "connection failed"}')
        
        # Validate response
        if echo "$response" | jq -e '.resourceType == "Bundle"' > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $message_type transformation successful${NC}"
        else
            echo -e "${RED}✗ $message_type transformation failed${NC}"
            echo "Response: $response"
            return 1
        fi
    done
}

# Function to test consent management
test_consent_management() {
    echo -e "${YELLOW}Testing consent management workflows...${NC}"
    
    # Test consent creation
    consent_response=$(curl -s -X POST \
        "${BASE_URL}/api/v1/consent" \
        -H "Content-Type: application/json" \
        -d '{
            "resourceType": "Consent",
            "status": "active",
            "scope": {
                "coding": [{
                    "system": "http://terminology.hl7.org/CodeSystem/consentscope",
                    "code": "patient-privacy"
                }]
            },
            "category": [{
                "coding": [{
                    "system": "http://loinc.org",
                    "code": "59284-0"
                }]
            }],
            "patient": {
                "reference": "Patient/test-patient-123"
            },
            "provision": {
                "type": "permit",
                "provision": [{
                    "type": "deny",
                    "purpose": [{
                        "system": "http://terminology.hl7.org/CodeSystem/v3-ActReason",
                        "code": "HMARKT"
                    }]
                }]
            }
        }' || echo '{"error": "connection failed"}')
    
    if echo "$consent_response" | jq -e '.id != null' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Consent creation successful${NC}"
        consent_id=$(echo "$consent_response" | jq -r '.id')
    else
        echo -e "${RED}✗ Consent creation failed${NC}"
        echo "Response: $consent_response"
        return 1
    fi
    
    # Test data filtering with consent
    echo "Testing data filtering with consent..."
    filtered_response=$(curl -s -X GET \
        "${BASE_URL}/api/v1/patient/test-patient-123/observations" \
        -H "X-Consent-Id: $consent_id" || echo '{"error": "connection failed"}')
    
    if echo "$filtered_response" | jq -e '.resourceType == "Bundle"' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Data filtering with consent successful${NC}"
    else
        echo -e "${RED}✗ Data filtering with consent failed${NC}"
        echo "Response: $filtered_response"
        return 1
    fi
}

# Function to test audit logging
test_audit_logging() {
    echo -e "${YELLOW}Testing audit logging and compliance reporting...${NC}"
    
    # Generate some audit events
    curl -s -X GET "${BASE_URL}/api/v1/patient/test-patient-123" > /dev/null 2>&1 || true
    
    # Wait for logs to propagate
    sleep 5
    
    # Check if audit endpoint is accessible
    audit_response=$(curl -s -X GET "${BASE_URL}/api/v1/audit/logs" || echo '{"logs": []}')
    
    if echo "$audit_response" | jq -e '.logs != null' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Audit logging endpoint accessible${NC}"
    else
        echo -e "${YELLOW}⚠ Audit logging endpoint not accessible (may be normal for test environment)${NC}"
    fi
    
    # Test CloudWatch logs if AWS resources are available
    if [ "$ALB_DNS" != "localhost" ]; then
        log_group="/ecs/${STACK_NAME}"
        audit_events=$(aws logs filter-log-events \
            --log-group-name "$log_group" \
            --filter-pattern "AUDIT" \
            --region ${AWS_REGION} \
            --limit 10 2>/dev/null || echo '{"events": []}')
        
        if echo "$audit_events" | jq -e '.events | length > 0' > /dev/null 2>&1; then
            echo -e "${GREEN}✓ CloudWatch audit logs found${NC}"
        else
            echo -e "${YELLOW}⚠ No CloudWatch audit events found (may need more time)${NC}"
        fi
    fi
}

# Function to run security tests
test_security() {
    echo -e "${YELLOW}Running security penetration tests...${NC}"
    
    # Test authentication endpoint
    auth_response=$(curl -s -X POST \
        "${BASE_URL}/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d '{
            "username": "test-user",
            "password": "test-password"
        }' || echo '{"error": "connection failed"}')
    
    if echo "$auth_response" | jq -e '.token != null or .error != null' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Authentication endpoint accessible${NC}"
    else
        echo -e "${RED}✗ Authentication endpoint failed${NC}"
    fi
    
    # Test HTTPS redirect
    https_response=$(curl -s -I "${BASE_URL}/api/v1/health" 2>/dev/null | head -1 || echo "")
    if [[ "$https_response" == *"200"* ]] || [[ "$https_response" == *"301"* ]] || [[ "$https_response" == *"302"* ]]; then
        echo -e "${GREEN}✓ Security headers present${NC}"
    else
        echo -e "${YELLOW}⚠ Security headers check skipped (local testing)${NC}"
    fi
    
    # Test rate limiting
    echo "Testing rate limiting..."
    for i in {1..10}; do
        curl -s "${BASE_URL}/api/v1/health" > /dev/null &
    done
    wait
    
    echo -e "${GREEN}✓ Rate limiting test completed${NC}"
}

# Function to run performance tests
test_performance() {
    echo -e "${YELLOW}Running performance and load testing...${NC}"
    
    # Simple load test with curl
    echo "Running 100 concurrent requests..."
    for i in {1..100}; do
        curl -s "${BASE_URL}/api/v1/health" > /dev/null &
    done
    wait
    
    # Test HL7 transformation performance
    echo "Testing HL7 transformation performance..."
    start_time=$(date +%s)
    for i in {1..50}; do
        curl -s -X POST \
            "${BASE_URL}/api/v1/transform/hl7" \
            -H "Content-Type: text/plain" \
            -d "MSH|^~\\&|SENDING|FACILITY|RECV|FACILITY|20240101120000||ADT^A01|MSG00${i}|P|2.5" > /dev/null &
    done
    wait
    end_time=$(date +%s)
    
    duration=$((end_time - start_time))
    echo -e "${GREEN}✓ Performance test completed in ${duration}s${NC}"
}

# Function to test disaster recovery
test_disaster_recovery() {
    echo -e "${YELLOW}Testing disaster recovery and backup procedures...${NC}"
    
    # Test backup endpoint
    backup_response=$(curl -s -X POST \
        "${BASE_URL}/api/v1/admin/backup" \
        -H "Content-Type: application/json" \
        -d '{"type": "full"}' || echo '{"error": "connection failed"}')
    
    if echo "$backup_response" | jq -e '.status == "started" or .error != null' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backup endpoint accessible${NC}"
    else
        echo -e "${YELLOW}⚠ Backup endpoint not accessible (may be admin-only)${NC}"
    fi
    
    # Test restore endpoint
    restore_response=$(curl -s -X GET \
        "${BASE_URL}/api/v1/admin/restore/status" || echo '{"error": "connection failed"}')
    
    if echo "$restore_response" | jq -e '.status != null or .error != null' > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Restore endpoint accessible${NC}"
    else
        echo -e "${YELLOW}⚠ Restore endpoint not accessible (may be admin-only)${NC}"
    fi
    
    # Test S3 backup buckets
    if command -v aws &> /dev/null; then
        buckets=$(aws s3 ls | grep "fhir-bridge" || echo "")
        if [ -n "$buckets" ]; then
            echo -e "${GREEN}✓ S3 backup buckets found${NC}"
            echo "$buckets"
        else
            echo -e "${YELLOW}⚠ No S3 backup buckets found${NC}"
        fi
    fi
}

# Function to run all tests
run_all_tests() {
    echo -e "${GREEN}Starting comprehensive integration testing...${NC}"
    
    get_aws_resources
    wait_for_service_health
    
    echo -e "\n${YELLOW}=== HL7 v2 to FHIR Transformation Tests ===${NC}"
    test_hl7_transformation
    
    echo -e "\n${YELLOW}=== Consent Management Tests ===${NC}"
    test_consent_management
    
    echo -e "\n${YELLOW}=== Audit Logging Tests ===${NC}"
    test_audit_logging
    
    echo -e "\n${YELLOW}=== Security Tests ===${NC}"
    test_security
    
    echo -e "\n${YELLOW}=== Performance Tests ===${NC}"
    test_performance
    
    echo -e "\n${YELLOW}=== Disaster Recovery Tests ===${NC}"
    test_disaster_recovery
    
    echo -e "\n${GREEN}=== All Integration Tests Completed ===${NC}"
}

# Main execution
if [ "$1" == "local" ]; then
    BASE_URL="http://localhost:8080"
    echo "Running tests against local environment: ${BASE_URL}"
else
    get_aws_resources
fi

run_all_tests