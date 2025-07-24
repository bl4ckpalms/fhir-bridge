# CloudWatch Dashboard for FHIR Bridge Application
resource "aws_cloudwatch_dashboard" "fhir_bridge_dashboard" {
  dashboard_name = "fhir-bridge-main-dashboard"
  
  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/ECS", "CPUUtilization", "ServiceName", aws_ecs_service.fhir_bridge.name, "ClusterName", aws_ecs_cluster.fhir_bridge.name],
            [".", "MemoryUtilization", ".", ".", ".", "."]
          ]
          period = 300
          stat   = "Average"
          region = var.aws_region
          title  = "ECS Service CPU and Memory Utilization"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        x      = 12
        properties = {
          metrics = [
            ["AWS/ApplicationELB", "RequestCount", "LoadBalancer", aws_lb.fhir_bridge.arn_suffix],
            [".", "HTTPCode_Target_2XX_Count", ".", "."],
            [".", "HTTPCode_Target_4XX_Count", ".", "."],
            [".", "HTTPCode_Target_5XX_Count", ".", "."]
          ]
          period = 300
          stat   = "Sum"
          region = var.aws_region
          title  = "Application Load Balancer Metrics"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        y      = 6
        properties = {
          metrics = [
            ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", aws_db_instance.fhir_bridge.id],
            [".", "DatabaseConnections", ".", "."],
            [".", "FreeableMemory", ".", "."]
          ]
          period = 300
          stat   = "Average"
          region = var.aws_region
          title  = "RDS Database Metrics"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        x      = 12
        y      = 6
        properties = {
          metrics = [
            ["AWS/ElastiCache", "CPUUtilization", "CacheClusterId", aws_elasticache_cluster.redis.cluster_id],
            [".", "FreeableMemory", ".", "."],
            [".", "CacheHits", ".", "."],
            [".", "CacheMisses", ".", "."]
          ]
          period = 300
          stat   = "Average"
          region = var.aws_region
          title  = "Redis Cache Metrics"
        }
      },
      {
        type   = "log"
        width  = 24
        height = 6
        y      = 12
        properties = {
          query   = "SOURCE '/ecs/fhir-bridge-app' | fields @timestamp, level, message | sort @timestamp desc | limit 100"
          region  = var.aws_region
          title   = "Application Logs"
        }
      }
    ]
  })
}

# CloudWatch Dashboard for Security Monitoring
resource "aws_cloudwatch_dashboard" "fhir_bridge_security_dashboard" {
  dashboard_name = "fhir-bridge-security-dashboard"
  
  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "metric"
        width  = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/WAFV2", "AllowedRequests", "Rule", "ALL", "WebACL", aws_wafv2_web_acl.fhir_bridge.name],
            [".", "BlockedRequests", ".", ".", ".", "."]
          ]
          period = 300
          stat   = "Sum"
          region = var.aws_region
          title  = "WAF Security Metrics"
        }
      },
      {
        type   = "metric"
        width  = 12
        height = 6
        x      = 12
        properties = {
          metrics = [
            ["AWS/CloudTrail", "Events", "EventName", "ConsoleLogin"],
            [".", "Events", "EventName", "AssumeRole"]
          ]
          period = 300
          stat   = "Sum"
          region = var.aws_region
          title  = "Authentication Events"
        }
      },
      {
        type   = "log"
        width  = 24
        height = 12
        y      = 6
        properties = {
          query   = "SOURCE '/security/fhir-bridge' | fields @timestamp, level, message, userId, action | sort @timestamp desc | limit 200"
          region  = var.aws_region
          title   = "Security Logs"
        }
      }
    ]
  })
}