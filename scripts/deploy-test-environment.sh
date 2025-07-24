#!/bin/bash
#!/bin/bash
# FHIR Bridge AWS Test Environment Deployment Script
# Updated to work with existing AWS infrastructure due to IAM limitations

set -e

# Configuration
AWS_REGION="us-east-1"
PROJECT_NAME="fhir-bridge"
ENVIRONMENT="test"
EXISTING_VPC_ID=""  # Will be discovered
EXISTING_SUBNET_IDS=()  # Will be discovered
EXISTING_SECURITY_GROUP=""  # Will be discovered

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}FHIR Bridge AWS Test Environment Deployment${NC}"
echo "=========================================="

# Function to check AWS CLI availability
check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}AWS CLI is not installed. Please install it first.${NC}"
        exit 1
    fi
    
    # Check AWS credentials
    if ! aws sts get-caller-identity &> /dev/null; then
        echo -e "${RED}AWS credentials not configured. Run 'aws configure' first.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ AWS CLI and credentials verified${NC}"
}

# Function to discover existing infrastructure
discover_infrastructure() {
    echo -e "\n${YELLOW}Discovering existing AWS infrastructure...${NC}"
    
    # Find existing VPC
    EXISTING_VPC_ID=$(aws ec2 describe-vpcs \
        --filters "Name=tag:Name,Values=*fhir*" "Name=tag:Environment,Values=*test*" \
        --query "Vpcs[0].VpcId" \
        --output text \
        --region $AWS_REGION)
    
    if [ "$EXISTING_VPC_ID" == "None" ]; then
        EXISTING_VPC_ID=$(aws ec2 describe-vpcs \
            --filters "Name=is-default,Values=true" \
            --query "Vpcs[0].VpcId" \
            --output text \
            --region $AWS_REGION)
    fi
    
    echo -e "${GREEN}✓ Using VPC: $EXISTING_VPC_ID${NC}"
    
    # Find existing subnets
    mapfile -t EXISTING_SUBNET_IDS < <(aws ec2 describe-subnets \
        --filters "Name=vpc-id,Values=$EXISTING_VPC_ID" \
        --query "Subnets[?State=='available'].SubnetId" \
        --output text \
        --region $AWS_REGION)
    
    echo -e "${GREEN}✓ Found ${#EXISTING_SUBNET_IDS[@]} subnets${NC}"
    
    # Find existing security group
    EXISTING_SECURITY_GROUP=$(aws ec2 describe-security-groups \
        --filters "Name=vpc-id,Values=$EXISTING_VPC_ID" "Name=group-name,Values=*fhir*" \
        --query "SecurityGroups[0].GroupId" \
        --output text \
        --region $AWS_REGION)
    
    if [ "$EXISTING_SECURITY_GROUP" == "None" ]; then
        EXISTING_SECURITY_GROUP=$(aws ec2 describe-security-groups \
            --filters "Name=vpc-id,Values=$EXISTING_VPC_ID" "Name=group-name,Values=default" \
            --query "SecurityGroups[0].GroupId" \
            --output text \
            --region $AWS_REGION)
    fi
    
    echo -e "${GREEN}✓ Using Security Group: $EXISTING_SECURITY_GROUP${NC}"
}

# Function to check existing resources
check_existing_resources() {
    echo -e "\n${YELLOW}Checking existing resources...${NC}"
    
    # Check ECR repository
    if aws ecr describe-repositories --repository-names $PROJECT_NAME --region $AWS_REGION &>/dev/null; then
        echo -e "${GREEN}✓ ECR repository exists: $PROJECT_NAME${NC}"
    else
        echo -e "${RED}✗ ECR repository missing: $PROJECT_NAME${NC}"
    fi
    
    # Check RDS instance
    if aws rds describe-db-instances --db-instance-identifier $PROJECT_NAME-db --region $AWS_REGION &>/dev/null; then
        echo -e "${GREEN}✓ RDS instance exists: $PROJECT_NAME-db${NC}"
    else
        echo -e "${RED}✗ RDS instance missing: $PROJECT_NAME-db${NC}"
    fi
    
    # Check ECS cluster
    if aws ecs describe-clusters --clusters $PROJECT_NAME-cluster --region $AWS_REGION | jq -r '.clusters[0].status' | grep -q "ACTIVE"; then
        echo -e "${GREEN}✓ ECS cluster exists: $PROJECT_NAME-cluster${NC}"
    else
        echo -e "${RED}✗ ECS cluster missing: $PROJECT_NAME-cluster${NC}"
    fi
    
    # Check ALB
    ALB_ARN=$(aws elbv2 describe-load-balancers --names tefca-gateway-alb --region $AWS_REGION --query "LoadBalancers[0].LoadBalancerArn" --output text)
    if [ "$ALB_ARN" != "None" ]; then
        echo -e "${GREEN}✓ ALB exists: tefca-gateway-alb${NC}"
    else
        echo -e "${RED}✗ ALB missing: tefca-gateway-alb${NC}"
    fi
    
    # Check Redis
    if aws elasticache describe-cache-clusters --cache-cluster-id $PROJECT_NAME-redis --region $AWS_REGION &>/dev/null; then
        echo -e "${GREEN}✓ Redis cluster exists: $PROJECT_NAME-redis${NC}"
    else
        echo -e "${RED}✗ Redis cluster missing: $PROJECT_NAME-redis${NC}"
    fi
}

