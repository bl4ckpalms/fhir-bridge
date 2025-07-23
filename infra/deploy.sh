#!/bin/bash

# FHIR Bridge AWS Infrastructure Deployment Script

set -e

echo "🚀 Starting FHIR Bridge infrastructure deployment..."

# Check prerequisites
echo "📋 Checking prerequisites..."
if ! command -v terraform &> /dev/null; then
    echo "❌ Terraform is not installed. Please install Terraform >= 1.5"
    exit 1
fi

if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI is not installed. Please install AWS CLI"
    exit 1
fi

# Check AWS credentials
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS credentials not configured. Run 'aws configure'"
    exit 1
fi

echo "✅ Prerequisites check passed"

# Initialize Terraform
echo "🔧 Initializing Terraform..."
terraform init

# Validate configuration
echo "🔍 Validating Terraform configuration..."
terraform validate

# Plan deployment
echo "📋 Planning infrastructure deployment..."
terraform plan -out=tfplan

# Ask for confirmation
read -p "🤔 Do you want to apply these changes? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Deployment cancelled"
    exit 1
fi

# Apply changes
echo "🚀 Deploying infrastructure..."
terraform apply tfplan

# Get outputs
echo "📊 Infrastructure deployment completed!"
echo "🔗 Application Load Balancer DNS: $(terraform output -raw alb_dns)"
echo "🗄️  Database Endpoint: $(terraform output -raw rds_endpoint)"
echo "🐳 ECR Repository: $(terraform output -raw ecr_repository_url)"
echo "🔴 Redis Endpoint: $(terraform output -raw redis_endpoint)"

echo "✅ FHIR Bridge infrastructure is ready!"
echo "📝 Next steps:"
echo "   1. Build and push Docker image to ECR"
echo "   2. Update ECS service with new image"
echo "   3. Configure application properties for AWS environment"