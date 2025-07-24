# =============================================================================
# S3 Bucket Configuration for Database Backups
# =============================================================================
# This Terraform configuration creates S3 buckets for encrypted database backups
# with proper lifecycle policies and access controls for HIPAA compliance
# =============================================================================

# S3 bucket for database backups
resource "aws_s3_bucket" "fhir_bridge_db_backups" {
  bucket = "${var.project_name}-db-backups-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name        = "${var.project_name}-db-backups"
    Environment = var.environment
    Purpose     = "database-backups"
    Compliance  = "HIPAA"
  }
}

# Enable versioning for backup retention
resource "aws_s3_bucket_versioning" "fhir_bridge_db_backups" {
  bucket = aws_s3_bucket.fhir_bridge_db_backups.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Enable server-side encryption with KMS
resource "aws_s3_bucket_server_side_encryption_configuration" "fhir_bridge_db_backups" {
  bucket = aws_s3_bucket.fhir_bridge_db_backups.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm     = "aws:kms"
      kms_master_key_id = aws_kms_key.db_backup_key.arn
    }
  }
}

# Block public access
resource "aws_s3_bucket_public_access_block" "fhir_bridge_db_backups" {
  bucket = aws_s3_bucket.fhir_bridge_db_backups.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Lifecycle policy for backup retention
resource "aws_s3_bucket_lifecycle_configuration" "fhir_bridge_db_backups" {
  bucket = aws_s3_bucket.fhir_bridge_db_backups.id

  rule {
    id     = "backup-retention"
    status = "Enabled"

    expiration {
      days = var.backup_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = var.backup_retention_days
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }

  rule {
    id     = "transition-to-glacier"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "GLACIER"
    }

    noncurrent_version_transition {
      noncurrent_days = 30
      storage_class   = "GLACIER"
    }
  }
}

# KMS key for encryption
resource "aws_kms_key" "db_backup_key" {
  description             = "KMS key for database backup encryption"
  deletion_window_in_days = 7
  enable_key_rotation     = true

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action   = "kms:*"
        Resource = "*"
      },
      {
        Sid    = "Allow backup service role"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.backup_role.arn
        }
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name        = "${var.project_name}-db-backup-key"
    Environment = var.environment
    Purpose     = "database-backup-encryption"
  }
}

resource "aws_kms_alias" "db_backup_key" {
  name          = "alias/${var.project_name}-db-backup-key"
  target_key_id = aws_kms_key.db_backup_key.key_id
}

# IAM role for backup operations
resource "aws_iam_role" "backup_role" {
  name = "${var.project_name}-db-backup-role"

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
    Name        = "${var.project_name}-db-backup-role"
    Environment = var.environment
  }
}

# IAM policy for S3 access
resource "aws_iam_policy" "backup_policy" {
  name        = "${var.project_name}-db-backup-policy"
  description = "Policy for database backup operations"

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:PutObject",
          "s3:GetObject",
          "s3:DeleteObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.fhir_bridge_db_backups.arn,
          "${aws_s3_bucket.fhir_bridge_db_backups.arn}/*"
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey"
        ]
        Resource = aws_kms_key.db_backup_key.arn
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "backup_policy_attachment" {
  role       = aws_iam_role.backup_role.name
  policy_arn = aws_iam_policy.backup_policy.arn
}

# S3 bucket policy for additional security
resource "aws_s3_bucket_policy" "fhir_bridge_db_backups" {
  bucket = aws_s3_bucket.fhir_bridge_db_backups.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid    = "DenyInsecureConnections"
        Effect = "Deny"
        Principal = "*"
        Action = "s3:*"
        Resource = [
          aws_s3_bucket.fhir_bridge_db_backups.arn,
          "${aws_s3_bucket.fhir_bridge_db_backups.arn}/*"
        ]
        Condition = {
          Bool = {
            "aws:SecureTransport" = "false"
          }
        }
      }
    ]
  })
}

# CloudWatch log group for backup operations
resource "aws_cloudwatch_log_group" "db_backup_logs" {
  name              = "/aws/rds/instance/${var.project_name}-db/backups"
  retention_in_days = 30

  tags = {
    Name        = "${var.project_name}-db-backup-logs"
    Environment = var.environment
  }
}

# Variables
variable "project_name" {
  description = "Name of the project"
  type        = string
  default     = "fhir-bridge"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "production"
}

variable "backup_retention_days" {
  description = "Number of days to retain backups"
  type        = number
  default     = 30
}

# Data source for current AWS account
data "aws_caller_identity" "current" {}

# Outputs
output "backup_bucket_name" {
  description = "Name of the S3 bucket for database backups"
  value       = aws_s3_bucket.fhir_bridge_db_backups.id
}

output "backup_bucket_arn" {
  description = "ARN of the S3 bucket for database backups"
  value       = aws_s3_bucket.fhir_bridge_db_backups.arn
}

output "backup_kms_key_arn" {
  description = "ARN of the KMS key for backup encryption"
  value       = aws_kms_key.db_backup_key.arn
}

output "backup_role_arn" {
  description = "ARN of the IAM role for backup operations"
  value       = aws_iam_role.backup_role.arn
}