# Function to update ECS service
update_ecs_service() {
    echo -e "\n${YELLOW}Updating ECS service with latest image...${NC}"
    
    # Get ECR login token
    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com
    
    # Build and push Docker image
    cd ..
    docker build -t $PROJECT_NAME:latest .
    docker tag $PROJECT_NAME:latest $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$PROJECT_NAME:latest
    docker push $AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/$PROJECT_NAME:latest
    
    # Update ECS service
    aws ecs update-service \
        --cluster $PROJECT_NAME-cluster \
        --service $PROJECT_NAME-service \
        --force-new-deployment \
        --region $AWS_REGION
    
    echo -e "${GREEN}✓ ECS service updated${NC}"
}

# Function to wait for service stability
wait_for_service() {
    echo -e "\n${YELLOW}Waiting for service to stabilize...${NC}"
    
    aws ecs wait services-stable \
        --cluster $PROJECT_NAME-cluster \
        --services $PROJECT_NAME-service \
        --region $AWS_REGION
    
    echo -e "${GREEN}✓ Service is stable${NC}"
}

# Function to get service endpoint
get_service_endpoint() {
    echo -e "\n${YELLOW}Getting service endpoint...${NC}"
    
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --names tefca-gateway-alb \
        --query "LoadBalancers[0].DNSName" \
        --output text \
        --region $AWS_REGION)
    
    if [ "$ALB_DNS" != "None" ]; then
        echo -e "${GREEN}✓ Service endpoint: http://$ALB_DNS${NC}"
        echo -e "${GREEN}✓ Health check: http://$ALB_DNS/health${NC}"
        echo -e "${GREEN}✓ FHIR endpoint: http://$ALB_DNS/fhir/R4${NC}"
    else
        echo -e "${RED}✗ Could not determine service endpoint${NC}"
    fi
}

# Function to run health checks
run_health_checks() {
    echo -e "\n${YELLOW}Running health checks...${NC}"
    
    ALB_DNS=$(aws elbv2 describe-load-balancers \
        --names tefca-gateway-alb \
        --query "LoadBalancers[0].DNSName" \
        --output text \
        --region $AWS_REGION)
    
    if [ "$ALB_DNS" != "None" ]; then
        # Wait a bit for service to be ready
        sleep 30
        
        # Test health endpoint
        if curl -f -s "http://$ALB_DNS/health" > /dev/null; then
            echo -e "${GREEN}✓ Health check passed${NC}"
        else
            echo -e "${RED}✗ Health check failed${NC}"
        fi
        
        # Test FHIR endpoint
        if curl -f -s "http://$ALB_DNS/fhir/R4/metadata" > /dev/null; then
            echo -e "${GREEN}✓ FHIR endpoint accessible${NC}"
        else
            echo -e "${RED}✗ FHIR endpoint not accessible${NC}"
        fi
    fi
}

# Main execution
main() {
    check_aws_cli
    
    # Get AWS account ID
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    
    discover_infrastructure
    check_existing_resources
    
    echo -e "\n${YELLOW}Note: Due to IAM policy limitations, using existing AWS infrastructure${NC}"
    echo -e "${YELLOW}To deploy new infrastructure, please:${NC}"
    echo -e "${YELLOW}1. Create a new IAM user with AdministratorAccess${NC}"
    echo -e "${YELLOW}2. Or increase the policy limit for the current user${NC}"
    echo -e "${YELLOW}3. Or use AWS IAM Identity Center for better permission management${NC}"
    
    read -p "Continue with existing infrastructure? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        update_ecs_service
        wait_for_service
        get_service_endpoint
        run_health_checks
        
        echo -e "\n${GREEN}Deployment completed successfully!${NC}"
        echo -e "${GREEN}The application is now running on existing AWS infrastructure${NC}"
    else
        echo -e "${YELLOW}Deployment cancelled${NC}"
        exit 0
    fi
}

# Execute main function
main