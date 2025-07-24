# FHIR Bridge Infrastructure Setup Guide

## Overview

This guide provides comprehensive instructions for setting up the complete FHIR Bridge infrastructure across different environments, from local development to production deployment on AWS.

## 📋 Prerequisites

### Required Tools
- **Terraform 1.5+**: Infrastructure as Code
- **AWS CLI 2.0+**: AWS management
- **Docker 20.10+**: Container runtime
- **kubectl**: Kubernetes CLI (for EKS deployments)
- **jq**: JSON processing

### AWS Requirements
- **AWS Account** with appropriate permissions
- **IAM User/Role** with administrative access
- **AWS CLI configured** with credentials
- **S3 bucket** for Terraform state (recommended)

### Local Development
- **Java 17+**: Application runtime
- **Maven 3.8+**: Build tool
- **PostgreSQL 14+**: Database (optional for local)
- **Redis 6+**: Cache (optional for local)

## 🏗️ Infrastructure Architecture

### AWS Architecture Overview
```
┌─────────────────────────────────────────────────────────────┐
│                        AWS Cloud                             │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │   Route 53          │    │   CloudFront        │        │
│  │   (DNS)             │    │   (CDN)             │        │
│  └──────────┬──────────┘    └──────────┬──────────┘        │
│             │                           │                   │
│  ┌──────────┴───────────────────────────┴──────────┐       │
│  │            Application Load Balancer             │       │
│  │              (SSL/TLS Termination)               │       │
│  └──────────┬───────────────────────────┬──────────┘       │
│             │                           │                   │
│  ┌──────────┴──────────┐    ┌──────────┴──────────┐       │
│  │   ECS Fargate       │    │   ECS Fargate       │       │
│  │   (Primary)         │    │   (Secondary)       │       │
│  └──────────┬──────────┘    └──────────┬──────────┘       │
│             │                           │                   │
│  ┌──────────┴──────────┐    ┌──────────┴──────────┐       │
│  │   RDS PostgreSQL    │    │   ElastiCache       │       │
│  │   (Multi-AZ)        │    │   Redis             │       │
│  └─────────────────────┘    └─────────────────────┘       │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Security & Monitoring                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐ │   │
│  │  │   WAF       │  │ CloudWatch  │  │   KMS       │ │   │
│  │  │   (WAF)     │  │   Alarms    │  │  (Keys)     │ │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Network Architecture
- **VPC**: Custom VPC with public/private subnets across 3 AZs
- **Subnets**: 
  - Public subnets: ALB, NAT Gateways
  - Private subnets: ECS tasks, RDS, Redis
- **Security Groups**: Least privilege access
- **NACLs**: Additional network security

## 🔧 Environment Setup

### 1. Local Development Setup

#### Quick Start with Docker
```bash
# Clone repository
git clone <repository-url>
cd fhir-bridge

# Start development environment
./scripts/dev-setup.sh setup

# Verify setup
curl http://localhost:8081/actuator/health
```

#### Manual Setup
```bash
# Install dependencies
./mvnw clean install

# Start PostgreSQL
docker run -d \
  --name postgres-dev \
  -e POSTGRES_DB=fhir_bridge \
  -e POSTGRES_USER=fhir_user \
  -e POSTGRES_PASSWORD=fhir_password \
  -p 5432:5432 \
  postgres:14

# Start Redis
docker run -d \
  --name redis-dev \
  -p 6379:6379 \
  redis:6-alpine

# Start application
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Staging Environment Setup

#### AWS Staging Deployment
```bash
# Configure AWS CLI
aws configure --profile fhir-bridge-staging

# Deploy staging infrastructure
cd infra/terraform
terraform workspace new staging
terraform init
terraform plan -var-file="staging.tfvars"
terraform apply
```

#### Staging Environment Variables
```bash
# Database
DB_HOST=staging-db.xxxxxxxx.us-east-1.rds.amazonaws.com
DB_NAME=fhir_bridge_staging
DB_USERNAME=staging_user
DB_PASSWORD=staging-secure-password

# Redis
REDIS_HOST=staging-redis.xxxxx.cache.amazonaws.com
REDIS_PORT=6379

# Security
JWT_ISSUER_URI=https://staging-auth.yourcompany.com
SSL_ENABLED=true
```

### 3. Production Environment Setup

