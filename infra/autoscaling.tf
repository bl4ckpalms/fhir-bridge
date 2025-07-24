# Auto Scaling for ECS Service
resource "aws_appautoscaling_target" "ecs_target" {
  max_capacity       = 10
  min_capacity       = 2
  resource_id        = "service/${aws_ecs_cluster.main.name}/${aws_ecs_service.fhir_bridge.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

# Auto Scaling Policy based on CPU utilization
resource "aws_appautoscaling_policy" "ecs_cpu_scaling" {
  name               = "fhir-bridge-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value = 70.0
  }
}

# Auto Scaling Policy based on memory utilization
resource "aws_appautoscaling_policy" "ecs_memory_scaling" {
  name               = "fhir-bridge-memory-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageMemoryUtilization"
    }
    target_value = 80.0
  }
}

# Auto Scaling Policy based on ALB request count
resource "aws_appautoscaling_policy" "ecs_alb_scaling" {
  name               = "fhir-bridge-alb-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs_target.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs_target.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs_target.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ALBRequestCountPerTarget"
      resource_label         = "${aws_lb.main.arn_suffix}/${aws_lb_target_group.fhir_bridge.arn_suffix}"
    }
    target_value = 1000.0
  }
}

# CloudWatch Alarms for Auto Scaling
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

  dimensions = {
    ServiceName = aws_ecs_service.fhir_bridge.name
    ClusterName = aws_ecs_cluster.main.name
  }
}

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

  dimensions = {
    ServiceName = aws_ecs_service.fhir_bridge.name
    ClusterName = aws_ecs_cluster.main.name
  }
}