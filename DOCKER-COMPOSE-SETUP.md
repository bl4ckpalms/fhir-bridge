# Docker Compose Setup for Local Testing

This document provides a comprehensive overview of the Docker Compose setup for FHIR Bridge local testing and development.

## 📁 Files Created

### Docker Compose Configurations
- **`docker-compose.test.yml`** - Complete testing environment with all dependencies
- **`docker-compose.local.yml`** - Updated local development environment
- **`Dockerfile.dev`** - Development-optimized Docker image (already existed)

### Management Scripts
- **`scripts/test-environment.bat`** - Windows management script
- **`scripts/test-environment.sh`** - Linux/macOS management script  
- **`scripts/validate-docker-setup.bat`** - Windows validation script

### Test Utilities
- **`scripts/generate-test-data.sh`** - Test data generation script
- **`scripts/simulate-hl7-messages.sh`** - HL7 message simulation script

### Documentation
- **`TESTING.md`** - Comprehensive testing guide
- **`DOCKER-COMPOSE-SETUP.md`** - This setup overview

## 🚀 Quick Start

### Windows
```cmd
cd fhir-bridge
scripts\validate-docker-setup.bat
scripts\test-environment.bat start
```

### Linux/macOS
```bash
cd fhir-bridge
chmod +x scripts/*.sh
./scripts/test-environment.sh start
```

## 🏗️ Architecture

### Test Environment (`docker-compose.test.yml`)
- **PostgreSQL** (port 5433) - Test database with sample data
- **Redis** (port 6380) - Cache layer for performance testing
- **FHIR Bridge** (port 8083) - Main application in test mode
- **PgAdmin** (port 5051) - Database administration (optional)
- **Redis Commander** (port 8085) - Redis administration (optional)

### Local Development (`docker-compose.local.yml`)
- **PostgreSQL** (port 5432) - Development database
- **Redis** (port 6379) - Development cache
- **FHIR Bridge** (port 8080) - Application (optional, can run via IDE)
- **PgAdmin** (port 5050) - Database administration (optional)
- **Redis Commander** (port 8082) - Redis administration (optional)

## 🔧 Key Features

### Service Profiles
- **Default**: Core services (database, cache, application)
- **admin**: Administrative tools (PgAdmin, Redis Commander)
- **test-data**: Test data generation utilities
- **simulator**: HL7 message simulation tools
- **integration-tests**: Automated test execution

### Health Checks
- All services include comprehensive health checks
- Proper dependency ordering with health check conditions
- Startup timing optimized for reliable initialization

### Development Features
- **Debug Support**: Remote debugging on port 5005/5006
- **Live Reload**: Volume mounts for source code changes
- **Comprehensive Logging**: Structured logging with correlation IDs
- **Performance Monitoring**: JVM and application metrics

### Testing Capabilities
- **Synthetic Data**: Automated generation of HIPAA-compliant test data
- **Message Simulation**: HL7 v2 message sending with various scenarios
- **Integration Testing**: Full end-to-end test execution
- **Load Testing**: Continuous message simulation for performance testing

## 📊 Service Configuration

| Service | Test Port | Local Port | Purpose |
|---------|-----------|------------|---------|
| FHIR Bridge | 8083 | 8080 | Main API |
| Management | 8084 | 8081 | Health/Metrics |
| PostgreSQL | 5433 | 5432 | Database |
| Redis | 6380 | 6379 | Cache |
| PgAdmin | 5051 | 5050 | DB Admin |
| Redis Commander | 8085 | 8082 | Cache Admin |
| Debug | 5006 | 5005 | Remote Debug |

## 🔐 Security Configuration

### Test Environment
- **Database**: `fhir_test_user` / `fhir_test_password`
- **PgAdmin**: `test@fhirbridge.local` / `test123`
- **Redis Commander**: `test` / `test123`
- **JWT Secret**: `test-secret-key-for-local-testing-only`

### Local Environment  
- **Database**: `fhir_user` / `fhir_password`
- **PgAdmin**: `admin@fhirbridge.local` / `admin123`
- **Redis Commander**: `admin` / `admin123`

## 📈 Performance Optimization

### Container Resources
- **JVM Heap**: 50% of container memory for development, 75% for production
- **Garbage Collector**: G1GC for better performance
- **Connection Pooling**: HikariCP with optimized settings
- **Cache Configuration**: Redis with appropriate memory limits

### Volume Management
- **Persistent Data**: Named volumes for database and cache data
- **Development**: Bind mounts for logs and build artifacts
- **Configuration**: Read-only mounts for config files

## 🧪 Testing Workflows

### 1. Basic Setup Validation
```bash
./scripts/test-environment.sh start
./scripts/test-environment.sh status
```

### 2. Test Data Generation
```bash
./scripts/test-environment.sh generate-data
```

### 3. HL7 Message Simulation
```bash
./scripts/test-environment.sh simulate
./scripts/test-environment.sh simulate --continuous
```

### 4. Integration Testing
```bash
./scripts/test-environment.sh test
```

### 5. Development Workflow
```bash
# Start dependencies only
docker compose -f docker-compose.local.yml up -d postgres redis

# Run application locally
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## 🔍 Monitoring and Debugging

### Application Logs
```bash
./scripts/test-environment.sh logs fhir-bridge-test
```

### Database Access
```bash
psql -h localhost -p 5433 -U fhir_test_user -d fhir_bridge_test
```

### Cache Access
```bash
redis-cli -h localhost -p 6380
```

### Health Monitoring
- Application: http://localhost:8084/actuator/health
- Metrics: http://localhost:8084/actuator/metrics
- Database: PgAdmin at http://localhost:5051
- Cache: Redis Commander at http://localhost:8085

## 🧹 Cleanup and Maintenance

### Stop Services
```bash
./scripts/test-environment.sh stop
```

### Complete Cleanup
```bash
./scripts/test-environment.sh clean
```

### Update Images
```bash
docker compose -f docker-compose.test.yml pull
docker compose -f docker-compose.test.yml build --no-cache
```

## 🚨 Troubleshooting

### Common Issues
1. **Port Conflicts**: Modify ports in compose files if needed
2. **Memory Issues**: Increase Docker Desktop memory allocation
3. **Permission Issues**: Ensure proper file permissions on Linux/macOS
4. **Network Issues**: Check firewall settings and Docker network configuration

### Debug Commands
```bash
# Check container status
docker compose -f docker-compose.test.yml ps

# View service logs
docker compose -f docker-compose.test.yml logs [service-name]

# Execute commands in containers
docker compose -f docker-compose.test.yml exec postgres-test psql -U fhir_test_user
docker compose -f docker-compose.test.yml exec redis-test redis-cli
docker compose -f docker-compose.test.yml exec fhir-bridge-test curl localhost:8081/actuator/health
```

## 🔄 CI/CD Integration

The Docker Compose setup is designed for easy integration with CI/CD pipelines:

```yaml
# Example GitHub Actions workflow
- name: Start test environment
  run: docker compose -f docker-compose.test.yml up -d --wait

- name: Run tests
  run: docker compose -f docker-compose.test.yml --profile integration-tests up --exit-code-from integration-tests

- name: Cleanup
  run: docker compose -f docker-compose.test.yml down -v
```

## 📚 Additional Resources

- **TESTING.md** - Detailed testing procedures and workflows
- **DOCKER.md** - Production Docker deployment guide
- **README.md** - General project documentation
- **infra/** - Infrastructure configuration files

This Docker Compose setup provides a complete, isolated testing environment that supports the full development lifecycle of the FHIR Bridge application.