#### Production Prerequisites
- **Route 53 hosted zone** configured
- **SSL/TLS certificates** in ACM
- **S3 bucket** for Terraform state
- **DynamoDB table** for state locking

#### Production Deployment
```bash
# Setup production workspace
cd infra/terraform
terraform workspace new production
terraform init -backend-config="backend-prod.conf"

# Plan production deployment
terraform plan -var-file="production.tfvars" -out=production-plan

# Apply production deployment
terraform apply production-plan
```

#### Production Environment Variables
```bash
# Database (RDS Multi-AZ)
DB_HOST=prod-db.xxxxxxxx.us-east-1.rds.amazonaws.com
DB_NAME=fhir_bridge_prod
DB_USERNAME=prod_user
DB_PASSWORD=prod-secure-password

# Redis (ElastiCache)
REDIS_HOST=prod-redis.xxxxx.cache.amazonaws.com
REDIS_PORT=6379

# Security
JWT_ISSUER_URI=https://auth.yourcompany.com
JWT_SECRET=prod-jwt-secret-min-256-bits
SSL_ENABLED=true
```

## 🏗️ Infrastructure Components

### 1. Networking Setup

#### VPC Configuration
```hcl
# infra/terraform/networking.tf
resource "aws_vpc" "fhir_bridge_vpc" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name        = "fhir-bridge-vpc"
    Environment = var.environment
  }
}
```

#### Subnet Configuration
```hcl
# Public subnets for ALB
resource "aws_subnet" "public" {
  count             = 3
  vpc_id            = aws_vpc.fhir_bridge_vpc.id
  cidr_block        = "10.0.${count.index + 1}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]
  
  map_public_ip_on_launch = true
  
  tags = {
    Name = "fhir-bridge-public-${count.index + 1}"
  }
}

# Private subnets for ECS and RDS
resource "aws_subnet" "private" {
  count             = 3
  vpc_id            = aws_vpc.fhir_bridge_vpc.id
  cidr_block        = "10.0.${count.index + 10}.0/24"
  availability_zone = data.aws_availability_zones.available.names[count.index]
  
  tags = {
    Name = "fhir-bridge-private-${count.index + 1}"
  }
}
```

### 2. Database Setup

#### RDS PostgreSQL Configuration
```hcl
# infra/terraform/rds.tf
resource "aws_db_instance" "fhir_bridge_db" {
  identifier     = "fhir-bridge-${var.environment}"
  engine         = "postgres"
  engine_version = "14.9"
  instance_class = var.db_instance_class
  
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_max_allocated_storage
  storage_encrypted     = true
  kms_key_id           = aws_kms_key.rds.arn
  
  db_name  = var.db_name
  username = var.db_username
  password = var.db_password
  
  vpc_security_group_ids = [aws_security_group.rds.id]
  db_subnet_group_name   = aws_db_subnet_group.main.name
  
  backup_retention_period = var.environment == "production" ? 30 : 7
  backup_window          = "03:00-04:00"
  maintenance_window     = "sun:04:00-sun:05:00"
  
  multi_az = var.environment == "production"
  
  enabled_cloudwatch_logs_exports = ["postgresql"]
  
  tags = {
    Name        = "fhir-bridge-db-${var.environment}"
    Environment = var.environment
  }
}
```

### 3. Redis Cache Setup

#### ElastiCache Configuration
```hcl
# infra/terraform/redis.tf
resource "aws_elasticache_subnet_group" "main" {
  name       = "fhir-bridge-${var.environment}"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_elasticache_replication_group" "fhir_bridge_redis" {
  replication_group_id       = "fhir-bridge-${var.environment}"
  description                = "FHIR Bridge Redis cache"
  
  node_type            = var.redis_node_type
  port                 = 6379
  parameter_group_name = "default.redis6.x"
  
  num_cache_clusters = var.environment == "production" ? 3 : 1
  
  subnet_group_name = aws_elasticache_subnet_group.main.name
  security_group_ids = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  
  tags = {
    Name        = "fhir-bridge-redis-${var.environment}"
    Environment = var.environment
  }
}
```

### 4. ECS Fargate Setup

