terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.1"
    }
  }
}

provider "aws" {
  region = "us-east-1"
}

# VPC / subnets / IGW
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_hostnames = true
  enable_dns_support   = true
  tags                 = { Name = "tefca-gateway-vpc" }
}

resource "aws_internet_gateway" "gw" {
  vpc_id = aws_vpc.main.id
  tags   = { Name = "tefca-gateway-igw" }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "us-east-1a"
  map_public_ip_on_launch = true
  tags                    = { Name = "public-subnet" }
}

# Public subnet in second AZ for ALB
resource "aws_subnet" "public_2" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.4.0/24"
  availability_zone       = "us-east-1b"
  map_public_ip_on_launch = true
  tags                    = { Name = "public-subnet-2" }
}

# Private subnets for database (multi-AZ)
resource "aws_subnet" "private_db_1" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.2.0/24"
  availability_zone = "us-east-1a"
  tags              = { Name = "private-db-subnet-1" }
}

resource "aws_subnet" "private_db_2" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.3.0/24"
  availability_zone = "us-east-1b"
  tags              = { Name = "private-db-subnet-2" }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.gw.id
  }
  tags = { Name = "public-rt" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "public_2" {
  subnet_id      = aws_subnet.public_2.id
  route_table_id = aws_route_table.public.id
}

# Security group (80/443 inbound)
resource "aws_security_group" "alb" {
  name_prefix = "tefca-alb-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Security group for RDS PostgreSQL
resource "aws_security_group" "rds" {
  name_prefix = "tefca-rds-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
    description     = "PostgreSQL access from application"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "tefca-rds-sg"
  }
}

# Security group for application (will be used by ECS tasks)
resource "aws_security_group" "app" {
  name_prefix = "tefca-app-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
    description     = "HTTP access from ALB"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "tefca-app-sg"
  }
}

# DB Subnet Group for RDS
resource "aws_db_subnet_group" "main" {
  name       = "tefca-db-subnet-group"
  subnet_ids = [aws_subnet.private_db_1.id, aws_subnet.private_db_2.id]

  tags = {
    Name = "tefca-db-subnet-group"
  }
}

# KMS Key for RDS encryption
resource "aws_kms_key" "rds" {
  description             = "KMS key for RDS encryption"
  deletion_window_in_days = 7

  tags = {
    Name = "tefca-rds-kms-key"
  }
}

resource "aws_kms_alias" "rds" {
  name          = "alias/tefca-rds"
  target_key_id = aws_kms_key.rds.key_id
}

# RDS PostgreSQL instance with encryption and automated backups
resource "aws_db_instance" "postgresql" {
  identifier = "tefca-fhir-bridge-db"

  # Engine configuration
  engine         = "postgres"
  engine_version = "15.8"
  instance_class = "db.t3.micro"

  # Database configuration
  db_name  = "fhirbridge"
  username = "fhirbridge_admin"
  password = "TempPassword123!" # Should be replaced with AWS Secrets Manager

  # Storage configuration
  allocated_storage     = 20
  max_allocated_storage = 100
  storage_type          = "gp3"
  storage_encrypted     = true

  # Network configuration
  db_subnet_group_name   = aws_db_subnet_group.main.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false

  # Backup configuration
  backup_retention_period = 7
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  # High availability and monitoring
  multi_az                        = false # Set to true for production
  monitoring_interval             = 60
  monitoring_role_arn             = aws_iam_role.rds_monitoring.arn
  enabled_cloudwatch_logs_exports = ["postgresql"]

  # Security and compliance
  deletion_protection = false # Set to true for production
  skip_final_snapshot = true  # Set to false for production

  # Performance insights
  performance_insights_enabled    = true

  tags = {
    Name        = "tefca-fhir-bridge-db"
    Environment = "development"
    Project     = "fhir-bridge"
  }

  depends_on = [aws_db_subnet_group.main]
}

# IAM role for RDS monitoring
resource "aws_iam_role" "rds_monitoring" {
  name = "tefca-rds-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "monitoring.rds.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "tefca-rds-monitoring-role"
  }
}

resource "aws_iam_role_policy_attachment" "rds_monitoring" {
  role       = aws_iam_role.rds_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

# ALB
resource "aws_lb" "main" {
  name               = "tefca-gateway-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = [aws_subnet.public.id, aws_subnet.public_2.id]
}

# Outputs
output "alb_dns" {
  value = aws_lb.main.dns_name
}

output "rds_endpoint" {
  value       = aws_db_instance.postgresql.endpoint
  description = "RDS PostgreSQL endpoint"
}

output "rds_port" {
  value       = aws_db_instance.postgresql.port
  description = "RDS PostgreSQL port"
}

output "database_name" {
  value       = aws_db_instance.postgresql.db_name
  description = "Database name"
}

output "kms_key_id" {
  value       = aws_kms_key.rds.id
  description = "KMS key ID for RDS encryption"
}