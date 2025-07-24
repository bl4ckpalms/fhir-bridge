#!/bin/bash
# AWS IAM Setup Script for FHIR Bridge Testing
# Resolves IAM policy limitations for comprehensive testing

set -e

# Configuration
AWS_REGION="us-east-1"
PROJECT_NAME="fhir-bridge"
IAM_GROUP_NAME="fhir-bridge-testers"
IAM_USER_NAME="fhir-bridge-test-user"
ROLE_NAME="fhir-bridge-test-role"
POLICY_NAME="fhir-bridge-test-policy"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}AWS IAM Setup for FHIR Bridge Testing${NC}"
echo "======================================"

# Function to check AWS CLI
check_aws_cli() {
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}AWS CLI is not installed. Please install it first.${NC}"
        exit 1
    fi
    
    if ! aws sts get-caller-identity &> /dev/null; then
        echo -e "${RED}AWS credentials not configured. Run 'aws configure' first.${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ AWS CLI and credentials verified${NC}"
}

# Function to get AWS account ID
get_account_id() {
    AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
    echo -e "${GREEN}✓ AWS Account ID: $AWS_ACCOUNT_ID${NC}"
}

# Function to create IAM policy
create_test_policy() {
    echo -e "\n${YELLOW}Creating comprehensive test policy...${NC}"
    
    cat > /tmp/fhir-bridge-test-policy.json << 'EOF'
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "EC2FullAccess",
            "Effect": "Allow",
            "Action": [
                "ec2:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "RDSFullAccess",
            "Effect": "Allow",
            "Action": [
                "rds:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "ECSFullAccess",
            "Effect": "Allow",
            "Action": [
                "ecs:*",
                "ecr:*",
                "servicediscovery:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "ELBFullAccess",
            "Effect": "Allow",
            "Action": [
                "elasticloadbalancing:*",
                "autoscaling:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "ElastiCacheFullAccess",
            "Effect": "Allow",
            "Action": [
                "elasticache:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "S3FullAccess",
            "Effect": "Allow",
            "Action": [
                "s3:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "IAMFullAccess",
            "Effect": "Allow",
            "Action": [
                "iam:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "KMSFullAccess",
            "Effect": "Allow",
            "Action": [
                "kms:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "CloudWatchFullAccess",
            "Effect": "Allow",
            "Action": [
                "cloudwatch:*",
                "logs:*",
                "events:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "ConfigServiceAccess",
            "Effect": "Allow",
            "Action": [
                "config:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "ACMAccess",
            "Effect": "Allow",
            "Action": [
                "acm:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "WAFAccess",
            "Effect": "Allow",
            "Action": [
                "wafv2:*",
                "waf:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "SecretsManagerAccess",
            "Effect": "Allow",
            "Action": [
                "secretsmanager:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "VPCAccess",
            "Effect": "Allow",
            "Action": [
                "vpc:*",
                "servicediscovery:*"
            ],
            "Resource": "*"
        },
        {
            "Sid": "CloudTrailAccess",
            "Effect": "Allow",
            "Action": [
                "cloudtrail:*"
            ],
            "Resource": "*"
        }
    ]
}
EOF

    # Create the policy
    POLICY_ARN=$(aws iam create-policy \
        --policy-name $POLICY_NAME \
        --policy-document file:///tmp/fhir-bridge-test-policy.json \
        --description "Comprehensive policy for FHIR Bridge testing" \
        --query 'Policy.Arn' \
        --output text)
    
    echo -e "${GREEN}✓ Policy created: $POLICY_ARN${NC}"
}

# Function to create IAM group
create_test_group() {
    echo -e "\n${YELLOW}Creating IAM group...${NC}"
    
    # Create group
    aws iam create-group --group-name $IAM_GROUP_NAME 2>/dev/null || echo -e "${YELLOW}Group already exists${NC}"
    
    # Attach policy to group
    aws iam attach-group-policy \
        --group-name $IAM_GROUP_NAME \
        --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/$POLICY_NAME
    
    echo -e "${GREEN}✓ Group created and policy attached: $IAM_GROUP_NAME${NC}"
}

# Function to create IAM role
create_test_role() {
    echo -e "\n${YELLOW}Creating IAM role for testing...${NC}"
    
    cat > /tmp/trust-policy.json << EOF
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": {
                "Service": [
                    "ec2.amazonaws.com",
                    "ecs-tasks.amazonaws.com",
                    "rds.amazonaws.com"
                ]
            },
            "Action": "sts:AssumeRole"
        },
        {
            "Effect": "Allow",
            "Principal": {
                "AWS": "arn:aws:iam::$AWS_ACCOUNT_ID:root"
            },
            "Action": "sts:AssumeRole"
        }
    ]
}
EOF

    # Create role
    ROLE_ARN=$(aws iam create-role \
        --role-name $ROLE_NAME \
        --assume-role-policy-document file:///tmp/trust-policy.json \
        --query 'Role.Arn' \
        --output text)
    
    # Attach policy to role
    aws iam attach-role-policy \
        --role-name $ROLE_NAME \
        --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/$POLICY_NAME
    
    echo -e "${GREEN}✓ Role created: $ROLE_ARN${NC}"
}

# Function to create test user
create_test_user() {
    echo -e "\n${YELLOW}Creating test user...${NC}"
    
    # Create user
    aws iam create-user --user-name $IAM_USER_NAME 2>/dev/null || echo -e "${YELLOW}User already exists${NC}"
    
    # Add user to group
    aws iam add-user-to-group \
        --group-name $IAM_GROUP_NAME \
        --user-name $IAM_USER_NAME
    
    # Create access keys
    KEYS=$(aws iam create-access-key \
        --user-name $IAM_USER_NAME \
        --query 'AccessKey.[AccessKeyId,SecretAccessKey]' \
        --output text)
    
    ACCESS_KEY=$(echo $KEYS | cut -d' ' -f1)
    SECRET_KEY=$(echo $KEYS | cut -d' ' -f2)
    
    echo -e "${GREEN}✓ Test user created: $IAM_USER_NAME${NC}"
    echo -e "${YELLOW}Access Key: $ACCESS_KEY${NC}"
    echo -e "${YELLOW}Secret Key: $SECRET_KEY${NC}"
    echo -e "${RED}⚠️  Save these credentials securely!${NC}"
}

# Function to verify setup
verify_setup() {
    echo -e "\n${YELLOW}Verifying IAM setup...${NC}"
    
    # Test policy
    aws iam get-policy --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/$POLICY_NAME
    
    # Test group
    aws iam get-group --group-name $IAM_GROUP_NAME
    
    # Test role
    aws iam get-role --role-name $ROLE_NAME
    
    # Test user
    aws iam get-user --user-name $IAM_USER_NAME
    
    echo -e "${GREEN}✓ All IAM entities verified${NC}"
}

# Function to create cleanup script
create_cleanup_script() {
    echo -e "\n${YELLOW}Creating cleanup script...${NC}"
    
    cat > scripts/cleanup-aws-iam.sh << EOF
#!/bin/bash
# Cleanup script for FHIR Bridge IAM setup

echo "Cleaning up FHIR Bridge IAM resources..."

# Remove user from group
aws iam remove-user-from-group --group-name $IAM_GROUP_NAME --user-name $IAM_USER_NAME 2>/dev/null

# Delete user access keys
ACCESS_KEYS=\$(aws iam list-access-keys --user-name $IAM_USER_NAME --query 'AccessKeyMetadata[*].AccessKeyId' --output text)
for key in \$ACCESS_KEYS; do
    aws iam delete-access-key --user-name $IAM_USER_NAME --access-key-id \$key
done

# Delete user
aws iam delete-user --user-name $IAM_USER_NAME 2>/dev/null

# Detach policy from role
aws iam detach-role-policy --role-name $ROLE_NAME --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/$POLICY_NAME 2>/dev/null

# Delete role
aws iam delete-role --role-name $ROLE_NAME 2>/dev/null

# Detach policy from group
aws iam detach-group-policy --group-name $IAM_GROUP_NAME --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/$POLICY_NAME 2>/dev/null

# Delete group
aws iam delete-group --group-name $IAM_GROUP_NAME 2>/dev/null

# Delete policy
aws iam delete-policy --policy-arn arn:aws:iam::$AWS_ACCOUNT_ID:policy/$POLICY_NAME 2>/dev/null

echo "Cleanup completed!"
EOF

    chmod +x scripts/cleanup-aws-iam.sh
    echo -e "${GREEN}✓ Cleanup script created: scripts/cleanup-aws-iam.sh${NC}"
}

# Function to display usage instructions
display_usage() {
    echo -e "\n${BLUE}Usage Instructions${NC}"
    echo "=================="
    echo -e "${GREEN}1. Configure AWS CLI with new credentials:${NC}"
    echo "   aws configure"
    echo ""
    echo -e "${GREEN}2. Test the new permissions:${NC}"
    echo "   aws sts get-caller-identity"
    echo ""
    echo -e "${GREEN}3. Run Terraform deployment:${NC}"
    echo "   cd infra/terraform"
    echo "   terraform init"
    echo "   terraform plan"
    echo "   terraform apply"
    echo ""
    echo -e "${GREEN}4. Run integration tests:${NC}"
    echo "   ./scripts/integration-test-suite.sh"
    echo ""
    echo -e "${YELLOW}5. Cleanup when done:${NC}"
    echo "   ./scripts/cleanup-aws-iam.sh"
}

# Main execution
main() {
    check_aws_cli
    get_account_id
    
    echo -e "\n${YELLOW}This will create comprehensive IAM permissions for FHIR Bridge testing${NC}"
    echo -e "${RED}Warning: This creates powerful permissions. Use only in test environments.${NC}"
    
    read -p "Continue? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        create_test_policy
        create_test_group
        create_test_role
        create_test_user
        verify_setup
        create_cleanup_script
        display_usage
        
        echo -e "\n${GREEN}IAM setup completed successfully!${NC}"
        echo -e "${GREEN}Use the new credentials to run full infrastructure deployment${NC}"
    else
        echo -e "${YELLOW}Setup cancelled${NC}"
        exit 0
    fi
}

# Execute main function
main