# FHIR Bridge Environment Configuration Guide

## Overview

This document provides comprehensive configuration requirements for all FHIR Bridge environments, including local development, staging, and production deployments.

## 📋 Configuration Files Structure

```
fhir-bridge/
├── src/main/resources/
│   ├── application.yml              # Core configuration
│   ├── application-dev.yml          # Development overrides
│   ├── application-staging.yml      # Staging environment
│   ├── application-prod.yml         # Production settings
│   ├── application-test.yml         # Testing configuration
│   └── logback-spring.xml          # Logging configuration
├── infra/terraform/
│   ├── terraform.tfvars            # Terraform variables
│   ├── dev.tfvars                  # Development variables
│   ├── staging.tfvars              # Staging variables
│   └── production.tfvars           # Production variables
└── docker-compose*.yml             # Docker configurations
```

## 🔧 Environment-Specific Configuration

### 1. Local Development (dev)

#### Application Configuration (`application-dev.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/fhir_bridge_dev
    username: fhir_user
    password: fhir_password
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  
  redis:
    host: localhost
    port: 6379
    password: 
    timeout: 2000ms
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/auth/realms/fhir-bridge
  
logging:
  level:
    com.bridge: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG

server:
  port: 8080

# Development-specific settings
app:
  cors:
    allowed-origins:
      - http://localhost:3000
      - http://localhost:8080
      - http://127.0.0.1:3000
  
  rate-limit:
    enabled: false
    requests-per-minute: 10000
  
  security:
    require-ssl: false
    enable-csrf: false
```

#### Docker Compose (`docker-compose.override.yml`)
```yaml
version: '3.8'
services:
  fhir-bridge:
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC
    ports:
      - "5005:5005"  # Debug port
    volumes:
      - ./src:/app/src:ro
      - ./logs:/app/logs
```

#### Environment Variables (.env.dev)
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fhir_bridge_dev
DB_USERNAME=fhir_user
DB_PASSWORD=fhir_password

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# Security
JWT_SECRET=dev-jwt-secret-not-for-production
JWT_ISSUER_URI=http://localhost:8080/auth/realms/fhir-bridge
JWT_JWK_SET_URI=http://localhost:8080/auth/realms/fhir-bridge/.well-known/jwks.json

# Development
DEBUG=true
LOG_LEVEL=DEBUG
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
```

### 2. Staging Environment (staging)

#### Application Configuration (`application-staging.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: true
    timeout: 5000ms
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}

logging:
  level:
    com.bridge: INFO
    org.springframework.security: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"

server:
  port: 8080
  ssl:
    enabled: ${SSL_ENABLED:false}

# Staging-specific settings
app:
  cors:
    allowed-origins:
      - https://staging.yourcompany.com
      - https://app-staging.yourcompany.com
  
  rate-limit:
    enabled: true
    requests-per-minute: 1000
    burst-capacity: 200
  
  security:
    require-ssl: true
    enable-csrf: true
  
  monitoring:
    prometheus:
      enabled: true
    health-check:
      detailed: true
```

#### Terraform Variables (`staging.tfvars`)
```hcl
environment = "staging"
region      = "us-east-1"

# ECS Configuration
ecs_task_cpu    = "1024"
ecs_task_memory = "2048"
desired_count   = 2

# RDS Configuration
db_instance_class      = "db.t3.medium"
db_allocated_storage  = 100
db_max_allocated_storage = 200
db_backup_retention_period = 7

# Redis Configuration
redis_node_type = "cache.t3.micro"

# Security
ssl_certificate_arn = "arn:aws:acm:us-east-1:123456789012:certificate/xxx"
```

#### Environment Variables (AWS SSM)
```bash
# Store in AWS Systems Manager Parameter Store
aws ssm put-parameter \
  --name "/fhir-bridge/staging/db-password" \
  --value "secure-staging-password" \
  --type "SecureString" \
  --kms-key-id "alias/fhir-bridge-staging"

aws ssm put-parameter \
  --name "/fhir-bridge/staging/jwt-secret" \
  --value "staging-jwt-secret-256-bits" \
  --type "SecureString" \
  --kms-key-id "alias/fhir-bridge-staging"
