# S3 Buckets for FHIR Bridge Application
# This file creates S3 buckets for audit logs, backups, and application artifacts
# with HIPAA-compliant security configurations

# KMS Key for S3 bucket encryption
resource "aws_kms_key" "s3" {
  description             = "KMS key for S3 bucket encryption"
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
        Sid    = "Allow key administrators"
        Effect = "Allow"
        Principal = {
          AWS = [
            "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root",
            "arn:aws:iam::${data.aws_caller_identity.current.account_id}:user/User_1"
          ]
        }
        Action = [
          "kms:Create*",
          "kms:Describe*",
          "kms:Enable*",
          "kms:List*",
          "kms:Put*",
          "kms:Update*",
          "kms:Revoke*",
          "kms:Disable*",
          "kms:Get*",
          "kms:Delete*",
          "kms:TagResource",
          "kms:UntagResource",
          "kms:ScheduleKeyDeletion",
          "kms:CancelKeyDeletion"
        ]
        Resource = "*"
      },
      {
        Sid    = "Allow S3 Service"
        Effect = "Allow"
        Principal = {
          Service = "s3.amazonaws.com"
        }
        Action = [
          "kms:Decrypt",
          "kms:GenerateDataKey",
          "kms:DescribeKey"
        ]
        Resource = "*"
      }
    ]
  })

  tags = {
    Name        = "tefca-s3-kms-key"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_kms_alias" "s3" {
  name          = "alias/tefca-s3"
  target_key_id = aws_kms_key.s3.key_id
}

# Data source for current AWS account ID
data "aws_caller_identity" "current" {}

# Random suffix for bucket names to ensure uniqueness
resource "random_id" "bucket_suffix" {
  byte_length = 4
}

# S3 Bucket for Audit Logs
resource "aws_s3_bucket" "audit_logs" {
  bucket = "tefca-fhir-bridge-audit-logs-${random_id.bucket_suffix.hex}"

  tags = {
    Name        = "tefca-fhir-bridge-audit-logs"
    Environment = "development"
    Project     = "fhir-bridge"
    Purpose     = "audit-logs"
    Compliance  = "HIPAA"
  }
}

