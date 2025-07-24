# VPC Flow Logs for network monitoring
resource "aws_iam_role" "vpc_flow_logs" {
  name = "vpc-flow-logs-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "vpc-flow-logs.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "vpc-flow-logs-role"
  }
}

resource "aws_iam_role_policy" "vpc_flow_logs_policy" {
  name = "vpc-flow-logs-policy"
  role = aws_iam_role.vpc_flow_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogGroups",
          "logs:DescribeLogStreams"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "vpc_flow_logs" {
  name              = "/aws/vpc/flowlogs"
  retention_in_days = 30

  tags = {
    Name = "vpc-flow-logs"
  }
}

resource "aws_flow_log" "vpc" {
  iam_role_arn    = aws_iam_role.vpc_flow_logs.arn
  log_destination = aws_cloudwatch_log_group.vpc_flow_logs.arn
  traffic_type    = "ALL"
  vpc_id          = aws_vpc.main.id

  tags = {
    Name = "vpc-flow-log"
  }
}

# CloudWatch Log Groups with HIPAA-compliant retention policies
resource "aws_cloudwatch_log_group" "application_logs" {
  name              = "/ecs/fhir-bridge-app"
  retention_in_days = 90  # HIPAA compliance: retain logs for 90 days minimum

  tags = {
    Name        = "fhir-bridge-app-logs"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_cloudwatch_log_group" "security_logs" {
  name              = "/security/fhir-bridge"
  retention_in_days = 365  # Security logs retained for 1 year

  tags = {
    Name        = "fhir-bridge-security-logs"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_cloudwatch_log_group" "audit_logs" {
  name              = "/audit/fhir-bridge"
  retention_in_days = 2557  # HIPAA compliance: 7 years for audit logs

  tags = {
    Name        = "fhir-bridge-audit-logs"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}