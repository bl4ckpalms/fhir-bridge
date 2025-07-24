# ECS Environment Variables Mapping

This document provides a comprehensive mapping of environment variables required for AWS ECS Fargate deployment.

## Database Configuration (RDS PostgreSQL)

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `DATABASE_HOST` | RDS PostgreSQL endpoint | `tefca-fhir-bridge-db.abc123xyz.us-east-1.rds.amazonaws.com` | `aws_db_instance.postgresql.endpoint` |
| `DATABASE_PORT` | PostgreSQL port | `5432` | Static/Output |
| `DATABASE_NAME` | Database name | `fhirbridge` | `aws_db_instance.postgresql.db_name` |
| `DATABASE_USERNAME` | Database username | `fhirbridge_admin` | AWS Secrets Manager |
| `DATABASE_PASSWORD` | Database password | `***` | AWS Secrets Manager |
| `DB_MAX_POOL_SIZE` | HikariCP max pool size | `50` | Configuration |
| `DB_MIN_IDLE` | HikariCP min idle connections | `10` | Configuration |

## Redis Configuration (ElastiCache)

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `REDIS_HOST` | ElastiCache Redis endpoint | `fhir-bridge-redis.abc123.cache.amazonaws.com` | `aws_elasticache_replication_group.redis.primary_endpoint_address` |
| `REDIS_PORT` | Redis port | `6379` | Static |
| `REDIS_PASSWORD` | Redis auth token | `***` | AWS Secrets Manager |
| `REDIS_MAX_ACTIVE` | Max active connections | `30` | Configuration |
| `REDIS_MAX_IDLE` | Max idle connections | `15` | Configuration |
| `REDIS_MIN_IDLE` | Min idle connections | `5` | Configuration |
| `REDIS_MAX_WAIT` | Max wait time | `5000ms` | Configuration |

## AWS Cognito OAuth2 Configuration

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `JWT_ISSUER_URI` | Cognito issuer URI | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_abc123` | Configuration |
| `JWT_JWK_SET_URI` | Cognito JWKS URI | `https://cognito-idp.us-east-1.amazonaws.com/us-east-1_abc123/.well-known/jwks.json` | Configuration |
| `JWT_AUDIENCES` | JWT audience | `fhir-bridge-api` | Configuration |
| `COGNITO_USER_POOL_ID` | Cognito user pool ID | `us-east-1_abc123` | Output |
| `COGNITO_DOMAIN` | Cognito domain prefix | `fhir-bridge-auth` | Configuration |
| `OAUTH2_CLIENT_ID` | OAuth2 client ID | `***` | AWS Secrets Manager |
| `OAUTH2_CLIENT_SECRET` | OAuth2 client secret | `***` | AWS Secrets Manager |

## CloudWatch Configuration

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `CLOUDWATCH_LOG_GROUP` | CloudWatch log group | `/ecs/fhir-bridge` | `aws_cloudwatch_log_group.fhir_bridge.name` |
| `CLOUDWATCH_LOG_STREAM` | CloudWatch log stream | `ecs-fhir-bridge` | Configuration |
| `CLOUDWATCH_LOG_RETENTION` | Log retention days | `90` | Configuration |
| `CLOUDWATCH_METRICS_ENABLED` | Enable CloudWatch metrics | `true` | Configuration |
| `CLOUDWATCH_NAMESPACE` | CloudWatch namespace | `fhir-bridge` | Configuration |
| `AWS_REGION` | AWS region | `us-east-1` | Configuration |

## S3 Configuration

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `S3_AUDIT_LOGS_BUCKET` | S3 bucket for audit logs | `tefca-fhir-bridge-audit-logs-123456789` | `aws_s3_bucket.audit_logs.bucket` |
| `S3_BACKUPS_BUCKET` | S3 bucket for backups | `tefca-fhir-bridge-backups-123456789` | `aws_s3_bucket.backups.bucket` |
| `S3_ARTIFACTS_BUCKET` | S3 bucket for artifacts | `tefca-fhir-bridge-artifacts-123456789` | `aws_s3_bucket.artifacts.bucket` |
| `S3_KMS_KEY_ID` | KMS key for S3 encryption | `arn:aws:kms:us-east-1:123456789:key/abc123` | `aws_kms_key.s3.arn` |

