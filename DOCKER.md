# FHIR Bridge Docker Setup

This document provides comprehensive instructions for setting up and running the FHIR Bridge application using Docker and Docker Compose.

## Quick Start

### Prerequisites

- Docker 20.10+ and Docker Compose 2.0+
- At least 4GB of available RAM
- Ports 8080, 8081, 5432, 6379 available on your system

### Development Environment

```bash
# Clone the repository and navigate to the project directory
cd fhir-bridge

# Start the development environment (Linux/macOS)
./scripts/dev-setup.sh setup

# Start the development environment (Windows)
scripts\dev-setup.bat setup
```

The development environment includes:
- FHIR Bridge application with hot reload
- PostgreSQL database with pgAdmin
- Redis cache with Redis Commander
- Nginx reverse proxy
- Mailhog for email testing

## Docker Compose Configurations

### 1. Production Configuration (`docker-compose.yml`)

The main configuration for production deployment:

```bash
# Start production environment
docker-compose up -d

# View logs
docker-compose logs -f

# Stop environment
docker-compose down
```

**Services:**
- `fhir-bridge`: Main application (ports 8080, 8081)
- `postgres`: PostgreSQL database (port 5432)
- `redis`: Redis cache (port 6379)

### 2. Development Configuration (`docker-compose.override.yml`)

Automatically loaded in development, extends the main configuration:

```bash
# Start development environment (automatically uses override)
docker-compose up -d

# Access development tools:
# - pgAdmin: http://localhost:5050 (admin@fhirbridge.local / admin123)
# - Redis Commander: http://localhost:8082 (admin / admin123)
# - Mailhog: http://localhost:8025
```

**Additional Services:**
- `pgadmin`: Database administration interface
- `redis-commander`: Redis administration interface
- `mailhog`: Email testing service
- `nginx`: Reverse proxy with rate limiting

### 3. Test Configuration (`docker-compose.test.yml`)

For running automated tests:

```bash
# Run all tests
docker-compose -f docker-compose.test.yml up --build --abort-on-container-exit

# Run specific test suites
docker-compose -f docker-compose.test.yml run fhir-bridge-test
docker-compose -f docker-compose.test.yml run integration-tests
docker-compose -f docker-compose.test.yml run performance-tests
```

## Environment Variables

### Database Configuration
- `DB_HOST`: Database host (default: postgres)
- `DB_PORT`: Database port (default: 5432)
- `DB_NAME`: Database name (default: fhir_bridge)
- `DB_USERNAME`: Database username (default: fhir_user)
- `DB_PASSWORD`: Database password (default: fhir_password)

### Redis Configuration
- `REDIS_HOST`: Redis host (default: redis)
- `REDIS_PORT`: Redis port (default: 6379)
- `REDIS_PASSWORD`: Redis password (default: empty)

### Application Configuration
- `SPRING_PROFILES_ACTIVE`: Spring profile (dev/test/prod)
- `JWT_ISSUER_URI`: JWT issuer URI for authentication
- `JAVA_OPTS`: JVM options for the application

## Development Workflow

### 1. Initial Setup

```bash
# Make scripts executable (Linux/macOS)
chmod +x scripts/dev-setup.sh

# Run setup
./scripts/dev-setup.sh setup
```

### 2. Daily Development

```bash
# Start services
./scripts/dev-setup.sh start

# View logs
./scripts/dev-setup.sh logs

# Run tests
./scripts/dev-setup.sh test

# Stop services
./scripts/dev-setup.sh stop
```

### 3. Code Changes

The development configuration supports hot reload:
- Java code changes trigger automatic restart
- Configuration changes are picked up automatically
- Debug port 5005 is exposed for IDE debugging

## Service Access Points

### Application Services
- **FHIR Bridge API**: http://localhost:8080
- **Management/Actuator**: http://localhost:8081
- **Health Check**: http://localhost:8081/actuator/health

### Development Tools
- **pgAdmin**: http://localhost:5050
  - Email: admin@fhirbridge.local
  - Password: admin123
