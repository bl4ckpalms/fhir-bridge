# CloudWatch Logs Insights Query Definitions
resource "aws_cloudwatch_query_definition" "application_errors" {
  name = "FHIRBridge-Application-Errors"

  log_group_names = [
    aws_cloudwatch_log_group.application_logs.name
  ]

  query_string = <<EOF
fields @timestamp, level, message, requestId, userId
| filter level = 'ERROR'
| sort @timestamp desc
| limit 100
EOF
}

resource "aws_cloudwatch_query_definition" "security_events" {
  name = "FHIRBridge-Security-Events"

  log_group_names = [
    aws_cloudwatch_log_group.security_logs.name
  ]

  query_string = <<EOF
fields @timestamp, level, message, userId, action, ipAddress
| filter level in ['WARN', 'ERROR'] or message like /authentication failed/ or message like /authorization failed/
| sort @timestamp desc
| limit 100
EOF
}

resource "aws_cloudwatch_query_definition" "performance_analysis" {
  name = "FHIRBridge-Performance-Analysis"

  log_group_names = [
    aws_cloudwatch_log_group.application_logs.name
  ]

  query_string = <<EOF
fields @timestamp, message, duration, requestId, endpoint
| filter message like /Request completed/
| parse message /Request completed in (?<duration>\d+)ms/
| stats avg(duration), max(duration), min(duration), count() by bin(5m)
| sort @timestamp desc
EOF
}

resource "aws_cloudwatch_query_definition" "audit_trail" {
  name = "FHIRBridge-Audit-Trail"

  log_group_names = [
    aws_cloudwatch_log_group.audit_logs.name
  ]

  query_string = <<EOF
fields @timestamp, userId, action, resourceType, resourceId, outcome
| sort @timestamp desc
| limit 200
EOF
}

# Lambda function for log processing and analysis
resource "aws_iam_role" "log_processor" {
  name = "fhir-bridge-log-processor-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name        = "fhir-bridge-log-processor-role"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

resource "aws_iam_role_policy" "log_processor_policy" {
  name = "fhir-bridge-log-processor-policy"
  role = aws_iam_role.log_processor.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:DescribeLogStreams",
          "logs:DescribeLogGroups",
          "logs:FilterLogEvents"
        ]
        Resource = "*"
      },
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject"
        ]
        Resource = "${aws_s3_bucket.log_analysis.arn}/*"
      }
    ]
  })
}

# S3 bucket for log analysis results
resource "aws_s3_bucket" "log_analysis" {
  bucket = "fhir-bridge-log-analysis-${var.environment}-${data.aws_caller_identity.current.account_id}"

  tags = {
    Name        = "fhir-bridge-log-analysis"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

resource "aws_s3_bucket_versioning" "log_analysis" {
  bucket = aws_s3_bucket.log_analysis.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "log_analysis" {
  bucket = aws_s3_bucket.log_analysis.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# CloudWatch Logs Subscription Filter for real-time processing
resource "aws_lambda_permission" "allow_cloudwatch" {
  statement_id  = "AllowExecutionFromCloudWatch"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.log_processor.function_name
  principal     = "logs.amazonaws.com"
  source_arn    = "${aws_cloudwatch_log_group.application_logs.arn}:*"
}

resource "aws_lambda_function" "log_processor" {
  filename         = "log_processor.zip"
  function_name    = "fhir-bridge-log-processor"
  role            = aws_iam_role.log_processor.arn
  handler         = "index.handler"
  runtime         = "python3.9"
  timeout         = 60

  environment {
    variables = {
      LOG_ANALYSIS_BUCKET = aws_s3_bucket.log_analysis.bucket
      ENVIRONMENT         = var.environment
    }
  }

  tags = {
    Name        = "fhir-bridge-log-processor"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# CloudWatch Logs Insights Scheduled Query
resource "aws_cloudwatch_query_definition" "daily_summary" {
  name = "FHIRBridge-Daily-Summary"

  log_group_names = [
    aws_cloudwatch_log_group.application_logs.name,
    aws_cloudwatch_log_group.security_logs.name,
    aws_cloudwatch_log_group.audit_logs.name
  ]

  query_string = <<EOF
fields @timestamp, level, message, userId, requestId
| stats count() by level, bin(1d)
| sort level, @timestamp desc
EOF
}