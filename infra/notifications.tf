# SNS topics for alerting and notifications
resource "aws_sns_topic" "alerts" {
  name              = "fhir-bridge-alerts"
  kms_master_key_id = aws_kms_key.secrets.id

  tags = {
    Name        = "fhir-bridge-alerts"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_sns_topic" "security_alerts" {
  name              = "fhir-bridge-security-alerts"
  kms_master_key_id = aws_kms_key.secrets.id

  tags = {
    Name        = "fhir-bridge-security-alerts"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_sns_topic" "compliance_alerts" {
  name              = "fhir-bridge-compliance-alerts"
  kms_master_key_id = aws_kms_key.secrets.id

  tags = {
    Name        = "fhir-bridge-compliance-alerts"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# CloudWatch Alarms for SNS notifications
resource "aws_cloudwatch_metric_alarm" "ecs_service_down" {
  alarm_name          = "fhir-bridge-service-down"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "HealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "300"
  statistic           = "Average"
  threshold           = "1"
  alarm_description   = "This metric monitors healthy ECS tasks behind ALB"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    TargetGroup = aws_lb_target_group.fhir_bridge.arn_suffix
    LoadBalancer = aws_lb.main.arn_suffix
  }
}

resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name          = "fhir-bridge-rds-cpu-high"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors RDS CPU utilization"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgresql.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "rds_storage_low" {
  alarm_name          = "fhir-bridge-rds-storage-low"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "1073741824"  # 1GB in bytes
  alarm_description   = "This metric monitors RDS free storage space"
  alarm_actions       = [aws_sns_topic.alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.postgresql.identifier
  }
}

resource "aws_cloudwatch_metric_alarm" "waf_blocked_requests" {
  alarm_name          = "fhir-bridge-waf-blocked-requests"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "BlockedRequests"
  namespace           = "AWS/WAFV2"
  period              = "300"
  statistic           = "Sum"
  threshold           = "100"
  alarm_description   = "This metric monitors WAF blocked requests"
  alarm_actions       = [aws_sns_topic.security_alerts.arn]

  dimensions = {
    Rule = "ALL"
    WebACL = aws_wafv2_web_acl.main.name
  }
}

resource "aws_cloudwatch_metric_alarm" "config_compliance_violation" {
  alarm_name          = "fhir-bridge-config-compliance-violation"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "NumberOfNonCompliantResources"
  namespace           = "AWS/Config"
  period              = "3600"
  statistic           = "Maximum"
  threshold           = "0"
  alarm_description   = "This metric monitors AWS Config compliance violations"
  alarm_actions       = [aws_sns_topic.compliance_alerts.arn]

  dimensions = {
    RuleName = "s3-bucket-public-read-prohibited"
  }
}

# IAM role for CloudWatch to publish to SNS
resource "aws_iam_role" "cloudwatch_sns" {
  name = "fhir-bridge-cloudwatch-sns-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "cloudwatch.amazonaws.com"
        }
      }
    ]
  })

  tags = {
    Name = "fhir-bridge-cloudwatch-sns-role"
  }
}

resource "aws_iam_role_policy" "cloudwatch_sns_policy" {
  name = "cloudwatch-sns-policy"
  role = aws_iam_role.cloudwatch_sns.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = [
          aws_sns_topic.alerts.arn,
          aws_sns_topic.security_alerts.arn,
          aws_sns_topic.compliance_alerts.arn
        ]
      }
    ]
  })
}

# SNS topic subscriptions (example - replace with actual email/phone)
resource "aws_sns_topic_subscription" "alerts_email" {
  topic_arn = aws_sns_topic.alerts.arn
  protocol  = "email"
  endpoint  = "alerts@tefca.com"
}

resource "aws_sns_topic_subscription" "security_alerts_email" {
  topic_arn = aws_sns_topic.security_alerts.arn
  protocol  = "email"
  endpoint  = "security@tefca.com"
}

resource "aws_sns_topic_subscription" "compliance_alerts_email" {
  topic_arn = aws_sns_topic.compliance_alerts.arn
  protocol  = "email"
  endpoint  = "compliance@tefca.com"
}