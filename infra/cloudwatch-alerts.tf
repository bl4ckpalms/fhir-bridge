# SNS Topic for Alerts
resource "aws_sns_topic" "fhir_bridge_alerts" {
  name = "fhir-bridge-alerts"
  
  tags = {
    Name        = "fhir-bridge-alerts"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

resource "aws_sns_topic_subscription" "fhir_bridge_alerts_email" {
  topic_arn = aws_sns_topic.fhir_bridge_alerts.arn
  protocol  = "email"
  endpoint  = var.alert_email
}

# High CPU Utilization Alert
resource "aws_cloudwatch_metric_alarm" "high_cpu" {
  alarm_name          = "fhir-bridge-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors ECS service CPU utilization"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    ServiceName = aws_ecs_service.fhir_bridge.name
    ClusterName = aws_ecs_cluster.fhir_bridge.name
  }

  tags = {
    Name        = "fhir-bridge-high-cpu-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# High Memory Utilization Alert
resource "aws_cloudwatch_metric_alarm" "high_memory" {
  alarm_name          = "fhir-bridge-high-memory"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "MemoryUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "85"
  alarm_description   = "This metric monitors ECS service memory utilization"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    ServiceName = aws_ecs_service.fhir_bridge.name
    ClusterName = aws_ecs_cluster.fhir_bridge.name
  }

  tags = {
    Name        = "fhir-bridge-high-memory-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# High Error Rate Alert
resource "aws_cloudwatch_metric_alarm" "high_error_rate" {
  alarm_name          = "fhir-bridge-high-error-rate"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "HTTPCode_Target_5XX_Count"
  namespace           = "AWS/ApplicationELB"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "This metric monitors application 5xx errors"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    LoadBalancer = aws_lb.fhir_bridge.arn_suffix
  }

  tags = {
    Name        = "fhir-bridge-high-error-rate-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Database CPU Alert
resource "aws_cloudwatch_metric_alarm" "rds_high_cpu" {
  alarm_name          = "fhir-bridge-rds-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors RDS CPU utilization"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.fhir_bridge.id
  }

  tags = {
    Name        = "fhir-bridge-rds-high-cpu-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Database Connection Alert
resource "aws_cloudwatch_metric_alarm" "rds_high_connections" {
  alarm_name          = "fhir-bridge-rds-high-connections"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "DatabaseConnections"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Maximum"
  threshold           = "80"
  alarm_description   = "This metric monitors RDS database connections"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.fhir_bridge.id
  }

  tags = {
    Name        = "fhir-bridge-rds-connections-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Low Database Storage Alert
resource "aws_cloudwatch_metric_alarm" "rds_low_storage" {
  alarm_name          = "fhir-bridge-rds-low-storage"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "FreeStorageSpace"
  namespace           = "AWS/RDS"
  period              = "300"
  statistic           = "Average"
  threshold           = "10737418240"  # 10GB in bytes
  alarm_description   = "This metric monitors RDS free storage space"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    DBInstanceIdentifier = aws_db_instance.fhir_bridge.id
  }

  tags = {
    Name        = "fhir-bridge-rds-low-storage-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Redis Cache CPU Alert
resource "aws_cloudwatch_metric_alarm" "redis_high_cpu" {
  alarm_name          = "fhir-bridge-redis-high-cpu"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ElastiCache"
  period              = "300"
  statistic           = "Average"
  threshold           = "80"
  alarm_description   = "This metric monitors Redis CPU utilization"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    CacheClusterId = aws_elasticache_cluster.redis.cluster_id
  }

  tags = {
    Name        = "fhir-bridge-redis-high-cpu-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Log-based Error Alert
resource "aws_cloudwatch_log_metric_filter" "application_errors" {
  name           = "fhir-bridge-application-errors"
  pattern        = "[time, level=ERROR, msg]"
  log_group_name = aws_cloudwatch_log_group.application_logs.name

  metric_transformation {
    name      = "ApplicationErrors"
    namespace = "FHIRBridge/Application"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "application_errors" {
  alarm_name          = "fhir-bridge-application-errors"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "ApplicationErrors"
  namespace           = "FHIRBridge/Application"
  period              = "300"
  statistic           = "Sum"
  threshold           = "5"
  alarm_description   = "This metric monitors application error logs"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  tags = {
    Name        = "fhir-bridge-application-errors-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Security Alert for Failed Authentication
resource "aws_cloudwatch_log_metric_filter" "failed_auth" {
  name           = "fhir-bridge-failed-authentication"
  pattern        = "[time, level=WARN, msg=\"*authentication failed*\"]"
  log_group_name = aws_cloudwatch_log_group.security_logs.name

  metric_transformation {
    name      = "FailedAuthentication"
    namespace = "FHIRBridge/Security"
    value     = "1"
  }
}

resource "aws_cloudwatch_metric_alarm" "failed_auth" {
  alarm_name          = "fhir-bridge-failed-authentication"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "FailedAuthentication"
  namespace           = "FHIRBridge/Security"
  period              = "300"
  statistic           = "Sum"
  threshold           = "10"
  alarm_description   = "This metric monitors failed authentication attempts"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  tags = {
    Name        = "fhir-bridge-failed-auth-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# Health Check Failure Alert
resource "aws_cloudwatch_metric_alarm" "health_check_failures" {
  alarm_name          = "fhir-bridge-health-check-failures"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "UnHealthyHostCount"
  namespace           = "AWS/ApplicationELB"
  period              = "60"
  statistic           = "Maximum"
  threshold           = "0"
  alarm_description   = "This metric monitors health check failures"
  alarm_actions       = [aws_sns_topic.fhir_bridge_alerts.arn]

  dimensions = {
    LoadBalancer = aws_lb.fhir_bridge.arn_suffix
    TargetGroup  = aws_lb_target_group.fhir_bridge.arn_suffix
  }

  tags = {
    Name        = "fhir-bridge-health-check-alert"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}