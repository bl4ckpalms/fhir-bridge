# FHIR Bridge Database Setup Guide

## Overview
This directory contains all database-related configurations, migrations, and utilities for the FHIR Bridge application.

## Directory Structure
```
infra/db/
├── backup/                 # Backup and restore utilities
│   ├── backup-database.sh
│   ├── restore-database.sh
│   └── Dockerfile.backup
├── init/                   # Database initialization scripts
│   └── 01-init-database.sql
├── monitoring/             # Monitoring and alerting configurations
│   ├── postgres-exporter.yml
│   └── postgres-alerts.yml
├── migrations/             # Flyway migration scripts (in src/main/resources/db/migration/)
└── README.md              # This file
```

## Database Migrations

### Flyway Configuration
The application uses Flyway for database migrations. Migrations are located in:
- `src/main/resources/db/migration/`

### Available Migrations
1. **V1__Initial_schema.sql** - Creates initial database schema
2. **V2__Performance_optimizations.sql** - Adds performance indexes and optimizations
3. **V3__Reference_data.sql** - Inserts reference data and utility functions

### Running Migrations
```bash
# Local development
mvn flyway:migrate -Dflyway.configFiles=flyway-local.conf

# Staging
mvn flyway:migrate -Dflyway.configFiles=flyway-staging.conf

# Production
mvn flyway:migrate -Dflyway.configFiles=flyway-prod.conf
```

## Backup and Restore

### Automated Backups
- **Schedule**: Daily at 2:00 AM UTC
- **Retention**: 30 days
- **Encryption**: AES-256 with GPG
- **Storage**: Local + AWS S3

### Manual Backup
```bash
# Create backup
./infra/db/backup/backup-database.sh production full

# Create schema-only backup
./infra/db/backup/backup-database.sh production schema

# Create data-only backup
./infra/db/backup/backup-database.sh production data
```

### Restore from Backup
```bash
# Restore latest backup
./infra/db/backup/restore-database.sh production

# Restore specific backup
./infra/db/backup/restore-database.sh production /path/to/backup.sql.gz
```

## Monitoring and Alerting

### Prometheus Metrics
- **Exporter**: postgres_exporter
- **Port**: 9187
- **Scrape Interval**: 30s

### Key Metrics Monitored
- Database availability
- Connection count
- Query performance
- Storage usage
- Replication lag
- FHIR Bridge specific metrics

### Alerting Rules
- PostgreSQLDown (Critical)
- PostgreSQLTooManyConnections (Warning)
- PostgreSQLDiskSpaceUsage (Warning)
- PostgreSQLLongRunningQueries (Warning)
- FHIRBridgeAuditEventsHigh (Warning)
- PostgreSQLBackupFailed (Critical)

## Database Configuration

### Connection Pool Settings
- **Maximum Pool Size**: 50 (production), 20 (development)
- **Minimum Idle**: 10 (production), 5 (development)
- **Connection Timeout**: 30s
- **Idle Timeout**: 10m
- **Max Lifetime**: 30m

### Performance Tuning
- **Shared Buffers**: 25% of RAM
- **Effective Cache Size**: 75% of RAM
- **Work Mem**: 4MB
- **Maintenance Work Mem**: 256MB
- **Checkpoint Completion Target**: 0.9

## Security Configuration

### Database User Permissions
- **Application User**: Read/Write on application tables
- **Migration User**: Full schema modification
- **Monitoring User**: Read-only access for metrics
- **Backup User**: Read-only access for backups

### Encryption
- **At Rest**: AWS RDS encryption with KMS
- **In Transit**: TLS 1.2+
- **Backups**: GPG encryption with AES-256

## Environment Variables

### Required Variables
```bash
# Database
DATABASE_HOST=your-db-host
DATABASE_PORT=5432
DATABASE_NAME=fhirbridge
DATABASE_USERNAME=fhir_user
DATABASE_PASSWORD=secure-password

# Backup
S3_BACKUPS_BUCKET=your-backup-bucket
ENCRYPTION_KEY=your-gpg-key
RETENTION_DAYS=30

# Monitoring
POSTGRES_EXPORTER_USER=monitoring_user
POSTGRES_EXPORTER_PASSWORD=monitoring_password
```

## Docker Setup

### Backup Container
```bash
# Build backup container
docker build -f infra/db/backup/Dockerfile.backup -t fhir-bridge-db-backup .

# Run backup container
docker run -d \
  --name fhir-bridge-backup \
  -e DATABASE_HOST=postgres \
  -e DATABASE_NAME=fhir_bridge \
  -e DATABASE_USERNAME=fhir_user \
  -e DATABASE_PASSWORD=fhir_password \
  -e S3_BACKUPS_BUCKET=your-bucket \
  -e ENCRYPTION_KEY=your-key \
  fhir-bridge-db-backup
```

### Monitoring Container
```bash
# Run postgres_exporter
docker run -d \
  --name postgres-exporter \
  -p 9187:9187 \
  -e DATA_SOURCE_NAME="postgresql://monitoring_user:password@postgres:5432/fhir_bridge?sslmode=disable" \
  -v $(pwd)/infra/db/monitoring/postgres-exporter.yml:/etc/postgres_exporter/postgres_exporter.yml \
  prometheuscommunity/postgres-exporter:latest
```

## Health Checks

### Database Health Check
```sql
SELECT health_check();
```

### Application Health Check
```bash
curl -f http://localhost:8081/actuator/health
```

## Troubleshooting

### Common Issues

1. **Migration Failures**
   - Check Flyway schema history table
   - Verify database connectivity
   - Check migration file syntax

2. **Connection Issues**
   - Verify connection pool settings
   - Check database logs
   - Verify network connectivity

3. **Performance Issues**
   - Check slow query logs
   - Review index usage
   - Monitor connection pool metrics

### Debug Commands
```bash
# Check database connectivity
psql -h localhost -U fhir_user -d fhir_bridge -c "SELECT 1;"

# Check table sizes
psql -h localhost -U fhir_user -d fhir_bridge -c "SELECT pg_size_pretty(pg_total_relation_size('audit_events'));"

# Check active connections
psql -h localhost -U fhir_user -d fhir_bridge -c "SELECT * FROM pg_stat_activity;"

# Check slow queries
psql -h localhost -U fhir_user -d fhir_bridge -c "SELECT query, query_start FROM pg_stat_activity WHERE state = 'active';"
```

## Support
For database-related issues, contact the DevOps team or create a ticket in the issue tracking system.