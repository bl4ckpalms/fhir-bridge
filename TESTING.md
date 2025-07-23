# FHIR Bridge Testing Environment

This document describes how to set up and use the Docker Compose testing environment for FHIR Bridge development and testing.

## Overview

The testing environment provides a complete, isolated setup for testing FHIR Bridge functionality with all required dependencies:

- **PostgreSQL Database** - Test database with sample data
- **Redis Cache** - Caching layer for performance testing
- **FHIR Bridge Application** - Main application in test mode
- **Admin Tools** - PgAdmin and Redis Commander for data inspection
- **Test Utilities** - Data generators and HL7 message simulators

## Quick Start

### Prerequisites

- Docker Desktop installed and running
- At least 4GB RAM available for containers
- Ports 5433, 6380, 8083-8085 available

### Starting the Test Environment

**Windows:**
```cmd
cd fhir-bridge
scripts\test-environment.bat start
```

**Linux/macOS:**
```bash
cd fhir-bridge
docker compose -f docker-compose.test.yml up -d postgres-test redis-test fhir-bridge-test
```

### Verifying the Setup

Check that all services are running:
```cmd
scripts\test-environment.bat status
```

Or manually check:
```bash
curl http://localhost:8084/actuator/health
```

## Service Configuration

### Core Services

| Service | Port | Purpose | Credentials |
|---------|------|---------|-------------|
| FHIR Bridge | 8083 | Main API | N/A |
| Management | 8084 | Health/Metrics | N/A |
| PostgreSQL | 5433 | Test Database | `fhir_test_user` / `fhir_test_password` |
| Redis | 6380 | Cache | No password |

### Optional Admin Services

Enable with `--profile admin`:

| Service | Port | Purpose | Credentials |
|---------|------|---------|-------------|
| PgAdmin | 5051 | Database Admin | `test@fhirbridge.local` / `test123` |
| Redis Commander | 8085 | Redis Admin | `test` / `test123` |

```bash
docker compose -f docker-compose.test.yml --profile admin up -d pgadmin-test redis-commander-test
```

## Testing Workflows

### 1. Basic API Testing

Test the transformation endpoint:
```bash
curl -X POST http://localhost:8083/api/v1/transform \
  -H "Content-Type: application/x-hl7-v2" \
  -H "Accept: application/fhir+json" \
  --data-raw "MSH|^~\&|SENDING_APP|SENDING_FACILITY|..."
```

### 2. Generate Test Data

Populate the database with sample data:
```cmd
scripts\test-environment.bat generate-data
```

This creates:
- 5 consent records with various statuses
- 4 test user accounts with different roles  
- Sample audit events
- HL7 v2 message samples
- FHIR resource examples

### 3. Simulate HL7 Messages

Send test HL7 messages to the API:
```cmd
scripts\test-environment.bat simulate
```

For continuous testing:
```cmd
scripts\test-environment.bat simulate --continuous
```

### 4. Run Integration Tests

Execute the full test suite:
```cmd
scripts\test-environment.bat test
```

Or manually:
```bash
docker compose -f docker-compose.test.yml --profile integration-tests up --build integration-tests
```

## Development Workflow

### 1. Start Dependencies Only

For local development, start only the database and cache:
```bash
docker compose -f docker-compose.test.yml up -d postgres-test redis-test
```

Then run the application locally:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

### 2. Debug Mode

The containerized application includes debug port 5006:
```bash
# Connect your IDE debugger to localhost:5006
```

### 3. Live Development

Mount source code for live updates:
```yaml
# Add to fhir-bridge-test service in docker-compose.test.yml
volumes:
  - ./src:/app/src:ro
  - ./target:/app/target
```

## Test Data Structure

### Database Schema

The test environment creates these tables:
- `consent_records` - Patient consent management
- `audit_events` - Audit trail logging
- `users` - User accounts and roles
- `fhir_resources` - Stored FHIR resources

### Sample HL7 Messages

Located in `src/test/resources/hl7-messages/`:
- `adt-a01-sample.hl7` - Patient admission
- `orm-o01-sample.hl7` - Order message
- `oru-r01-sample.hl7` - Observation results

### Sample FHIR Resources

