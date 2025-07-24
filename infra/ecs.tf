# ECS Cluster for running the FHIR Bridge application
resource "aws_ecs_cluster" "main" {
  name = "fhir-bridge-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name        = "fhir-bridge-cluster"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "fhir_bridge" {
  family                   = "fhir-bridge"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([
    {
      name  = "fhir-bridge"
      image = "your-account.dkr.ecr.us-east-1.amazonaws.com/fhir-bridge:latest"
      
      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = "aws"
        },
        {
          name  = "DATABASE_HOST"
          value = aws_db_instance.postgresql.endpoint
        },
        {
          name  = "DATABASE_PORT"
          value = tostring(aws_db_instance.postgresql.port)
        },
        {
          name  = "DATABASE_NAME"
          value = aws_db_instance.postgresql.db_name
        },
        {
          name  = "REDIS_HOST"
          value = aws_elasticache_replication_group.redis.primary_endpoint_address
        },
        {
          name  = "REDIS_PORT"
          value = tostring(aws_elasticache_replication_group.redis.port)
        },
        {
          name  = "S3_AUDIT_LOGS_BUCKET"
          value = aws_s3_bucket.audit_logs.bucket
        },
        {
          name  = "S3_BACKUPS_BUCKET"
          value = aws_s3_bucket.backups.bucket
        },
        {
          name  = "S3_ARTIFACTS_BUCKET"
          value = aws_s3_bucket.artifacts.bucket
        },
        {
          name  = "AWS_REGION"
          value = "us-east-1"
        }
      ]

      secrets = [
        {
          name      = "DATABASE_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.db_credentials.arn}:username::"
        },
        {
          name      = "DATABASE_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.db_credentials.arn}:password::"
        },
        {
          name      = "REDIS_PASSWORD"
          valueFrom = aws_secretsmanager_secret.redis_auth.arn
        },

      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.fhir_bridge.name
          "awslogs-region"        = "us-east-1"
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  tags = {
    Name        = "fhir-bridge-task"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# CloudWatch Log Group
resource "aws_cloudwatch_log_group" "fhir_bridge" {
  name              = "/ecs/fhir-bridge"
  retention_in_days = 7

  tags = {
    Name        = "fhir-bridge-logs"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# ECS Service
resource "aws_ecs_service" "fhir_bridge" {
  name            = "fhir-bridge-service"
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.fhir_bridge.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = [aws_subnet.private_app_1.id, aws_subnet.private_app_2.id]
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.fhir_bridge.arn
    container_name   = "fhir-bridge"
    container_port   = 8080
  }

  depends_on = [aws_lb_listener.fhir_bridge]

  tags = {
    Name        = "fhir-bridge-service"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# Target Group for ALB
resource "aws_lb_target_group" "fhir_bridge" {
  name        = "fhir-bridge-tg"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"

  health_check {
    enabled             = true
    healthy_threshold   = 2
    unhealthy_threshold = 2
    timeout             = 5
    interval            = 30
    path                = "/actuator/health"
    matcher             = "200"
    port                = "traffic-port"
    protocol            = "HTTP"
  }

  tags = {
    Name        = "fhir-bridge-target-group"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

# ALB Listener
resource "aws_lb_listener" "fhir_bridge" {
  load_balancer_arn = aws_lb.main.arn
  port              = "80"
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.fhir_bridge.arn
  }
}