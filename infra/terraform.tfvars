# AWS Configuration
aws_region = "us-east-1"
environment = "test"

# Application Configuration
app_name = "fhir-bridge"
app_version = "1.0.0"

# VPC Configuration
vpc_cidr = "10.0.0.0/16"
availability_zones = ["us-east-1a", "us-east-1b"]

# Database Configuration
db_instance_class = "db.t3.micro"
db_allocated_storage = 20
db_backup_retention_period = 7

# ECS Configuration
ecs_task_cpu = "512"
ecs_task_memory = "1024"
ecs_desired_count = 2

# Security Configuration
enable_waf = true
enable_cloudtrail = true
enable_vpc_flow_logs = true

# Monitoring Configuration
enable_monitoring = true
log_retention_days = 90
security_log_retention_days = 365
audit_log_retention_days = 2557
alert_email = "admin@example.com"
scaling_notification_email = "devops@example.com"
enable_autoscaling = true

# Backup Configuration
backup_schedule = "cron(0 2 * * ? *)"
backup_retention_days = 7