#### ECS Cluster Configuration
```hcl
# infra/terraform/ecs.tf
resource "aws_ecs_cluster" "fhir_bridge_cluster" {
  name = "fhir-bridge-${var.environment}"
  
  setting {
    name  = "containerInsights"
    value = "enabled"
  }
  
  tags = {
    Name        = "fhir-bridge-cluster-${var.environment}"
    Environment = var.environment
  }
}
```

#### Task Definition
```json
{
  "family": "fhir-bridge",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "executionRoleArn": "${ecs_task_execution_role_arn}",
  "taskRoleArn": "${ecs_task_role_arn}",
  "containerDefinitions": [
    {
      "name": "fhir-bridge",
      "image": "${ecr_repository_url}:latest",
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        },
        {
          "containerPort": 8081,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "${environment}"
        },
        {
          "name": "DB_HOST",
          "value": "${db_host}"
        },
        {
          "name": "DB_NAME",
          "value": "${db_name}"
        },
        {
          "name": "REDIS_HOST",
          "value": "${redis_host}"
        }
      ],
      "secrets": [
        {
          "name": "DB_PASSWORD",
          "valueFrom": "${db_password_secret_arn}"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "${jwt_secret_arn}"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/fhir-bridge",
          "awslogs-region": "${aws_region}",
          "awslogs-stream-prefix": "ecs"
        }
      }
    }
  ]
}
```

### 5. Security Configuration

#### KMS Key Setup
```hcl
# infra/terraform/kms.tf
resource "aws_kms_key" "fhir_bridge" {
  description             = "FHIR Bridge encryption key"
  deletion_window_in_days = 10
  enable_key_rotation     = true
  
  tags = {
    Name        = "fhir-bridge-kms-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_kms_alias" "fhir_bridge" {
  name          = "alias/fhir-bridge-${var.environment}"
  target_key_id = aws_kms_key.fhir_bridge.key_id
}
```

#### Secrets Manager Configuration
```hcl
# infra/terraform/secrets.tf
resource "aws_secretsmanager_secret" "db_password" {
  name                    = "fhir-bridge-db-password-${var.environment}"
  recovery_window_in_days = 0
  
  tags = {
    Name        = "fhir-bridge-db-password-${var.environment}"
    Environment = var.environment
  }
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}
```

## 🚀 Deployment Procedures

### 1. Initial Setup

#### Configure AWS CLI
```bash
# Configure AWS CLI with named profile
aws configure --profile fhir-bridge
# Enter your AWS Access Key ID
# Enter your AWS Secret Access Key
# Enter your preferred region (e.g., us-east-1)
# Enter output format (json)
```

#### Setup Terraform Backend
```bash
# Create S3 bucket for Terraform state
aws s3 mb s3://fhir-bridge-terraform-state-${AWS_ACCOUNT_ID}

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket fhir-bridge-terraform-state-${AWS_ACCOUNT_ID} \
  --versioning-configuration Status=Enabled

# Create DynamoDB table for state locking
aws dynamodb create-table \
  --table-name fhir-bridge-terraform-locks \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
```

### 2. Environment-Specific Deployments

#### Development Deployment
```bash
cd infra/terraform

# Initialize Terraform
terraform init -backend-config="backend-dev.conf"

# Create development workspace
terraform workspace new dev
terraform workspace select dev

# Plan deployment
terraform plan \
  -var-file="dev.tfvars" \
  -out=dev-plan

# Apply deployment
terraform apply dev-plan
```

#### Staging Deployment
```bash
# Create staging workspace
terraform workspace new staging
terraform workspace select staging

# Plan staging deployment
terraform plan \
  -var-file="staging.tfvars" \
  -out=staging-plan

# Apply staging deployment
terraform apply staging-plan
```

#### Production Deployment
```bash
# Create production workspace
terraform workspace new production
terraform workspace select production

# Plan production deployment
terraform plan \
  -var-file="production.tfvars" \
  -out=production-plan

# Review plan carefully
terraform show production-plan

# Apply production deployment
terraform apply production-plan
```

### 3. Post-Deployment Configuration

#### Configure DNS
```bash
# Get ALB DNS name
ALB_DNS=$(terraform output -raw alb_dns_name)

# Create Route 53 record
aws route53 change-resource-record-sets \
  --hosted-zone-id YOUR_HOSTED_ZONE_ID \
  --change-batch '{
    "Changes": [{
      "Action": "CREATE",
      "ResourceRecordSet": {
        "Name": "api.yourcompany.com",
        "Type": "CNAME",
        "TTL": 300,
        "ResourceRecords": [{"Value": "'$ALB_DNS'"}]
      }
    }]
  }'
```

