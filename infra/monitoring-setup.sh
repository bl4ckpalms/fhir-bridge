#!/bin/bash

# FHIR Bridge Monitoring Setup Script
# This script sets up comprehensive monitoring and alerting for the FHIR Bridge application

set -e

echo "🚀 Starting FHIR Bridge Monitoring Setup..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
ENVIRONMENT=${1:-development}
AWS_REGION=${2:-us-east-1}
ALERT_EMAIL=${3:-admin@example.com}
SCALING_EMAIL=${4:-devops@example.com}

echo -e "${GREEN}Environment: $ENVIRONMENT${NC}"
echo -e "${GREEN}AWS Region: $AWS_REGION${NC}"
echo -e "${GREEN}Alert Email: $ALERT_EMAIL${NC}"
echo -e "${GREEN}Scaling Email: $SCALING_EMAIL${NC}"

# Validate AWS CLI is installed
if ! command -v aws &> /dev/null; then
    echo -e "${RED}AWS CLI is not installed. Please install it first.${NC}"
    exit 1
fi

# Validate AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}AWS credentials not configured. Please run 'aws configure' first.${NC}"
    exit 1
fi

# Create Lambda deployment package for log processor
echo -e "${YELLOW}Creating Lambda deployment package...${NC}"
cd lambda
zip -r ../log_processor.zip log_processor.py
cd ..

# Update terraform variables
echo -e "${YELLOW}Updating terraform variables...${NC}"
cat > terraform.tfvars << EOF
environment = "$ENVIRONMENT"
aws_region = "$AWS_REGION"
alert_email = "$ALERT_EMAIL"
scaling_notification_email = "$SCALING_EMAIL"
enable_monitoring = true
enable_autoscaling = true
log_retention_days = 90
security_log_retention_days = 365
audit_log_retention_days = 2557
EOF

# Initialize and apply terraform
echo -e "${YELLOW}Applying terraform configuration...${NC}"
terraform init
terraform plan -out=tfplan
terraform apply tfplan

# Create CloudWatch dashboards
echo -e "${YELLOW}Creating CloudWatch dashboards...${NC}"
aws cloudwatch put-dashboard \
    --dashboard-name fhir-bridge-main-dashboard \
    --dashboard-body file://dashboards/main-dashboard.json \
    --region $AWS_REGION

aws cloudwatch put-dashboard \
    --dashboard-name fhir-bridge-security-dashboard \
    --dashboard-body file://dashboards/security-dashboard.json \
    --region $AWS_REGION

# Create log insights queries
echo -e "${YELLOW}Setting up CloudWatch Logs Insights queries...${NC}"
aws logs put-query-definition \
    --name "FHIRBridge-Application-Errors" \
    --log-group-names "/ecs/fhir-bridge-app" \
    --query-string 'fields @timestamp, level, message, requestId, userId | filter level = "ERROR" | sort @timestamp desc | limit 100' \
    --region $AWS_REGION

aws logs put-query-definition \
    --name "FHIRBridge-Security-Events" \
    --log-group-names "/security/fhir-bridge" \
    --query-string 'fields @timestamp, level, message, userId, action, ipAddress | filter level in ["WARN", "ERROR"] or message like /authentication failed/ or message like /authorization failed/ | sort @timestamp desc | limit 100' \
    --region $AWS_REGION

# Test health check endpoints
echo -e "${YELLOW}Testing health check endpoints...${NC}"
HEALTH_CHECK_URL=$(terraform output -raw load_balancer_dns_name)
if [ -n "$HEALTH_CHECK_URL" ]; then
    echo "Testing health endpoints..."
    curl -f "http://$HEALTH_CHECK_URL/health" || echo "Health check failed"
    curl -f "http://$HEALTH_CHECK_URL/health/ready" || echo "Readiness check failed"
    curl -f "http://$HEALTH_CHECK_URL/health/live" || echo "Liveness check failed"
    curl -f "http://$HEALTH_CHECK_URL/health/detailed" || echo "Detailed health check failed"
else
    echo -e "${YELLOW}Load balancer DNS not available yet. Skipping health check tests.${NC}"
fi

# Create monitoring documentation
echo -e "${YELLOW}Creating monitoring documentation...${NC}"
cat > MONITORING_SETUP.md << EOF
# FHIR Bridge Monitoring Setup

## Overview
Comprehensive monitoring and alerting has been configured for the FHIR Bridge application.

## Dashboards
- **Main Dashboard**: Application metrics, CPU, memory, request counts
- **Security Dashboard**: Security events, authentication failures, WAF metrics

## Alerts
- High CPU utilization (>80%)
- High memory utilization (>85%)
- High error rate (>10 5xx errors)
- Database connection issues
- Health check failures
- Security events (failed authentication, suspicious activities)

## Health Check Endpoints
- \`/health\` - Basic health status
- \`/health/ready\` - Readiness probe (checks DB connectivity)
- \`/health/live\` - Liveness probe
- \`/health/detailed\` - Detailed system health

## Auto-scaling
- CPU-based scaling (70% threshold)
- Memory-based scaling (80% threshold)
- Request count-based scaling (1000 req/min)
- Scheduled scaling for business hours

## Log Analysis
- Real-time log processing via Lambda
- Security event detection
- Performance metrics extraction
- Compliance audit trails

## Access
- CloudWatch Console: https://console.aws.amazon.com/cloudwatch/
- Dashboard URLs: Available in AWS Console
- Alert emails: $ALERT_EMAIL
EOF

echo -e "${GREEN}✅ Monitoring setup completed successfully!${NC}"
echo -e "${GREEN}📊 Dashboards: Check AWS CloudWatch Console${NC}"
echo -e "${GREEN}📧 Alerts: Check $ALERT_EMAIL for subscription confirmation${NC}"
echo -e "${GREEN}📖 Documentation: See MONITORING_SETUP.md${NC}"