# Audit logs bucket versioning
resource "aws_s3_bucket_versioning" "audit_logs" {
  bucket = aws_s3_bucket.audit_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Audit logs bucket encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "audit_logs" {
  bucket = aws_s3_bucket.audit_logs.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.s3.arn
      sse_algorithm     = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

# Audit logs bucket public access block
resource "aws_s3_bucket_public_access_block" "audit_logs" {
  bucket = aws_s3_bucket.audit_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Audit logs bucket lifecycle configuration
resource "aws_s3_bucket_lifecycle_configuration" "audit_logs" {
  bucket = aws_s3_bucket.audit_logs.id

  rule {
    id     = "audit_logs_lifecycle"
    status = "Enabled"

    filter {
      prefix = ""
    }

    # Transition to IA after 30 days
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    # Transition to Glacier after 90 days
    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    # Transition to Deep Archive after 365 days
    transition {
      days          = 365
      storage_class = "DEEP_ARCHIVE"
    }

    # Delete non-current versions after 90 days
    noncurrent_version_expiration {
      noncurrent_days = 90
    }

    # Clean up incomplete multipart uploads
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }

  # Additional rule for different log types with different retention
  rule {
    id     = "security_logs_extended_retention"
    status = "Enabled"

    filter {
      prefix = "security-logs/"
    }

    # Keep security logs longer for compliance
    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 180
      storage_class = "GLACIER"
    }

    transition {
      days          = 730
      storage_class = "DEEP_ARCHIVE"
    }

    # Keep security log versions longer
    noncurrent_version_expiration {
      noncurrent_days = 180
    }
  }

  # Rule for temporary/debug logs with shorter retention
  rule {
    id     = "temp_logs_short_retention"
    status = "Enabled"

    filter {
      prefix = "temp-logs/"
    }

    # Delete temporary logs after 30 days
    expiration {
      days = 30
    }

    # Delete non-current versions after 7 days
    noncurrent_version_expiration {
      noncurrent_days = 7
    }
  }
}

# S3 Bucket for Backups
resource "aws_s3_bucket" "backups" {
  bucket = "tefca-fhir-bridge-backups-${random_id.bucket_suffix.hex}"

  tags = {
    Name        = "tefca-fhir-bridge-backups"
    Environment = "development"
    Project     = "fhir-bridge"
    Purpose     = "backups"
    Compliance  = "HIPAA"
  }
}

# Backups bucket versioning
resource "aws_s3_bucket_versioning" "backups" {
  bucket = aws_s3_bucket.backups.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Backups bucket encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.s3.arn
      sse_algorithm     = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

# Backups bucket public access block
resource "aws_s3_bucket_public_access_block" "backups" {
  bucket = aws_s3_bucket.backups.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Backups bucket lifecycle configuration
resource "aws_s3_bucket_lifecycle_configuration" "backups" {
  bucket = aws_s3_bucket.backups.id

  rule {
    id     = "database_backups_lifecycle"
    status = "Enabled"

    filter {
      prefix = "database-backups/"
    }

    # Transition to IA after 30 days (minimum for STANDARD_IA)
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    # Transition to Glacier after 90 days
    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    # Transition to Deep Archive after 1 year
    transition {
      days          = 365
      storage_class = "DEEP_ARCHIVE"
    }

    # Keep database backups for 7 years (2557 days) for compliance
    expiration {
      days = 2557
    }

    # Delete non-current versions after 90 days
    noncurrent_version_expiration {
      noncurrent_days = 90
    }

    # Clean up incomplete multipart uploads
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }

  rule {
    id     = "application_backups_lifecycle"
    status = "Enabled"

    filter {
      prefix = "application-backups/"
    }

    # Application backups have shorter retention
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    # Keep application backups for 2 years
    expiration {
      days = 730
    }

    # Delete non-current versions after 30 days
    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }

  rule {
    id     = "configuration_backups_lifecycle"
    status = "Enabled"

    filter {
      prefix = "config-backups/"
    }

    # Configuration backups kept longer for rollback capability
    transition {
      days          = 60
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 180
      storage_class = "GLACIER"
    }

    # Keep configuration backups for 3 years
    expiration {
      days = 1095
    }

    # Keep more versions of configuration backups
    noncurrent_version_expiration {
      noncurrent_days = 180
    }
  }
}

# S3 Bucket for Application Artifacts
resource "aws_s3_bucket" "artifacts" {
  bucket = "tefca-fhir-bridge-artifacts-${random_id.bucket_suffix.hex}"

  tags = {
    Name        = "tefca-fhir-bridge-artifacts"
    Environment = "development"
    Project     = "fhir-bridge"
    Purpose     = "artifacts"
  }
}

# Artifacts bucket versioning
resource "aws_s3_bucket_versioning" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id
  versioning_configuration {
    status = "Enabled"
  }
}

# Artifacts bucket encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  rule {
    apply_server_side_encryption_by_default {
      kms_master_key_id = aws_kms_key.s3.arn
      sse_algorithm     = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

# Artifacts bucket public access block
resource "aws_s3_bucket_public_access_block" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Artifacts bucket lifecycle configuration
resource "aws_s3_bucket_lifecycle_configuration" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  rule {
    id     = "application_artifacts_lifecycle"
    status = "Enabled"

    filter {
      prefix = "applications/"
    }

    # Transition older application versions to cheaper storage
    transition {
      days          = 90
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 180
      storage_class = "GLACIER"
    }

    # Keep application artifacts for 2 years
    expiration {
      days = 730
    }

    # Keep only the latest 20 versions of applications
    noncurrent_version_expiration {
      noncurrent_days = 60
    }

    # Clean up incomplete multipart uploads
    abort_incomplete_multipart_upload {
      days_after_initiation = 1
    }
  }

  rule {
    id     = "configuration_artifacts_lifecycle"
    status = "Enabled"

    filter {
      prefix = "configurations/"
    }

    # Configuration files accessed more frequently
    transition {
      days          = 60
      storage_class = "STANDARD_IA"
    }

    # Keep configuration artifacts for 1 year
    expiration {
      days = 365
    }

    # Keep more versions of configuration files
    noncurrent_version_expiration {
      noncurrent_days = 90
    }
  }

  rule {
    id     = "deployment_artifacts_lifecycle"
    status = "Enabled"

    filter {
      prefix = "deployments/"
    }

    # Deployment artifacts for rollback capability
    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    # Keep deployment artifacts for 6 months
    expiration {
      days = 180
    }

    # Keep recent deployment versions
    noncurrent_version_expiration {
      noncurrent_days = 30
    }
  }

  rule {
    id     = "temp_artifacts_lifecycle"
    status = "Enabled"

    filter {
      prefix = "temp/"
    }

    # Delete temporary artifacts quickly
    expiration {
      days = 7
    }

    # Delete non-current versions immediately
    noncurrent_version_expiration {
      noncurrent_days = 1
    }
  }
}
# S3 Access Logging Bucket (for audit trail of S3 access)
resource "aws_s3_bucket" "access_logs" {
  bucket = "tefca-fhir-bridge-access-logs-${random_id.bucket_suffix.hex}"

  tags = {
    Name        = "tefca-fhir-bridge-access-logs"
    Environment = "development"
    Project     = "fhir-bridge"
    Purpose     = "access-logs"
  }
}

# Access logs bucket encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Access logs bucket public access block
resource "aws_s3_bucket_public_access_block" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Access logs bucket lifecycle
resource "aws_s3_bucket_lifecycle_configuration" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  rule {
    id     = "access_logs_lifecycle"
    status = "Enabled"

    filter {
      prefix = ""
    }

    # Delete access logs after 90 days
    expiration {
      days = 90
    }
  }
}

# Enable access logging for audit logs bucket
resource "aws_s3_bucket_logging" "audit_logs" {
  bucket = aws_s3_bucket.audit_logs.id

  target_bucket = aws_s3_bucket.access_logs.id
  target_prefix = "audit-logs-access/"
}

# Enable access logging for backups bucket
resource "aws_s3_bucket_logging" "backups" {
  bucket = aws_s3_bucket.backups.id

  target_bucket = aws_s3_bucket.access_logs.id
  target_prefix = "backups-access/"
}

# Enable access logging for artifacts bucket
resource "aws_s3_bucket_logging" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  target_bucket = aws_s3_bucket.access_logs.id
  target_prefix = "artifacts-access/"
}

# Bucket policies for enhanced security

# Audit logs bucket policy - restrict access to specific IAM roles
resource "aws_s3_bucket_policy" "audit_logs" {
  bucket = aws_s3_bucket.audit_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "DenyInsecureConnections"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          Bool = {
            "aws:SecureTransport" = "false"
          }
        }
      },
      {
        Sid       = "DenyUnencryptedObjectUploads"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.audit_logs.arn}/*"
        Condition = {
          StringNotEquals = {
            "s3:x-amz-server-side-encryption" = "aws:kms"
          }
        }
      },
      {
        Sid       = "DenyIncorrectEncryptionHeader"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.audit_logs.arn}/*"
        Condition = {
          StringNotEquals = {
            "s3:x-amz-server-side-encryption-aws-kms-key-id" = aws_kms_key.s3.arn
          }
        }
      },
      {
        Sid       = "DenyPublicReadAccess"
        Effect    = "Deny"
        Principal = "*"
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion"
        ]
        Resource = "${aws_s3_bucket.audit_logs.arn}/*"
        Condition = {
          StringEquals = {
            "aws:PrincipalServiceName" = ""
          }
        }
      },
      {
        Sid       = "DenyOldTLSVersions"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          NumericLessThan = {
            "s3:TlsVersion" = "1.2"
          }
        }
      },
      {
        Sid       = "RestrictSourceIP"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          Bool = {
            "aws:ViaAWSService" = "false"
          }
          StringNotEquals = {
            "aws:PrincipalArn" = [
              aws_iam_role.ecs_task.arn,
              "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
            ]
          }
        }
      },
      {
        Sid    = "AllowApplicationAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_task.arn
        }
        Action = [
          "s3:PutObject",
          "s3:PutObjectAcl",
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          StringEquals = {
            "s3:x-amz-server-side-encryption" = "aws:kms"
            "s3:x-amz-server-side-encryption-aws-kms-key-id" = aws_kms_key.s3.arn
          }
          DateGreaterThan = {
            "aws:CurrentTime" = "2024-01-01T00:00:00Z"
          }
        }
      },
      {
        Sid    = "AllowReadOnlyAccessForCompliance"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action = [
          "s3:GetObject",
          "s3:GetObjectVersion",
          "s3:ListBucket",
          "s3:ListBucketVersions"
        ]
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          StringEquals = {
            "s3:ExistingObjectTag/Purpose" = "audit"
          }
        }
      },
      {
        Sid    = "DenyDeleteOperations"
        Effect = "Deny"
        Principal = "*"
        Action = [
          "s3:DeleteObject",
          "s3:DeleteObjectVersion",
          "s3:DeleteBucket"
        ]
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          StringNotEquals = {
            "aws:PrincipalArn" = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
          }
        }
      },
      {
        Sid    = "RequireMFAForSensitiveOperations"
        Effect = "Deny"
        Principal = "*"
        Action = [
          "s3:DeleteObject",
          "s3:DeleteObjectVersion",
          "s3:PutBucketPolicy",
          "s3:DeleteBucketPolicy"
        ]
        Resource = [
          aws_s3_bucket.audit_logs.arn,
          "${aws_s3_bucket.audit_logs.arn}/*"
        ]
        Condition = {
          BoolIfExists = {
            "aws:MultiFactorAuthPresent" = "false"
          }
        }
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.audit_logs]
}

# Backups bucket policy
resource "aws_s3_bucket_policy" "backups" {
  bucket = aws_s3_bucket.backups.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "DenyInsecureConnections"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.backups.arn,
          "${aws_s3_bucket.backups.arn}/*"
        ]
        Condition = {
          Bool = {
            "aws:SecureTransport" = "false"
          }
        }
      },
      {
        Sid       = "EnforceEncryption"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.backups.arn}/*"
        Condition = {
          StringNotEquals = {
            "s3:x-amz-server-side-encryption" = "aws:kms"
          }
        }
      },
      {
        Sid       = "DenyOldTLSVersions"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.backups.arn,
          "${aws_s3_bucket.backups.arn}/*"
        ]
        Condition = {
          NumericLessThan = {
            "s3:TlsVersion" = "1.2"
          }
        }
      },
      {
        Sid       = "RestrictTimeBasedAccess"
        Effect    = "Deny"
        Principal = "*"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.backups.arn}/*"
        Condition = {
          DateLessThan = {
            "aws:CurrentTime" = "2024-01-01T00:00:00Z"
          }
        }
      },
      {
        Sid    = "AllowBackupAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_task.arn
        }
        Action = [
          "s3:PutObject",
          "s3:PutObjectAcl",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:GetObjectVersion"
        ]
        Resource = [
          aws_s3_bucket.backups.arn,
          "${aws_s3_bucket.backups.arn}/*"
        ]
        Condition = {
          StringEquals = {
            "s3:x-amz-server-side-encryption" = "aws:kms"
          }
        }
      },
      {
        Sid    = "AllowRestoreOperations"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_task.arn
        }
        Action = [
          "s3:RestoreObject"
        ]
        Resource = "${aws_s3_bucket.backups.arn}/*"
        Condition = {
          StringLike = {
            "s3:prefix" = [
              "database-backups/*",
              "application-backups/*"
            ]
          }
        }
      },
      {
        Sid    = "DenyDeleteOperationsExceptLifecycle"
        Effect = "Deny"
        Principal = "*"
        Action = [
          "s3:DeleteObject",
          "s3:DeleteObjectVersion"
        ]
        Resource = "${aws_s3_bucket.backups.arn}/*"
        Condition = {
          StringNotEquals = {
            "aws:PrincipalArn" = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
          }
          StringNotLike = {
            "aws:userid" = "AIDACKCEVSQ6C2EXAMPLE:*"
          }
        }
      },
      {
        Sid    = "RequireMFAForCriticalOperations"
        Effect = "Deny"
        Principal = "*"
        Action = [
          "s3:DeleteObject",
          "s3:DeleteObjectVersion",
          "s3:PutBucketPolicy",
          "s3:DeleteBucketPolicy",
          "s3:PutBucketVersioning"
        ]
        Resource = [
          aws_s3_bucket.backups.arn,
          "${aws_s3_bucket.backups.arn}/*"
        ]
        Condition = {
          BoolIfExists = {
            "aws:MultiFactorAuthPresent" = "false"
          }
        }
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.backups]
}

# Artifacts bucket policy
resource "aws_s3_bucket_policy" "artifacts" {
  bucket = aws_s3_bucket.artifacts.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "DenyInsecureConnections"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "${aws_s3_bucket.artifacts.arn}/*"
        ]
        Condition = {
          Bool = {
            "aws:SecureTransport" = "false"
          }
        }
      },
      {
        Sid       = "EnforceEncryption"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.artifacts.arn}/*"
        Condition = {
          StringNotEquals = {
            "s3:x-amz-server-side-encryption" = "aws:kms"
          }
        }
      },
      {
        Sid       = "DenyOldTLSVersions"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:*"
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "${aws_s3_bucket.artifacts.arn}/*"
        ]
        Condition = {
          NumericLessThan = {
            "s3:TlsVersion" = "1.2"
          }
        }
      },
      {
        Sid       = "RestrictFileTypes"
        Effect    = "Deny"
        Principal = "*"
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.artifacts.arn}/*"
        Condition = {
          StringNotLike = {
            "s3:x-amz-content-sha256" = "*"
          }
          StringNotEquals = {
            "s3:x-amz-content-type" = [
              "application/java-archive",
              "application/zip",
              "application/x-tar",
              "application/gzip",
              "text/yaml",
              "application/json",
              "text/plain"
            ]
          }
        }
      },
      {
        Sid    = "AllowArtifactAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_task.arn
        }
        Action = [
          "s3:PutObject",
          "s3:PutObjectAcl",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:GetObjectVersion"
        ]
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "${aws_s3_bucket.artifacts.arn}/*"
        ]
        Condition = {
          StringEquals = {
            "s3:x-amz-server-side-encryption" = "aws:kms"
          }
        }
      },
      {
        Sid    = "AllowDeploymentAccess"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_execution.arn
        }
        Action = [
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "${aws_s3_bucket.artifacts.arn}/applications/*",
          "${aws_s3_bucket.artifacts.arn}/configurations/*"
        ]
      },
      {
        Sid    = "RestrictTemporaryFileAccess"
        Effect = "Deny"
        Principal = "*"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.artifacts.arn}/temp/*"
        Condition = {
          DateGreaterThan = {
            "aws:CurrentTime" = "2025-12-31T23:59:59Z"
          }
        }
      },
      {
        Sid    = "AllowVersionManagement"
        Effect = "Allow"
        Principal = {
          AWS = aws_iam_role.ecs_task.arn
        }
        Action = [
          "s3:DeleteObjectVersion"
        ]
        Resource = "${aws_s3_bucket.artifacts.arn}/*"
        Condition = {
          StringLike = {
            "s3:prefix" = [
              "temp/*",
              "deployments/old/*"
            ]
          }
        }
      },
      {
        Sid    = "RequireMFAForSensitiveOperations"
        Effect = "Deny"
        Principal = "*"
        Action = [
          "s3:DeleteObject",
          "s3:PutBucketPolicy",
          "s3:DeleteBucketPolicy"
        ]
        Resource = [
          aws_s3_bucket.artifacts.arn,
          "${aws_s3_bucket.artifacts.arn}/*"
        ]
        Condition = {
          BoolIfExists = {
            "aws:MultiFactorAuthPresent" = "false"
          }
          StringNotLike = {
            "s3:prefix" = "temp/*"
          }
        }
      }
    ]
  })

  depends_on = [aws_s3_bucket_public_access_block.artifacts]
}

# CloudWatch Log Group for S3 access monitoring
resource "aws_cloudwatch_log_group" "s3_access" {
  name              = "/aws/s3/fhir-bridge-access"
  retention_in_days = 90

  tags = {
    Name        = "fhir-bridge-s3-access-logs"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# Outputs for S3 buckets
output "s3_audit_logs_bucket" {
  value       = aws_s3_bucket.audit_logs.bucket
  description = "S3 bucket name for audit logs"
}

output "s3_backups_bucket" {
  value       = aws_s3_bucket.backups.bucket
  description = "S3 bucket name for backups"
}

output "s3_artifacts_bucket" {
  value       = aws_s3_bucket.artifacts.bucket
  description = "S3 bucket name for application artifacts"
}

output "s3_access_logs_bucket" {
  value       = aws_s3_bucket.access_logs.bucket
  description = "S3 bucket name for access logs"
}

output "s3_kms_key_id" {
  value       = aws_kms_key.s3.id
  description = "KMS key ID for S3 bucket encryption"
}

output "s3_kms_key_arn" {
  value       = aws_kms_key.s3.arn
  description = "KMS key ARN for S3 bucket encryption"
}