#### SSL Certificate Validation
```bash
# Check SSL certificate
openssl s_client -connect api.yourcompany.com:443 -servername api.yourcompany.com
```

## 🔍 Validation & Testing

### 1. Infrastructure Validation

#### Check Service Health
```bash
# Check ECS service health
aws ecs describe-services \
  --cluster fhir-bridge-production \
  --services fhir-bridge-service \
  --query 'services[0].healthStatus'

# Check target health
aws elbv2 describe-target-health \
  --target-group-arn $(terraform output -raw target_group_arn)
```

#### Database Connectivity Test
```bash
# Test database connection
aws rds describe-db-instances \
  --db-instance-identifier fhir-bridge-production \
  --query 'DBInstances[0].DBInstanceStatus'

# Connect to database
psql -h $(terraform output -raw db_endpoint) \
     -U $(terraform output -raw db_username) \
     -d fhir_bridge_prod
```

### 2. Application Testing

#### Health Check
```bash
# Check application health
curl -f https://api.yourcompany.com/actuator/health

# Check individual health indicators
curl -f https://api.yourcompany.com/actuator/health/db
curl -f https://api.yourcompany.com/actuator/health/redis
```

#### API Testing
```bash
# Test HL7 to FHIR transformation
curl -X POST https://api.yourcompany.com/api/transform/hl7 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{"hl7Message": "MSH|^~\\\\&|..."}'
```

## 📊 Monitoring Setup

### 1. CloudWatch Dashboards

#### Create Monitoring Dashboard
```bash
# Deploy CloudWatch dashboard
aws cloudwatch put-dashboard \
  --dashboard-name fhir-bridge-production \
  --dashboard-body '{
    "widgets": [
      {
        "type": "metric",
        "properties": {
          "metrics": [
            ["AWS/ECS", "CPUUtilization", "ServiceName", "fhir-bridge-service"],
            [".", "MemoryUtilization", ".", "."]
          ],
          "period": 300,
          "stat": "Average",
          "region": "us-east-1",
          "title": "ECS Service Metrics"
        }
      }
    ]
  }'
```

### 2. Alert Configuration

#### Setup CloudWatch Alarms
```bash
# Create high CPU alarm
aws cloudwatch put-metric-alarm \
  --alarm-name fhir-bridge-high-cpu \
  --alarm-description "High CPU utilization" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=ServiceName,Value=fhir-bridge-service
```

## 🧹 Cleanup

### Development Environment Cleanup
```bash
# Destroy development infrastructure
terraform workspace select dev
terraform destroy -var-file="dev.tfvars"

# Remove workspace
terraform workspace delete dev
```

### Complete Cleanup
```bash
# Destroy all resources
terraform workspace select production
terraform destroy -var-file="production.tfvars"

# Clean up S3 bucket
aws s3 rb s3://fhir-bridge-terraform-state-${AWS_ACCOUNT_ID} --force

# Clean up DynamoDB table
aws dynamodb delete-table --table-name fhir-bridge-terraform-locks
```

## 🚨 Troubleshooting

### Common Issues

#### IAM Permissions
```bash
# Check IAM permissions
aws sts get-caller-identity
aws iam simulate-principal-policy \
  --policy-source-arn arn:aws:iam::${AWS_ACCOUNT_ID}:role/your-role \
  --action-names ec2:DescribeInstances
```

#### Resource Limits
```bash
# Check service quotas
aws service-quotas get-service-quota \
  --service-code ecs \
  --quota-code L-1194D53C
```

#### Terraform State Issues
```bash
# Refresh Terraform state
terraform refresh

# Import existing resources
terraform import aws_instance.example i-1234567890abcdef0
```

## 📚 Additional Resources

- **[Terraform Documentation](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)**
- **[AWS ECS Best Practices](https://docs.aws.amazon.com/AmazonECS/latest/bestpracticesguide/)**
- **[AWS RDS Best Practices](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/CHAP_BestPractices.html)**
- **[Security Best Practices](docs/SECURITY.md)**
- **[Monitoring Guide](docs/MONITORING.md)**