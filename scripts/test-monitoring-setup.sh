#!/bin/bash

# FHIR Bridge Monitoring Validation Script
# This script tests the monitoring setup and validates alerts

set -e

echo "🧪 Testing FHIR Bridge Monitoring Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-test}
AWS_REGION=${2:-us-east-1}
ALERT_EMAIL=${3:-admin@example.com}

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Helper functions
log_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((TESTS_PASSED++))
}

log_failure() {
    echo -e "${RED}❌ $1${NC}"
    ((TESTS_FAILED++))
}

log_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# Validate AWS CLI and credentials
echo -e "\n${YELLOW}Validating AWS Setup...${NC}"
if command -v aws &> /dev/null; then
    log_success "AWS CLI is installed"
else
    log_failure "AWS CLI is not installed"
    exit 1
fi

if aws sts get-caller-identity &> /dev/null; then
    log_success "AWS credentials are configured"
else
    log_failure "AWS credentials are not configured"
    exit 1
fi

# Test CloudWatch Log Groups
echo -e "\n${YELLOW}Testing CloudWatch Log Groups...${NC}"
LOG_GROUPS=(
    "/ecs/fhir-bridge-app"
    "/security/fhir-bridge"
    "/audit/fhir-bridge"
    "/aws/vpc/flowlogs"
)

for log_group in "${LOG_GROUPS[@]}"; do
    if aws logs describe-log-groups --log-group-name-prefix "$log_group" --region $AWS_REGION | grep -q "$log_group"; then
        log_success "Log group exists: $log_group"
    else
        log_failure "Log group missing: $log_group"
    fi
done

# Test CloudWatch Dashboards
echo -e "\n${YELLOW}Testing CloudWatch Dashboards...${NC}"
DASHBOARDS=(
    "fhir-bridge-main-dashboard"
    "fhir-bridge-security-dashboard"
)

for dashboard in "${DASHBOARDS[@]}"; do
    if aws cloudwatch describe-dashboards --dashboard-names "$dashboard" --region $AWS_REGION | grep -q "$dashboard"; then
        log_success "Dashboard exists: $dashboard"
    else
        log_failure "Dashboard missing: $dashboard"
    fi
done

# Test CloudWatch Alarms
echo -e "\n${YELLOW}Testing CloudWatch Alarms...${NC}"
ALARMS=(
    "fhir-bridge-high-cpu"
    "fhir-bridge-high-memory"
    "fhir-bridge-high-error-rate"
    "fhir-bridge-rds-high-cpu"
    "fhir-bridge-health-check-failures"
)

for alarm in "${ALARMS[@]}"; do
    if aws cloudwatch describe-alarms --alarm-names "$alarm" --region $AWS_REGION | grep -q "$alarm"; then
        log_success "Alarm exists: $alarm"
    else
        log_failure "Alarm missing: $alarm"
    fi
done

# Test SNS Topics
echo -e "\n${YELLOW}Testing SNS Topics...${NC}"
TOPICS=(
    "fhir-bridge-alerts"
    "fhir-bridge-scaling-notifications"
)

for topic in "${TOPICS[@]}"; do
    if aws sns list-topics --region $AWS_REGION | grep -q "$topic"; then
        log_success "SNS topic exists: $topic"
    else
        log_failure "SNS topic missing: $topic"
    fi
done

# Test Auto-scaling Configuration
echo -e "\n${YELLOW}Testing Auto-scaling Configuration...${NC}"
if aws application-autoscaling describe-scaling-policies --service-namespace ecs --region $AWS_REGION | grep -q "fhir-bridge"; then
    log_success "ECS auto-scaling policies configured"
else
    log_failure "ECS auto-scaling policies missing"
fi

# Test Health Check Endpoints
echo -e "\n${YELLOW}Testing Health Check Endpoints...${NC}"
LOAD_BALANCER_DNS=$(aws elbv2 describe-load-balancers --region $AWS_REGION --query 'LoadBalancers[0].DNSName' --output text 2>/dev/null || echo "")

if [ -n "$LOAD_BALANCER_DNS" ] && [ "$LOAD_BALANCER_DNS" != "None" ]; then
    log_info "Testing health endpoints on: $LOAD_BALANCER_DNS"
    
    # Test basic health check
    if curl -s -f "http://$LOAD_BALANCER_DNS/health" > /dev/null; then
        log_success "Basic health check endpoint responding"
    else
        log_failure "Basic health check endpoint not responding"
    fi
    
    # Test readiness check
    if curl -s -f "http://$LOAD_BALANCER_DNS/health/ready" > /dev/null; then
        log_success "Readiness check endpoint responding"
    else
        log_failure "Readiness check endpoint not responding"
    fi
    
    # Test liveness check
    if curl -s -f "http://$LOAD_BALANCER_DNS/health/live" > /dev/null; then
        log_success "Liveness check endpoint responding"
    else
        log_failure "Liveness check endpoint not responding"
    fi
    
    # Test detailed health check
    if curl -s -f "http://$LOAD_BALANCER_DNS/health/detailed" > /dev/null; then
        log_success "Detailed health check endpoint responding"
    else
        log_failure "Detailed health check endpoint not responding"
    fi
else
    log_warning "Load balancer DNS not available, skipping endpoint tests"
fi

# Test Log Insights Queries
echo -e "\n${YELLOW}Testing CloudWatch Logs Insights Queries...${NC}"
QUERIES=(
    "FHIRBridge-Application-Errors"
    "FHIRBridge-Security-Events"
    "FHIRBridge-Performance-Analysis"
    "FHIRBridge-Audit-Trail"
)

for query in "${QUERIES[@]}"; do
    if aws logs describe-query-definitions --region $AWS_REGION | grep -q "$query"; then
        log_success "Query definition exists: $query"
    else
        log_failure "Query definition missing: $query"
    fi
done

# Test S3 Buckets
echo -e "\n${YELLOW}Testing S3 Buckets...${NC}"
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
BUCKET_NAME="fhir-bridge-log-analysis-$ENVIRONMENT-$ACCOUNT_ID"

if aws s3 ls "s3://$BUCKET_NAME" &> /dev/null; then
    log_success "S3 bucket exists: $BUCKET_NAME"
else
    log_failure "S3 bucket missing: $BUCKET_NAME"
fi

# Test IAM Roles and Policies
echo -e "\n${YELLOW}Testing IAM Roles and Policies...${NC}"
ROLES=(
    "fhir-bridge-log-processor-role"
)

for role in "${