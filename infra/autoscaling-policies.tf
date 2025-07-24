# Application Auto Scaling for ECS Service
resource "aws_appautoscaling_target" "fhir_bridge" {
  service_namespace  = "ecs"
  resource_id        = "service/${aws_ecs_cluster.fhir_bridge.name}/${aws_ecs_service.fhir_bridge.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  min_capacity       = 2
  max_capacity       = 10

  depends_on = [aws_ecs_service.fhir_bridge]
}

# CPU-based scaling policy
resource "aws_appautoscaling_policy" "cpu_scaling" {
  name               = "fhir-bridge-cpu-scaling"
  service_namespace  = "ecs"
  resource_id        = aws_appautoscaling_target.fhir_bridge.resource_id
  scalable_dimension = aws_appautoscaling_target.fhir_bridge.scalable_dimension

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

# Memory-based scaling policy
resource "aws_appautoscaling_policy" "memory_scaling" {
  name               = "fhir-bridge-memory-scaling"
  service_namespace  = "ecs"
  resource_id        = aws_appautoscaling_target.fhir_bridge.resource_id
  scalable_dimension = aws_appautoscaling_target.fhir_bridge.scalable_dimension

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value = 80.0
  }
}

# Request count-based scaling policy
resource "aws_appautoscaling_policy" "request_scaling" {
  name               = "fhir-bridge-request-scaling"
  service_namespace  = "ecs"
  resource_id        = aws_appautoscaling_target.fhir_bridge.resource_id
  scalable_dimension = aws_appautoscaling_target.fhir_bridge.scalable_dimension

  target_tracking_scaling_policy_configuration {
    customized_metric_specification {
      metric_name = "RequestCount"
      namespace   = "AWS/ApplicationELB"
      statistic   = "Average"
      
      dimensions {
        name  = "LoadBalancer"
        value = aws_lb.fhir_bridge.arn_suffix
      }
    }
    target_value = 1000.0
  }
}

# Scheduled scaling for business hours
resource "aws_appautoscaling_scheduled_action" "business_hours_scale_up" {
  name               = "fhir-bridge-business-hours-scale-up"
  service_namespace  = "ecs"
  resource_id        = aws_appautoscaling_target.fhir_bridge.resource_id
  scalable_dimension = aws_appautoscaling_target.fhir_bridge.scalable_dimension
  schedule           = "cron(0 8 ? * MON-FRI *)"
  timezone           = "America/Chicago"

  scalable_target_action {
    min_capacity = 4
    max_capacity = 12
  }
}

resource "aws_appautoscaling_scheduled_action" "business_hours_scale_down" {
  name               = "fhir-bridge-business-hours-scale-down"
  service_namespace  = "ecs"
  resource_id        = aws_appautoscaling_target.fhir_bridge.resource_id
  scalable_dimension = aws_appautoscaling_target.fhir_bridge.scalable_dimension
  schedule           = "cron(0 18 ? * MON-FRI *)"
  timezone           = "America/Chicago"

  scalable_target_action {
    min_capacity = 2
    max_capacity = 8
  }
}

# RDS Auto Scaling (Read Replicas)
resource "aws_appautoscaling_target" "rds_read_replicas" {
  service_namespace  = "rds"
  resource_id        = "cluster:${aws_rds_cluster.fhir_bridge.id}"
  scalable_dimension = "rds:cluster:ReadReplicaCount"
  min_capacity       = 0
  max_capacity       = 3
}

resource "aws_appautoscaling_policy" "rds_cpu_scaling" {
  name               = "fhir-bridge-rds-cpu-scaling"
  service_namespace  = "rds"
  resource_id        = aws_appautoscaling_target.rds_read_replicas.resource_id
  scalable_dimension = aws_appautoscaling_target.rds_read_replicas.scalable_dimension

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "RDSReaderAverageCPUUtilization"
    }
    target_value = 75.0
  }
}

# CloudWatch Alarms for scaling events
resource "aws_cloudwatch_metric_alarm" "scale_up_alarm" {
  alarm_name          = "fhir-bridge-scale-up"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "70"
  alarm_description   = "Trigger scale up when CPU > 70%"
  alarm_actions       = [aws_appautoscaling_policy.cpu_scaling.arn]

  dimensions = {
    ServiceName = aws_ecs_service.fhir_bridge.name
    ClusterName = aws_ecs_cluster.fhir_bridge.name
  }

  tags = {
    Name        = "fhir-bridge-scale-up-alarm"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

resource "aws_cloudwatch_metric_alarm" "scale_down_alarm" {
  alarm_name          = "fhir-bridge-scale-down"
  comparison_operator = "LessThanThreshold"
  evaluation_periods  = "2"
  metric_name         = "CPUUtilization"
  namespace           = "AWS/ECS"
  period              = "300"
  statistic           = "Average"
  threshold           = "30"
  alarm_description   = "Trigger scale down when CPU < 30%"
  alarm_actions       = [aws_appautoscaling_policy.cpu_scaling.arn]

  dimensions = {
    ServiceName = aws_ecs_service.fhir_bridge.name
    ClusterName = aws_ecs_cluster.fhir_bridge.name
  }

  tags = {
    Name        = "fhir-bridge-scale-down-alarm"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

# SNS topic for scaling notifications
resource "aws_sns_topic" "scaling_notifications" {
  name = "fhir-bridge-scaling-notifications"
  
  tags = {
    Name        = "fhir-bridge-scaling-notifications"
    Environment = var.environment
    Project     = "fhir-bridge"
  }
}

resource "aws_sns_topic_subscription" "scaling_notifications_email" {
  topic_arn = aws_sns_topic.scaling_notifications.arn
  protocol  = "email"
  endpoint  = var.scaling_notification_email
}

# CloudWatch Events for scaling notifications
resource "aws_cloudwatch_event_rule" "scaling_events" {
  name        = "fhir-bridge-scaling-events"
  description = "Capture ECS service scaling events"
  
  event_pattern = jsonencode({
    source      = ["aws.ecs"]
    detail-type = ["ECS Service Action"]
    detail = {
      eventName = ["SERVICE_STEADY_STATE", "SERVICE_TASK_PLACEMENT_FAILURE"]
    }
  })
}

resource "aws_cloudwatch_event_target" "scaling_events_target" {
  rule      = aws_cloudwatch_event_rule.scaling_events.name
  target_id = "SendToSNS"
  arn       = aws_sns_topic.scaling_notifications.arn
}