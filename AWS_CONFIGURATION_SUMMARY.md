# AWS Configuration Summary for FHIR Bridge

This document provides a comprehensive summary of all AWS-specific configurations implemented for the FHIR Bridge application.

## Overview

The FHIR Bridge application has been configured for AWS deployment with the following components:
- **RDS PostgreSQL** for primary database
- **ElastiCache Redis** for caching and session management
- **ECS Fargate** for containerized deployment
- **CloudWatch** for logging and monitoring
- **AWS Cognito** for OAuth2/JWT authentication
- **S3** for file storage and audit logs
- **AWS X-Ray** for distributed tracing

## Configuration Files Created/Updated

### 1. `application-aws.yml` (New)
Comprehensive AWS-specific configuration profile including:
- RDS PostgreSQL connection settings
- ElastiCache Redis configuration with SSL
- AWS Cognito OAuth2 integration
- CloudWatch logging and metrics
- AWS X-Ray tracing
- S3 bucket configurations
- ECS-optimized settings

### 2. `application-prod.yml` (Updated)
Enhanced production configuration with:
- AWS resource references
- CloudWatch logging integration
- AWS Cognito JWT configuration
- ECS environment variable mappings
- HIPAA-compliant audit log retention (7 years)

### 3. `ecs-environment-mapping.md` (New)
Complete mapping of all environment variables required for ECS deployment, including:
- Database connection parameters
- Redis configuration
- OAuth2/JWT settings
- CloudWatch configuration
- S3 bucket references
- Security secrets mapping

## Database Configuration (RDS)

### Connection String
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DATABASE_HOST}:${DATABASE_PORT:5432}/${DATABASE_NAME:fhirbridge}
```

### Environment Variables
- `DATABASE_HOST`: RDS endpoint (e.g., `tefca-fhir-bridge-db.abc123xyz.us-east-1.rds.amazonaws.com`)
- `DATABASE_PORT`: `5432`
- `DATABASE_NAME`: `fhirbridge`
- `DATABASE_USERNAME`: Retrieved from AWS Secrets Manager
- `DATABASE_PASSWORD`: Retrieved from AWS Secrets Manager

### Connection Pool Settings
- Maximum pool size: 50
- Minimum idle: 10
- Connection timeout: 30s
- Idle timeout: 10min
- Max lifetime: 30min

## Redis Configuration (ElastiCache)

### Connection Settings
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      ssl:
        enabled: true
```

### Environment Variables
- `REDIS_HOST`: ElastiCache endpoint (e.g., `fhir-bridge-redis.abc123.cache.amazonaws.com`)
- `REDIS_PORT`: `6379`
- `REDIS_PASSWORD`: Retrieved from AWS Secrets Manager

## OAuth2/JWT Configuration (AWS Cognito)

### JWT Issuer Configuration
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://cognito-idp.${AWS_REGION:us-east-1}.amazonaws.com/${COGNITO_USER_POOL_ID}
```

### Environment Variables
- `JWT_ISSUER_URI`: Cognito issuer URI
- `JWT_JWK_SET_URI`: Cognito JWKS endpoint
- `JWT_AUDIENCES`: `fhir-bridge-api`
- `COGNITO_USER_POOL_ID`: AWS Cognito user pool ID
- `COGNITO_DOMAIN`: Cognito domain prefix

## CloudWatch Integration

### Logging Configuration
```yaml
logging:
  group: ${CLOUDWATCH_LOG_GROUP:/ecs/fhir-bridge}
  stream-name: ${CLOUDWATCH_LOG_STREAM:ecs-fhir-bridge}
  region: ${AWS_REGION:us-east-1}
```

### Metrics Configuration
```yaml
management:
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: fhir-bridge
        step: 60s
```

### Log Retention
- Application logs: 90 days (HIPAA compliant)
- Security logs: 365 days
- Audit logs: 2555 days (7 years for HIPAA compliance)

## S3 Configuration

### Buckets
- **Audit Logs**: `${S3_AUDIT_LOGS_BUCKET}`
- **Backups**: `${S3_BACKUPS_BUCKET}`
- **Artifacts**: `${S3_ARTIFACTS_BUCKET}`

### Encryption
- KMS encryption enabled for all buckets
- Server-side encryption with AWS KMS

## ECS Task Definition Environment Variables

### Required Environment Variables
```json
[
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
    "name": "AWS_REGION",
    "value": "us-east-1"
  }
]
```

### Required Secrets
```json
[
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
  }
]
```

## Security Configuration

### HIPAA Compliance Features
- 7-year audit log retention
- Encrypted data at rest (RDS, S3, ElastiCache)
- Encrypted data in transit (SSL/TLS)
- AWS Secrets Manager for sensitive data
- VPC isolation with private subnets
- Security groups with least privilege access

### Network Security
- Private subnets for application and database tiers
- NAT Gateway for outbound internet access
- Security groups restricting access by service
- VPC Flow Logs for network monitoring

## Monitoring and Observability

### Health Checks
- `/actuator/health` - General health check
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe

### Metrics
- CloudWatch custom metrics
- Prometheus metrics endpoint
- Application performance metrics
- Database connection pool metrics

### Tracing
- AWS X-Ray integration enabled
- Distributed tracing across services
- Performance bottleneck identification

## Deployment Checklist

### Pre-deployment
- [ ] AWS Secrets Manager populated with all required secrets
- [ ] S3 buckets created with appropriate IAM policies
- [ ] RDS instance provisioned and accessible
- [ ] ElastiCache Redis cluster provisioned
- [ ] Cognito user pool configured
- [ ] ECS cluster and task definition created

### Environment Variables Validation
- [ ] All required environment variables are set
- [ ] Secrets are properly referenced in task definition
- [ ] Region-specific variables are correctly configured
- [ ] CloudWatch log groups are created

### Security Validation
- [ ] IAM roles and policies are properly configured
- [ ] Security groups allow only necessary traffic
- [ ] Encryption is enabled for all data stores
- [ ] Secrets are not exposed in environment variables

## Next Steps

1. **Deploy Infrastructure**: Run Terraform to create all AWS resources
2. **Configure Secrets**: Populate AWS Secrets Manager with actual values
3. **Build and Push Image**: Build Docker image and push to ECR
4. **Deploy Application**: Deploy ECS service with updated task definition
5. **Validate Deployment**: Verify all components are working correctly
6. **Configure Monitoring**: Set up CloudWatch alarms and dashboards
7. **Security Review**: Conduct final security review and penetration testing