- **Redis Commander**: http://localhost:8082
  - Username: admin
  - Password: admin123
- **Mailhog**: http://localhost:8025
- **Nginx Proxy**: http://localhost:80

### Direct Database Access
- **PostgreSQL**: localhost:5432
  - Database: fhir_bridge
  - Username: fhir_user
  - Password: fhir_password
- **Redis**: localhost:6379

## Debugging

### Application Debugging

The development configuration exposes port 5005 for remote debugging:

1. Configure your IDE to connect to `localhost:5005`
2. Set breakpoints in your code
3. Debug requests will pause at breakpoints

### Database Debugging

Use pgAdmin or connect directly:

```bash
# Connect to PostgreSQL
docker-compose exec postgres psql -U fhir_user -d fhir_bridge

# View database logs
docker-compose logs postgres
```

### Cache Debugging

Use Redis Commander or connect directly:

```bash
# Connect to Redis CLI
docker-compose exec redis redis-cli

# View Redis logs
docker-compose logs redis
```

## Testing

### Unit Tests

```bash
# Run unit tests
docker-compose -f docker-compose.test.yml run fhir-bridge-test
```

### Integration Tests

```bash
# Run integration tests
docker-compose -f docker-compose.test.yml run integration-tests
```

### Performance Tests

```bash
# Run performance tests
docker-compose -f docker-compose.test.yml run performance-tests

# View results
ls -la target/performance-results/
```

## Troubleshooting

### Common Issues

1. **Port Conflicts**
   ```bash
   # Check what's using the ports
   netstat -tulpn | grep :8080
   
   # Stop conflicting services or change ports in docker-compose.yml
   ```

2. **Memory Issues**
   ```bash
   # Check Docker memory usage
   docker stats
   
   # Adjust JAVA_OPTS MaxRAMPercentage if needed
   ```

3. **Database Connection Issues**
   ```bash
   # Check database health
   docker-compose exec postgres pg_isready -U fhir_user
   
   # View database logs
   docker-compose logs postgres
   ```

4. **Application Startup Issues**
   ```bash
   # Check application logs
   docker-compose logs fhir-bridge
   
   # Check health endpoint
   curl http://localhost:8081/actuator/health
   ```

### Cleanup

```bash
# Stop and remove containers
docker-compose down

# Remove containers and volumes
docker-compose down --volumes

# Complete cleanup (removes images too)
./scripts/dev-setup.sh cleanup
```

## Production Deployment

For production deployment:

1. Use the main `docker-compose.yml` without override
2. Set appropriate environment variables
3. Configure external database and Redis if needed
4. Set up proper SSL/TLS termination
5. Configure monitoring and logging

```bash
# Production deployment
SPRING_PROFILES_ACTIVE=prod \
DB_PASSWORD=secure_password \
JWT_ISSUER_URI=https://your-auth-server.com \
docker-compose up -d
```

## Security Considerations

- Change default passwords in production
- Use environment variables for sensitive configuration
- Enable SSL/TLS for external access
- Configure proper firewall rules
- Regular security updates for base images
- Monitor access logs and security events

## Performance Tuning

### JVM Tuning
Adjust `JAVA_OPTS` in docker-compose.yml:
```yaml
JAVA_OPTS: >-
  -XX:+UseContainerSupport
  -XX:MaxRAMPercentage=75.0
  -XX:+UseG1GC
  -XX:+UseStringDeduplication
```

### Database Tuning
Modify PostgreSQL configuration in `infra/db/postgresql.conf`

### Redis Tuning
Modify Redis configuration in `infra/redis/redis-dev.conf`

## Monitoring

The application includes comprehensive monitoring:
- Health checks for all services
- Actuator endpoints for metrics
- Structured logging with correlation IDs
- Performance monitoring aspects

Access monitoring endpoints:
- Health: http://localhost:8081/actuator/health
- Metrics: http://localhost:8081/actuator/metrics
- Info: http://localhost:8081/actuator/info