```

### 3. Production Environment (prod)

#### Application Configuration (`application-prod.yml`)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
  
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: false
        show_sql: false
        jdbc:
          batch_size: 25
        cache:
          use_second_level_cache: true
          use_query_cache: true
  
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    password: ${REDIS_PASSWORD}
    ssl: true
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
        min-idle: 5
  
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${JWT_ISSUER_URI}
          jwk-set-uri: ${JWT_JWK_SET_URI}

logging:
  level:
    com.bridge: WARN
    org.springframework.security: WARN
    org.hibernate.SQL: ERROR
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: /app/logs/fhir-bridge.log
    max-size: 100MB
    max-history: 30

server:
  port: 8080
  ssl:
    enabled: true
    key-store: /app/ssl/keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

# Production-specific settings
app:
  cors:
    allowed-origins:
      - https://yourcompany.com
      - https://app.yourcompany.com
  
  rate-limit:
    enabled: true
    requests-per-minute: 2000
    burst-capacity: 500
  
  security:
    require-ssl: true
    enable-csrf: true
    session-timeout: 30m
  
  monitoring:
    prometheus:
      enabled: true
    health-check:
      detailed: false
    metrics:
      export:
        cloudwatch:
          enabled: true
          namespace: FHIRBridge/Production
  
  cache:
    ttl:
      consent: 3600000  # 1 hour
      fhir-resources: 1800000  # 30 minutes
```

#### Terraform Variables (`production.tfvars`)
```hcl
environment = "production"
region      = "us-east-1"

# ECS Configuration
ecs_task_cpu    = "2048"
ecs_task_memory = "4096"
desired_count   = 3

# RDS Configuration
db_instance_class      = "db.r5.xlarge"
db_allocated_storage  = 500
db_max_allocated_storage = 1000
db_backup_retention_period = 30
db_multi_az = true

# Redis Configuration
redis_node_type = "cache.r5.large"
redis_num_cache_clusters = 3

# Security
ssl_certificate_arn = "arn:aws:acm:us-east-1:123456789012:certificate/xxx"
```

### 4. Testing Environment (test)

#### Application Configuration (`application-test.yml`)
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  
  redis:
    host: localhost
    port: 6379
    database: 15  # Use separate database for tests
  
  security:
    oauth2:
      resourceserver:
        jwt:
          public-key-location: classpath:test-jwt-public-key.pem

logging:
  level:
    com.bridge: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE

server:
  port: 0  # Random port for testing

# Test-specific settings
app:
  cors:
    allowed-origins:
      - "*"
  
  rate-limit:
    enabled: false
  
  security:
    require-ssl: false
    enable-csrf: false
  
  test:
    generate-test-data: true
    mock-external-services: true
```

## 🔐 Security Configuration

### JWT Configuration
```yaml
# JWT Settings
jwt:
  secret: ${JWT_SECRET}  # 256-bit secret key
  issuer: ${JWT_ISSUER_URI}
  audience: fhir-bridge
  expiration:
    access-token: 900    # 15 minutes
    refresh-token: 604800  # 7 days
  refresh-token:
    rotation-enabled: true
    reuse-detection: true
```

### SSL/TLS Configuration
```yaml
# SSL Configuration
ssl:
  enabled: ${SSL_ENABLED:true}
  protocol: TLS
  enabled-protocols: TLSv1.2,TLSv1.3
  ciphers:
    - TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
    - TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256
    - TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384
    - TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256
```

### Database Encryption
```yaml
# Database Security
database:
  encryption:
    enabled: true
    algorithm: AES-256
    key-rotation: true
  ssl:
    enabled: true
    mode: REQUIRE
    verify-server-certificate: true
```

## 📊 Monitoring Configuration

### Metrics Configuration
```yaml
# Micrometer Metrics
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
      cloudwatch:
        enabled: ${CLOUDWATCH_ENABLED:false}
        namespace: FHIRBridge
    tags:
      application: fhir-bridge
      environment: ${ENVIRONMENT:local}
    distribution:
      percentiles-histogram:
        http.server.requests: true
      slo:
        http.server.requests: 100ms,500ms,1s,2s