Located in `src/test/resources/fhir-resources/`:
- `patient-sample.json` - Patient resource
- Additional resources as needed

## Monitoring and Debugging

### Application Logs

View real-time logs:
```cmd
scripts\test-environment.bat logs fhir-bridge-test
```

### Database Inspection

Connect to PostgreSQL:
```bash
psql -h localhost -p 5433 -U fhir_test_user -d fhir_bridge_test
```

Or use PgAdmin at http://localhost:5051

### Cache Inspection

Connect to Redis:
```bash
redis-cli -h localhost -p 6380
```

Or use Redis Commander at http://localhost:8085

### Health Checks

Monitor service health:
- Application: http://localhost:8084/actuator/health
- Database: `pg_isready -h localhost -p 5433 -U fhir_test_user`
- Cache: `redis-cli -h localhost -p 6380 ping`

## Performance Testing

### Load Testing

Use the continuous simulator for load testing:
```bash
# Terminal 1: Start continuous simulation
scripts\test-environment.bat simulate --continuous

# Terminal 2: Monitor performance
curl http://localhost:8084/actuator/metrics
```

### Memory and CPU Monitoring

Monitor container resources:
```bash
docker stats fhir-bridge-test-app fhir-bridge-test-db fhir-bridge-test-cache
```

## Cleanup

### Stop Services

```cmd
scripts\test-environment.bat stop
```

### Complete Cleanup

Remove all containers, volumes, and data:
```cmd
scripts\test-environment.bat clean
```

## Troubleshooting

### Common Issues

1. **Port Conflicts**
   - Ensure ports 5433, 6380, 8083-8085 are available
   - Modify ports in docker-compose.test.yml if needed

2. **Memory Issues**
   - Increase Docker Desktop memory allocation
   - Reduce JVM heap size in container environment

3. **Database Connection Issues**
   - Wait for PostgreSQL health check to pass
   - Check logs: `docker logs fhir-bridge-test-db`

4. **Application Startup Issues**
   - Check application logs: `docker logs fhir-bridge-test-app`
   - Verify database schema initialization

### Debug Commands

```bash
# Check container status
docker compose -f docker-compose.test.yml ps

# View all logs
docker compose -f docker-compose.test.yml logs

# Restart specific service
docker compose -f docker-compose.test.yml restart fhir-bridge-test

# Execute commands in containers
docker compose -f docker-compose.test.yml exec postgres-test psql -U fhir_test_user -d fhir_bridge_test
docker compose -f docker-compose.test.yml exec redis-test redis-cli
docker compose -f docker-compose.test.yml exec fhir-bridge-test curl localhost:8081/actuator/health
```

## Integration with CI/CD

### GitHub Actions Example

```yaml
name: Integration Tests
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Start test environment
        run: docker compose -f docker-compose.test.yml up -d --wait
      - name: Generate test data
        run: docker compose -f docker-compose.test.yml --profile test-data up test-data-generator
      - name: Run integration tests
        run: docker compose -f docker-compose.test.yml --profile integration-tests up --exit-code-from integration-tests integration-tests
      - name: Cleanup
        run: docker compose -f docker-compose.test.yml down -v
```

## Configuration Reference

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `test` | Spring profile |
| `DB_HOST` | `postgres-test` | Database host |
| `DB_PORT` | `5432` | Database port |
| `DB_NAME` | `fhir_bridge_test` | Database name |
| `DB_USERNAME` | `fhir_test_user` | Database user |
| `DB_PASSWORD` | `fhir_test_password` | Database password |
| `REDIS_HOST` | `redis-test` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `JWT_ISSUER_URI` | `http://localhost:8080/auth` | JWT issuer |

### Volume Mounts

- `postgres_test_data` - Database data persistence
- `redis_test_data` - Redis data persistence  
- `./logs` - Application logs
- `./target/test-results` - Test results
- `./src/test/resources` - Test resources

### Network Configuration

- Network: `fhir-bridge-test-network`
- Subnet: `172.20.0.0/16`
- Driver: `bridge`

This testing environment provides a comprehensive platform for developing, testing, and validating FHIR Bridge functionality in an isolated, reproducible manner.