# FHIR Bridge Database Deployment Guide

## Overview
This guide provides step-by-step instructions for deploying the complete database infrastructure including migrations, backups, and monitoring.

## Prerequisites

### Required Tools
- Docker & Docker Compose
- PostgreSQL 15+
- AWS CLI (for S3 backups)
- Terraform (for S3 bucket setup)
- Maven (for Flyway migrations)

### Environment Variables
Create `.env` files for each environment:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fhir_bridge
DB_USER=fhir_user
DB_PASSWORD=secure_password

# AWS Configuration
AWS_REGION=us-east-1
S3_BACKUPS_BUCKET=fhir-bridge-db-backups
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key

# Monitoring Configuration
GRAFANA_PASSWORD=secure_grafana_password
SMTP_HOST=smtp.gmail.com:587
SMTP_USERNAME=your_email
SMTP_PASSWORD=your_email_password
ALERT_EMAIL_FROM=alerts@fhirbridge.com
ALERT_EMAIL_TO=devops@fhirbridge.com
```

## Step 1: Environment Configuration

### Local Development
```bash
# Create local environment file
cp infra/db/config/flyway-local.conf.template infra/db/config/flyway-local.conf
# Edit with your local database credentials
```

### Staging/Production
```bash
# Set environment variables
export STAGING_DB_HOST=your-staging-db-host
export STAGING_DB_USER=your-staging-user
export STAGING_DB_PASSWORD=your-staging-password

export PRODUCTION_DB_HOST=your-production-db-host
export PRODUCTION_DB_USER=your-production-user
export PRODUCTION_DB_PASSWORD=your-production-password
```

## Step 2: S3 Bucket Setup

### Using Terraform
```bash
cd infra/db
terraform init
terraform plan -var="environment=staging"
terraform apply -var="environment=staging"
```

### Manual Setup (if not using Terraform)
1. Create S3 bucket with versioning enabled
2. Enable server-side encryption with KMS
3. Configure lifecycle policies for backup retention
4. Set up IAM roles for backup access

## Step 3: Database Migrations

### Run Migrations
```bash
# Local
mvn flyway:migrate -Dflyway.configFiles=infra/db/config/flyway-local.conf

# Staging
mvn flyway:migrate -Dflyway.configFiles=infra/db/config/flyway-staging.conf

# Production
mvn flyway:migrate -Dflyway.configFiles=infra/db/config/flyway-prod.conf
```

### Verify Migrations
```bash
# Check migration status
mvn flyway:info -Dflyway.configFiles=infra/db/config/flyway-local.conf

# Validate schema
mvn flyway:validate -Dflyway.configFiles=infra/db/config/flyway-local.conf
```

## Step 4: Monitoring Stack Deployment

### Local Development
```bash
cd infra/db
chmod +x deploy-monitoring.sh
./deploy-monitoring.sh local deploy
```

### Access Monitoring
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **AlertManager**: http://localhost:9093

### Production Deployment
```bash
# Deploy monitoring stack
./deploy-monitoring.sh production deploy

# Configure alerts
# Update alertmanager.yml with production email/Slack settings
```

## Step 5: Backup Configuration

### Test Backup/Restore
```bash
# Run comprehensive tests
chmod +x test-backup-restore.sh
./test-backup-restore.sh staging

# Manual backup test
./backup-database.sh staging full /tmp/test-backup.sql.gz

# Manual restore test
./restore-database.sh staging /tmp/test-backup.sql.gz
```

### Schedule Automated Backups

#### Using Cron (Linux/Mac)
```bash
# Add to crontab
(crontab -l 2>/dev/null; echo "0 2 * * * /path/to/backup-database.sh production") | crontab -

# Verify cron job
crontab -l | grep backup-database
```

#### Using Windows Task Scheduler
```bash
# Create scheduled task
schtasks /create /tn "FHIR Bridge Backup" /tr "C:\path\to\backup-database.bat production" /sc daily /st 02:00
```

#### Using Docker
```bash
# Build backup container
docker build -f Dockerfile.backup -t fhir-bridge-backup .

# Run with cron
docker run -d \
  --name fhir-bridge-backup \
  -e ENVIRONMENT=production \
  -e S3_BACKUPS_BUCKET=your-bucket \
  -e ENCRYPTION_KEY=your-key \
  fhir-bridge-backup
```

## Step 6: Health Checks and Monitoring

### Database Health Check
```sql
-- Test database connectivity
SELECT health_check();

-- Check table counts
SELECT 
    'audit_events' as table_name, COUNT(*) as record_count FROM audit_events
UNION ALL
SELECT 
    'consent_records' as table_name, COUNT(*) as record_count FROM consent_records;
```

### Monitoring Health Check
```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets'

# Check Grafana health
curl http://localhost:3000/api/health

# Check PostgreSQL exporter
curl http://localhost:9187/metrics | grep -E "pg_up|fhir_bridge"
```

## Step 7: Security Configuration

### Database Security
```sql
-- Create monitoring user
CREATE USER monitoring_user WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE fhir_bridge TO monitoring_user;
GRANT USAGE ON SCHEMA public TO monitoring_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO monitoring_user;

-- Create backup user
CREATE USER backup_user WITH PASSWORD 'secure_password';
GRANT CONNECT ON DATABASE fhir_bridge TO backup_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO backup_user;
```

### Encryption Setup
```bash
# Generate GPG key for backup encryption
gpg --gen-key

# Export public key
gpg --armor --export your-email@domain.com > backup-public-key.asc

# Import for backup scripts
gpg --import backup-public-key.asc
```

## Step 8: Validation and Testing

### End-to-End Test
```bash
# Run complete validation
./test-backup-restore.sh staging run

# Check monitoring alerts
# Trigger test alert
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[{"labels":{"alertname":"TestAlert","severity":"warning"}}]'
```

### Performance Testing
```bash
# Test database performance
psql -h localhost -U fhir_user -d fhir_bridge -c "
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;"
```

## Troubleshooting

### Common Issues

1. **Migration Failures**
   ```bash
   # Check Flyway history
   mvn flyway:info -Dflyway.configFiles=infra/db/config/flyway-local.conf
   
   # Repair failed migrations
   mvn flyway:repair -Dflyway.configFiles=infra/db/config/flyway-local.conf
   ```

2. **Backup Failures**
   ```bash
   # Check logs
   tail -f /var/log/fhir-bridge/backup.log
   
   # Test connectivity
   pg_isready -h localhost -p 5432 -U fhir_user
   ```

3. **Monitoring Issues**
   ```bash
   # Check Prometheus targets
   docker-compose -f monitoring/docker-compose.monitoring.yml logs prometheus
   
   # Check PostgreSQL exporter
   curl http://localhost:9187/metrics
   ```

## Maintenance

### Regular Tasks
- **Daily**: Verify backup completion
- **Weekly**: Review monitoring dashboards
- **Monthly**: Test restore procedures
- **Quarterly**: Update security patches

### Backup Retention
- **Daily**: Keep for 7 days
- **Weekly**: Keep for 4 weeks
- **Monthly**: Keep for 12 months
- **Yearly**: Keep for 7 years (HIPAA compliance)

## Support and Escalation

### Emergency Contacts
- **Database Issues**: database-team@fhirbridge.com
- **Monitoring Issues**: devops-team@fhirbridge.com
- **Security Issues**: security-team@fhirbridge.com

### Documentation Links
- [Database Schema Documentation](./README.md)
- [Monitoring Dashboards](./monitoring/README.md)
- [Backup Procedures](./backup/README.md)