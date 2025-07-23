# IAM roles for ECS tasks

# ECS Execution Role
resource "aws_iam_role" "ecs_execution" {
  name = "fhir-bridge-ecs-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "fhir-bridge-ecs-execution-role"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_iam_role_policy_attachment" "ecs_execution_role_policy" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Additional policy for Secrets Manager access
resource "aws_iam_role_policy" "ecs_secrets_policy" {
  name = "fhir-bridge-ecs-secrets-policy"
  role = aws_iam_role.ecs_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue"
        ]
        Resource = [
          aws_secretsmanager_secret.db_credentials.arn,
          aws_secretsmanager_secret.redis_auth.arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt"
        ]
        Resource = [
          aws_kms_key.rds.arn
        ]
      }
    ]
  })
}

# ECS Task Role (for application runtime permissions)
resource "aws_iam_role" "ecs_task" {
  name = "fhir-bridge-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ecs-tasks.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "fhir-bridge-ecs-task-role"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# Policy for application to access AWS services (CloudWatch, S3, etc.)
resource "aws_iam_role_policy" "ecs_task_policy" {
  name = "fhir-bridge-ecs-task-policy"
  role = aws_iam_role.ecs_task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = [
          "${aws_cloudwatch_log_group.fhir_bridge.arn}:*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:PutObjectAcl",
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          "arn:aws:s3:::tefca-fhir-bridge-audit-logs-*",
          "arn:aws:s3:::tefca-fhir-bridge-audit-logs-*/*",
          "arn:aws:s3:::tefca-fhir-bridge-backups-*",
          "arn:aws:s3:::tefca-fhir-bridge-backups-*/*",
          "arn:aws:s3:::tefca-fhir-bridge-artifacts-*",
          "arn:aws:s3:::tefca-fhir-bridge-artifacts-*/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ]
        Resource = [
          "arn:aws:kms:*:*:key/*"
        ]
        Condition = {
          StringEquals = {
            "kms:ViaService" = "s3.us-east-1.amazonaws.com"
          }
        }
      }
    ]
  })
}