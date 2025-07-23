#!/bin/bash

# Docker Build and Deploy Script for FHIR Bridge

set -e

# Get ECR repository URL from Terraform output
ECR_REPO=$(terraform output -raw ecr_repository_url)
AWS_REGION="us-east-1"
IMAGE_TAG=${1:-latest}

echo "🐳 Building and deploying FHIR Bridge Docker image..."
echo "📦 ECR Repository: $ECR_REPO"
echo "🏷️  Image Tag: $IMAGE_TAG"

# Navigate to project root
cd ..

# Build the application
echo "🔨 Building application..."
./mvnw clean package -DskipTests

# Get ECR login token
echo "🔐 Logging into ECR..."
aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REPO

# Build Docker image
echo "🐳 Building Docker image..."
docker build -t fhir-bridge:$IMAGE_TAG .

# Tag image for ECR
docker tag fhir-bridge:$IMAGE_TAG $ECR_REPO:$IMAGE_TAG

# Push image to ECR
echo "📤 Pushing image to ECR..."
docker push $ECR_REPO:$IMAGE_TAG

# Update ECS service
echo "🔄 Updating ECS service..."
cd infra
aws ecs update-service \
    --cluster fhir-bridge-cluster \
    --service fhir-bridge-service \
    --force-new-deployment \
    --region $AWS_REGION

echo "✅ Docker image deployed successfully!"
echo "🔗 Monitor deployment: https://console.aws.amazon.com/ecs/home?region=$AWS_REGION#/clusters/fhir-bridge-cluster/services"