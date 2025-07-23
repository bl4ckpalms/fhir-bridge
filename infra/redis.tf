# ElastiCache Redis for consent and performance caching
resource "aws_elasticache_subnet_group" "redis" {
  name       = "fhir-bridge-redis-subnet-group"
  subnet_ids = [aws_subnet.private_db_1.id, aws_subnet.private_db_2.id]

  tags = {
    Name        = "fhir-bridge-redis-subnet-group"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_security_group" "redis" {
  name_prefix = "fhir-bridge-redis-"
  vpc_id      = aws_vpc.main.id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
    description     = "Redis access from application"
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "fhir-bridge-redis-sg"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_elasticache_replication_group" "redis" {
  replication_group_id         = "fhir-bridge-redis"
  description                  = "Redis cluster for FHIR Bridge caching"
  
  node_type                    = "cache.t3.micro"
  port                         = 6379
  parameter_group_name         = "default.redis7"
  
  num_cache_clusters           = 1
  
  subnet_group_name            = aws_elasticache_subnet_group.redis.name
  security_group_ids           = [aws_security_group.redis.id]
  
  at_rest_encryption_enabled   = true
  transit_encryption_enabled   = true
  auth_token                  = random_password.redis_auth.result
  
  automatic_failover_enabled   = false
  multi_az_enabled            = false
  
  maintenance_window          = "sun:05:00-sun:06:00"
  snapshot_retention_limit    = 1
  snapshot_window             = "03:00-05:00"
  
  tags = {
    Name        = "fhir-bridge-redis"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "random_password" "redis_auth" {
  length  = 32
  special = false
}

# Store Redis auth token in Secrets Manager
resource "aws_secretsmanager_secret" "redis_auth" {
  name                    = "fhir-bridge/redis/auth-token"
  description             = "Redis authentication token for FHIR Bridge"
  recovery_window_in_days = 7

  tags = {
    Name        = "fhir-bridge-redis-auth"
    Environment = "development"
    Project     = "fhir-bridge"
  }
}

resource "aws_secretsmanager_secret_version" "redis_auth" {
  secret_id     = aws_secretsmanager_secret.redis_auth.id
  secret_string = random_password.redis_auth.result
}

# Outputs
output "redis_endpoint" {
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
  description = "Redis primary endpoint"
}

output "redis_port" {
  value       = aws_elasticache_replication_group.redis.port
  description = "Redis port"
}