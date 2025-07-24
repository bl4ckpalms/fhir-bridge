# SSL/TLS Certificate via ACM
resource "aws_acm_certificate" "main" {
  domain_name       = "fhir-bridge.tefca.com"
  validation_method = "DNS"

  subject_alternative_names = [
    "*.fhir-bridge.tefca.com"
  ]

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "fhir-bridge-ssl-cert"
  }
}

# Additional KMS keys for encryption key management
resource "aws_kms_key" "cloudtrail" {
  description             = "KMS key for CloudTrail encryption"
  deletion_window_in_days = 7

  tags = {
    Name = "fhir-bridge-cloudtrail-kms-key"
  }
}

resource "aws_kms_alias" "cloudtrail" {
  name          = "alias/fhir-bridge-cloudtrail"
  target_key_id = aws_kms_key.cloudtrail.key_id
}

resource "aws_kms_key" "secrets" {
  description             = "KMS key for Secrets Manager encryption"
  deletion_window_in_days = 7

  tags = {
    Name = "fhir-bridge-secrets-kms-key"
  }
}

resource "aws_kms_alias" "secrets" {
  name          = "alias/fhir-bridge-secrets"
  target_key_id = aws_kms_key.secrets.key_id
}

# VPC Endpoints for AWS services
resource "aws_vpc_endpoint" "s3" {
  vpc_id       = aws_vpc.main.id
  service_name = "com.amazonaws.us-east-1.s3"
  vpc_endpoint_type = "Gateway"

  route_table_ids = [
    aws_route_table.private.id
  ]

  tags = {
    Name = "s3-vpc-endpoint"
  }
}

resource "aws_vpc_endpoint" "cloudwatch_logs" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.logs"
  vpc_endpoint_type = "Interface"

  subnet_ids = [
    aws_subnet.private_app_1.id,
    aws_subnet.private_app_2.id
  ]

  security_group_ids = [aws_security_group.vpc_endpoints.id]

  tags = {
    Name = "cloudwatch-logs-vpc-endpoint"
  }
}

resource "aws_vpc_endpoint" "secretsmanager" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.secretsmanager"
  vpc_endpoint_type = "Interface"

  subnet_ids = [
    aws_subnet.private_app_1.id,
    aws_subnet.private_app_2.id
  ]

  security_group_ids = [aws_security_group.vpc_endpoints.id]

  tags = {
    Name = "secretsmanager-vpc-endpoint"
  }
}

resource "aws_vpc_endpoint" "ecr_api" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.ecr.api"
  vpc_endpoint_type = "Interface"

  subnet_ids = [
    aws_subnet.private_app_1.id,
    aws_subnet.private_app_2.id
  ]

  security_group_ids = [aws_security_group.vpc_endpoints.id]

  tags = {
    Name = "ecr-api-vpc-endpoint"
  }
}

resource "aws_vpc_endpoint" "ecr_dkr" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.ecr.dkr"
  vpc_endpoint_type = "Interface"

  subnet_ids = [
    aws_subnet.private_app_1.id,
    aws_subnet.private_app_2.id
  ]

  security_group_ids = [aws_security_group.vpc_endpoints.id]

  tags = {
    Name = "ecr-dkr-vpc-endpoint"
  }
}

resource "aws_vpc_endpoint" "ecs" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.ecs"
  vpc_endpoint_type = "Interface"

  subnet_ids = [
    aws_subnet.private_app_1.id,
    aws_subnet.private_app_2.id
  ]

  security_group_ids = [aws_security_group.vpc_endpoints.id]

  tags = {
    Name = "ecs-vpc-endpoint"
  }
}

resource "aws_vpc_endpoint" "ecs_telemetry" {
  vpc_id            = aws_vpc.main.id
  service_name      = "com.amazonaws.us-east-1.ecs-telemetry"
  vpc_endpoint_type = "Interface"

  subnet_ids = [
    aws_subnet.private_app_1.id,
    aws_subnet.private_app_2.id
  ]

  security_group_ids = [aws_security_group.vpc_endpoints.id]

  tags = {
    Name = "ecs-telemetry-vpc-endpoint"
  }
}

# Security group for VPC endpoints
resource "aws_security_group" "vpc_endpoints" {
  name_prefix = "vpc-endpoints-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["10.0.0.0/16"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "vpc-endpoints-sg"
  }
}