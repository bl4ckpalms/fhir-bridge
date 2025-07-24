# FHIR Bridge - Cloud-Hosted Zero-Trust Gateway

A HIPAA-compliant, cloud-native FHIR bridge that transforms HL7 v2 messages into FHIR R4 resources with comprehensive consent management and zero-trust security architecture.

## 🚀 Quick Start

### Prerequisites
- **Docker 20.10+** and **Docker Compose 2.0+**
- **Java 17+** (for local development)
- **PostgreSQL 14+** (or use Docker setup)
- **Redis 6+** (or use Docker setup)
- **4GB+ available RAM**

### Local Development Setup

```bash
# Clone and setup
git clone <repository-url>
cd fhir-bridge

# Quick start with Docker (Linux/macOS)
./scripts/dev-setup.sh setup

# Quick start with Docker (Windows)
scripts\dev-setup.bat setup
```

The development environment includes:
- FHIR Bridge application with hot reload
- PostgreSQL database with sample data
- Redis cache with monitoring
- pgAdmin for database management
- Redis Commander for cache monitoring
- Mailhog for email testing

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Deployment Options](#deployment-options)
- [Environment Configuration](#environment-configuration)
- [Security Features](#security-features)
- [Monitoring & Observability](#monitoring--observability)
- [Testing](#testing)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)
- [Backup & Disaster Recovery](#backup--disaster-recovery)

## 🏗️ Architecture Overview

### Core Components
- **FHIR Bridge API**: RESTful API for HL7-to-FHIR transformation
- **Consent Management**: HIPAA-compliant consent verification and enforcement
- **Zero-Trust Security**: JWT-based authentication with OAuth2 integration
- **Caching Layer**: Redis for performance optimization
- **Database**: PostgreSQL with HIPAA-compliant encryption
- **Monitoring**: Comprehensive observability with Prometheus/Grafana

### Security Architecture
- **Zero-Trust Model**: Every request authenticated and authorized
- **End-to-End Encryption**: TLS 1.3 for data in transit
- **Data Encryption**: AES-256 encryption at rest
- **Audit Logging**: Comprehensive audit trail for compliance
- **Rate Limiting**: Token bucket algorithm with Redis backing

## 🚀 Deployment Options

### 1. Local Development (Docker)
```bash
# Start development environment
./scripts/dev-setup.sh start

# Access services
# - API: http://localhost:8080
# - Management: http://localhost:8081
# - pgAdmin: http://localhost:5050
# - Redis Commander: http://localhost:8082
```

### 2. Testing Environment
```bash
# Run comprehensive tests
./scripts/test-environment.sh start
./scripts/test-environment.sh test
```

### 3. Production Deployment
- **AWS ECS Fargate**: Serverless container deployment
- **Terraform Infrastructure**: Infrastructure as Code
- **Auto-scaling**: Based on CPU/memory metrics
- **Multi-AZ**: High availability across availability zones

## 🔧 Environment Configuration

### Environment Profiles
- **dev**: Local development with debug logging
- **staging**: Pre-production testing environment
- **prod**: Production with optimized settings
- **test**: Automated testing with test data

### Required Environment Variables

#### Database Configuration
```bash
# PostgreSQL
DB_HOST=your-db-host
DB_PORT=5432
DB_NAME=fhir_bridge
DB_USERNAME=fhir_user
DB_PASSWORD=secure-password

# Redis
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=secure-password
```

#### Security Configuration
```bash
# JWT/OAuth2
JWT_SECRET=your-jwt-secret-key-min-256-bits
JWT_ISSUER_URI=https://your-auth-server.com
JWT_JWK_SET_URI=https://your-auth-server.com/.well-known/jwks.json

# SSL/TLS
SSL_ENABLED=true
SSL_CERT_PATH=/path/to/cert.pem
SSL_KEY_PATH=/path/to/key.pem
```

#### FHIR Configuration
```bash
FHIR_SERVER_BASE_URL=https://your-fhir-server.com/baseR4
FHIR_VALIDATION_ENABLED=true
```

### Configuration Files
- `application.yml`: Core application configuration
- `application-dev.yml`: Development overrides
- `application-staging.yml`: Staging environment
- `application-prod.yml`: Production settings
- `application-test.yml`: Testing configuration

## 🔒 Security Features

### Authentication & Authorization
- **JWT Tokens**: Access (15min) and refresh (7 days) tokens
- **OAuth2 Integration**: Compatible with major identity providers
- **Role-Based Access**: Fine-grained permissions
- **Multi-Factor Authentication**: Optional MFA support

### Data Protection
- **Encryption at Rest**: AES-256 database encryption
- **Encryption in Transit**: TLS 1.3 for all communications
- **Key Management**: AWS KMS or HashiCorp Vault integration
- **Data Masking**: Automatic PII masking in logs

### Compliance
- **HIPAA Compliance**: Full HIPAA security controls
- **Audit Logging**: Comprehensive audit trail
- **Data Retention**: Configurable retention policies
- **Breach Detection**: Real-time security monitoring

## 📊 Monitoring & Observability

### Health Checks
```bash
# Application health
curl http://localhost:8081/actuator/health

# Database connectivity
curl http://localhost:8081/actuator/health/db

# Cache health
curl http://localhost:8081/actuator/health/redis
```

### Metrics Endpoints
```bash
# Prometheus metrics
curl http://localhost:8081/actuator/prometheus

# Application metrics
curl http://localhost:8081/actuator/metrics
```

### Logging
- **Structured Logging**: JSON format for log aggregation
- **Correlation IDs**: Request tracing across services
- **Log Levels**: Environment-specific log levels
- **Log Rotation**: Size and time-based rotation

### Monitoring Stack
- **Prometheus**: Metrics collection
- **Grafana**: Visualization and dashboards
- **AlertManager**: Alert routing and management
- **Jaeger**: Distributed tracing (optional)

## 🧪 Testing

### Test Suites
```bash
# Run all tests
./scripts/test-environment.sh test

# Run specific test suites
./scripts/test-environment.sh test-unit
./scripts/test-environment.sh test-integration
./scripts/test-environment.sh test-performance
./scripts/test-environment.sh test-security
```

### Test Data Generation
```bash
# Generate synthetic test data
./scripts/generate-test-data.sh

# Simulate HL7 messages
./scripts/simulate-hl7-messages.sh --continuous
```

### Performance Testing
```bash
# Load testing
./scripts/test-environment.sh load-test --users=100 --duration=5m

# Stress testing
./scripts/test-environment.sh stress-test --max-users=1000
```

## 🔍 Troubleshooting

### Common Issues

#### Port Conflicts
```bash
# Check port usage
netstat -tulpn | grep :8080

# Change ports in docker-compose.yml
```

#### Memory Issues
```bash
# Check Docker memory
docker stats

# Increase Docker Desktop memory allocation
```

#### Database Connection Issues
```bash
# Test database connectivity
docker-compose exec postgres pg_isready -U fhir_user

# Check database logs
docker-compose logs postgres
```

#### Application Startup Issues
```bash
# Check application logs
docker-compose logs fhir-bridge

# Check health endpoint
curl http://localhost:8081/actuator/health
```

### Debug Mode
```bash
# Enable debug logging
export SPRING_PROFILES_ACTIVE=dev
export LOGGING_LEVEL_COM_BRIDGE=DEBUG

# Remote debugging
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar fhir-bridge.jar
```

## 🏭 Production Deployment

### AWS ECS Fargate Deployment
```bash
# Deploy to AWS
./scripts/complete-aws-setup.sh

# Monitor deployment
aws ecs list-services --cluster fhir-bridge-cluster
```

### Infrastructure as Code
```bash
# Initialize Terraform
cd infra/terraform
terraform init

# Plan deployment
terraform plan -out=tfplan

# Apply deployment
terraform apply tfplan
```

### Production Checklist
- [ ] SSL/TLS certificates configured
- [ ] Database backups enabled
- [ ] Monitoring alerts configured
- [ ] Security groups properly configured
- [ ] Auto-scaling policies set
- [ ] Disaster recovery tested
- [ ] Performance benchmarks met
- [ ] Security audit completed

## 💾 Backup & Disaster Recovery

### Automated Backups
- **Database**: Daily automated PostgreSQL backups
- **Configuration**: Version-controlled infrastructure
- **Secrets**: Encrypted backup of secrets

### Disaster Recovery
- **RTO**: 1 hour (Recovery Time Objective)
- **RPO**: 15 minutes (Recovery Point Objective)
- **Multi-region**: Cross-region replication
- **Failover**: Automated failover procedures

### Backup Procedures
```bash
# Manual database backup
./infra/db/backup/backup-database.sh

# Restore from backup
./infra/db/backup/restore-database.sh --backup-file=backup-2024-01-01.sql

# Verify backup integrity
./scripts/validate-backup.sh --backup-file=backup-2024-01-01.sql
```

## 📚 Additional Documentation

- **[DOCKER.md](DOCKER.md)**: Comprehensive Docker setup guide
- **[DOCKER-COMPOSE-SETUP.md](DOCKER-COMPOSE-SETUP.md)**: Local testing environment
- **[CONFIGURATION_SUMMARY.md](CONFIGURATION_SUMMARY.md)**: Configuration reference
- **[DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)**: Detailed deployment procedures
- **[TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)**: Troubleshooting guide
- **[SECURITY.md](docs/SECURITY.md)**: Security documentation
- **[MONITORING.md](docs/MONITORING.md)**: Monitoring setup

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Run tests: `./scripts/test-environment.sh test`
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Issues**: Create GitHub issues for bugs
- **Discussions**: Use GitHub discussions for questions
- **Security**: Report security issues privately
- **Documentation**: Check docs/ directory for guides