## AWS X-Ray Configuration

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `AWS_XRAY_ENABLED` | Enable AWS X-Ray tracing | `true` | Configuration |
| `AWS_XRAY_SAMPLING_RATE` | X-Ray sampling rate | `0.1` | Configuration |

## Application Configuration

| Environment Variable | Description | Example Value | Source |
|---------------------|-------------|---------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod,aws` | Configuration |
| `SERVER_PORT` | Server port | `8080` | Configuration |
| `ENVIRONMENT` | Environment name | `production` | Configuration |
| `FHIR_SERVER_BASE_URL` | FHIR server base URL | `https://hapi.fhir.org/baseR4` | Configuration |
| `CORS_ALLOWED_ORIGINS` | CORS allowed origins | `https://app.fhirbridge.com` | Configuration |
| `RATE_LIMIT_RPM` | Rate limit per minute | `1000` | Configuration |
| `RATE_LIMIT_BURST` | Rate limit burst capacity | `100` | Configuration |
| `VIRTUAL_THREADS_ENABLED` | Enable virtual threads | `true` | Configuration |

## ECS Task Definition Environment Variables

```json
"environment": [
  {
    "name": "SPRING_PROFILES_ACTIVE",
    "value": "prod,aws"
  },
  {
    "name": "DATABASE_HOST",
    "value": "${aws_db_instance.postgresql.endpoint}"
  },
  {
    "name": "DATABASE_PORT",
    "value": "5432"
  },
  {
    "name": "DATABASE_NAME",
    "value": "${aws_db_instance.postgresql.db_name}"
  },
  {
    "name": "REDIS_HOST",
    "value": "${aws_elasticache_replication_group.redis.primary_endpoint_address}"
  },
  {
    "name": "REDIS_PORT",
    "value": "6379"
  },
  {
    "name": "S3_AUDIT_LOGS_BUCKET",
    "value": "${aws_s3_bucket.audit_logs.bucket}"
  },
  {
    "name": "S3_BACKUPS_BUCKET",
    "value": "${aws_s3_bucket.backups.bucket}"
  },
  {
    "name": "S3_ARTIFACTS_BUCKET",
    "value": "${aws_s3_bucket.artifacts.bucket}"
  },
  {
    "name": "AWS_REGION",
    "value": "us-east-1"
  },
  {
    "name": "CLOUDWATCH_LOG_GROUP",
    "value": "${aws_cloudwatch_log_group.fhir_bridge.name}"
  },
  {
    "name": "CLOUDWATCH_LOG_RETENTION",
    "value": "90"
  }
]
```

## ECS Task Definition Secrets

```json
"secrets": [
  {
    "name": "DATABASE_USERNAME",
    "valueFrom": "${aws_secretsmanager_secret.db_credentials.arn}:username::"
  },
  {
    "name": "DATABASE_PASSWORD",
    "valueFrom": "${aws_secretsmanager_secret.db_credentials.arn}:password::"
  },
  {
    "name": "REDIS_PASSWORD",
    "valueFrom": "${aws_secretsmanager_secret.redis_auth.arn}"
  },
  {
    "name": "JWT_SECRET",
    "valueFrom": "${aws_secretsmanager_secret.jwt_secret.arn}"
  },
  {
    "name": "OAUTH2_CLIENT_ID",
    "valueFrom": "${aws_secretsmanager_secret.oauth2_credentials.arn}:client-id::"
  },
  {
    "name": "OAUTH2_CLIENT_SECRET",
    "valueFrom": "${aws_secretsmanager_secret.oauth2_credentials.arn}:client-secret::"
  }
]
```

## Health Check Configuration

The application exposes health check endpoints at:
- `/actuator/health` - General health check
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe

## Notes

1. All sensitive values (passwords, secrets) should be stored in AWS Secrets Manager
2. Environment variables are automatically injected by ECS from the task definition
3. CloudWatch log group is automatically created by ECS
4. S3 buckets should have appropriate IAM policies for ECS task role
5. All AWS resources should be in the same region for optimal performance