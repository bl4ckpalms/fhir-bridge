# Local Development with Docker Compose

This guide explains how to set up and use Docker Compose for local development and testing of the FHIR Bridge application.

## Quick Start

### 1. Start Dependencies Only (Recommended for Development)

If you want to run the application from your IDE or via Maven while using containerized dependencies:

```bash
# Start PostgreSQL and Redis only
docker-compose -f docker-compose.local.yml up postgres redis

# Or run in background
docker-compose -f docker-compose.local.yml up -d postgres redis
```

Then run the application locally:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Start Everything (Full Containerized Setup)

To run the complete application stack in containers:

```bash
# Start all services including the application
docker-compose -f docker-compose.local.yml --profile app up

# Or run in background
docker-compose -f docker-compose.local.yml --profile app up -d
```

### 3. Start with Admin Tools

To include database and Redis admin interfaces:

```bash
# Start with admin tools
docker-compose -f docker-compose.local.yml --profile admin up

# Or with both app and admin tools
docker-compose -f docker-compose.local.yml --profile app --profile admin up
```

## Available Services

| Service | Port | Description | Profile |
|---------|------|-------------|---------|
| postgres | 5432 | PostgreSQL Database | default |
| redis | 6379 | Redis Cache | default |
| fhir-bridge | 8080, 8081, 5005 | FHIR Bridge App | app |
| pgadmin | 5050 | PostgreSQL Admin | admin |
| redis-commander | 8082 | Redis Admin | admin |

## Service Details

### PostgreSQL Database
- **Host**: localhost:5432
- **Database**: fhir_bridge
- **Username**: fhir_user
- **Password**: fhir_password
- **Admin UI**: http://localhost:5050 (when admin profile is active)
  - Email: admin@fhirbridge.local
  - Password: admin123

### Redis Cache
- **Host**: localhost:6379
- **No password required**
- **Admin UI**: http://localhost:8082 (when admin profile is active)
  - Username: admin
  - Password: admin123

### FHIR Bridge Application (when using app profile)
- **Application**: http://localhost:8080
- **Management/Actuator**: http://localhost:8081
- **Debug Port**: 5005
- **Health Check**: http://localhost:8081/actuator/health

## Common Commands

### Start Services
```bash
# Dependencies only (recommended for development)
docker-compose -f docker-compose.local.yml up -d postgres redis

# Full application stack
docker-compose -f docker-compose.local.yml --profile app up -d

# With admin tools
docker-compose -f docker-compose.local.yml --profile admin up -d

# Everything
docker-compose -f docker-compose.local.yml --profile app --profile admin up -d
```

### Stop Services
```bash
# Stop all services
docker-compose -f docker-compose.local.yml down

# Stop and remove volumes (WARNING: This will delete all data)
docker-compose -f docker-compose.local.yml down -v
```

### View Logs
```bash
# All services
docker-compose -f docker-compose.local.yml logs

# Specific service
docker-compose -f docker-compose.local.yml logs postgres
docker-compose -f docker-compose.local.yml logs redis
docker-compose -f docker-compose.local.yml logs fhir-bridge

# Follow logs
docker-compose -f docker-compose.local.yml logs -f
```

### Check Service Status
```bash
# List running services
docker-compose -f docker-compose.local.yml ps

# Check health status
docker-compose -f docker-compose.local.yml ps --services --filter "status=running"
```

## Development Workflow

### Option 1: Hybrid Development (Recommended)
1. Start dependencies: `docker-compose -f docker-compose.local.yml up -d postgres redis`
2. Run application locally: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
3. Develop and test with hot reload
4. Stop dependencies when done: `docker-compose -f docker-compose.local.yml down`

### Option 2: Full Container Development
1. Start everything: `docker-compose -f docker-compose.local.yml --profile app up -d`
2. Attach debugger to port 5005 if needed
3. View logs: `docker-compose -f docker-compose.local.yml logs -f fhir-bridge`
4. Stop when done: `docker-compose -f docker-compose.local.yml down`

## Testing

### Integration Testing
```bash
# Start test dependencies
docker-compose -f docker-compose.local.yml up -d postgres redis

# Run integration tests
./mvnw test -Dspring.profiles.active=test

# Or use the dedicated test compose file
docker-compose -f docker-compose.test.yml up --abort-on-container-exit
```

### Manual Testing with Admin Tools
```bash
# Start with admin interfaces
docker-compose -f docker-compose.local.yml --profile admin up -d

# Access pgAdmin at http://localhost:5050
# Access Redis Commander at http://localhost:8082
```

## Troubleshooting

### Port Conflicts
If you get port conflicts, you can modify the ports in `docker-compose.local.yml` or stop conflicting services:

```bash
# Check what's using a port
netstat -an | findstr :5432  # Windows
lsof -i :5432               # macOS/Linux

# Stop conflicting PostgreSQL service
net stop postgresql-x64-15  # Windows
brew services stop postgresql  # macOS
sudo systemctl stop postgresql  # Linux
```

### Database Connection Issues
```bash
# Check if PostgreSQL is ready
docker-compose -f docker-compose.local.yml exec postgres pg_isready -U fhir_user -d fhir_bridge

# Connect to database directly
docker-compose -f docker-compose.local.yml exec postgres psql -U fhir_user -d fhir_bridge
```

### Redis Connection Issues
```bash
# Check Redis connectivity
docker-compose -f docker-compose.local.yml exec redis redis-cli ping

# Connect to Redis CLI
docker-compose -f docker-compose.local.yml exec redis redis-cli
```

### Application Issues
```bash
# Check application logs
docker-compose -f docker-compose.local.yml logs fhir-bridge

# Check health endpoint
curl http://localhost:8081/actuator/health

# Restart application service
docker-compose -f docker-compose.local.yml restart fhir-bridge
```

### Clean Reset
```bash
# Stop everything and remove volumes
docker-compose -f docker-compose.local.yml down -v

# Remove images (optional)
docker-compose -f docker-compose.local.yml down --rmi all

# Start fresh
docker-compose -f docker-compose.local.yml up -d postgres redis
```

## Environment Variables

You can customize the setup using environment variables:

```bash
# Create .env file in the project root
cat > .env << EOF
DB_USERNAME=custom_user
DB_PASSWORD=custom_password
REDIS_PASSWORD=redis_password
JWT_ISSUER_URI=http://localhost:8080/auth
EOF

# Start with custom configuration
docker-compose -f docker-compose.local.yml up -d
```

## Data Persistence

- **PostgreSQL data**: Stored in `postgres_local_data` Docker volume
- **Redis data**: Stored in `redis_local_data` Docker volume
- **Application logs**: Mounted to `./logs` directory

To backup data:
```bash
# Backup PostgreSQL
docker-compose -f docker-compose.local.yml exec postgres pg_dump -U fhir_user fhir_bridge > backup.sql

# Restore PostgreSQL
docker-compose -f docker-compose.local.yml exec -T postgres psql -U fhir_user fhir_bridge < backup.sql
```