```

### Health Checks
```yaml
# Health Check Configuration
health:
  checks:
    database:
      enabled: true
      query: "SELECT 1"
    redis:
      enabled: true
    disk-space:
      enabled: true
      threshold: 1GB
    external-services:
      enabled: true
      services:
        - name: fhir-server
          url: ${FHIR_SERVER_BASE_URL}/metadata
          timeout: 5s
```

## 🐳 Docker Configuration

### Development Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy application files
COPY target/fhir-bridge-*.jar app.jar
COPY src/main/resources/application-dev.yml application.yml

# Environment variables
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xmx1g -Xms512m -XX:+UseG1GC"

# Expose ports
EXPOSE 8080 8081 5005

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Production Dockerfile
```dockerfile
FROM openjdk:17-jre-alpine

# Install security updates
RUN apk update && apk upgrade && apk add --no-cache curl

# Create non-root user
RUN addgroup -g 1000 fhir && adduser -D -s /bin/sh -u 1000 -G fhir fhir

WORKDIR /app

# Copy application
COPY --chown=fhir:fhir target/fhir-bridge-*.jar app.jar
COPY --chown=fhir:fhir src/main/resources/application-prod.yml application.yml

# Create directories
RUN mkdir -p /app/logs /app/ssl && chown -R fhir:fhir /app

# Switch to non-root user
USER fhir

# Environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:+UseContainerSupport"

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 🔍 Configuration Validation

### Environment Validation Script
```bash
#!/bin/bash
# validate-config.sh

echo "🔍 Validating FHIR Bridge Configuration..."

# Check required environment variables
required_vars=(
  "DB_HOST"
  "DB_NAME"
  "DB_USERNAME"
  "DB_PASSWORD"
  "REDIS_HOST"
  "JWT_SECRET"
  "JWT_ISSUER_URI"
)

missing_vars=()
for var in "${required_vars[@]}"; do
  if [[ -z "${!var}" ]]; then
    missing_vars+=("$var")
  fi
done

if [[ ${#missing_vars[@]} -gt 0 ]]; then
  echo "❌ Missing required environment variables:"
  printf ' - %s\n' "${missing_vars[@]}"
  exit 1
fi

# Validate database connection
echo "🗄️  Testing database connection..."
if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -t 10; then
  echo "❌ Database connection failed"
  exit 1
fi

# Validate Redis connection
echo "🔄 Testing Redis connection..."
if ! redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping | grep -q PONG; then
  echo "❌ Redis connection failed"
  exit 1
fi

# Validate JWT configuration
echo "🔐 Testing JWT configuration..."
if [[ ${#JWT_SECRET} -lt 32 ]]; then
  echo "❌ JWT secret must be at least 256 bits (32 bytes)"
  exit 1
fi

echo "✅ Configuration validation passed!"
```

## 🚀 Configuration Deployment

### Environment-Specific Deployment

#### Development
```bash
# Local development
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run

# Docker development
docker-compose -f docker-compose.yml -f docker-compose.override.yml up
```

#### Staging
```bash
# Deploy to staging
aws ssm put-parameter \
  --name "/fhir-bridge/staging/config" \
  --type "SecureString" \
  --value file://staging-config.json

# Update ECS service
aws ecs update-service \
  --cluster fhir-bridge-staging \
  --service fhir-bridge-service \
  --force-new-deployment
```

#### Production
```bash
# Deploy to production
aws ssm put-parameter \
  --name "/fhir-bridge/production/config" \
  --type "SecureString" \
  --value file://production-config.json

# Blue-green deployment
aws ecs create-task-set \
  --cluster fhir-bridge-production \
  --service fhir-bridge-service \
  --task-definition fhir-bridge:2 \
  --launch-type FARGATE
```

## 📋 Configuration Checklist

### Pre-deployment Checklist
- [ ] All required environment variables configured
- [ ] Database connectivity verified
- [ ] Redis connectivity verified
- [ ] SSL certificates configured (production)
- [ ] Security groups configured
- [ ] IAM roles and policies configured
- [ ] Monitoring and alerting configured
- [ ] Backup strategy implemented
- [ ] Disaster recovery tested

### Post-deployment Validation
- [ ] Health checks passing
- [ ] Metrics collection working
- [ ] Log aggregation configured
- [ ] Security scanning completed
- [ ] Performance testing completed
- [ ] Load testing completed
- [ ] Backup restoration tested