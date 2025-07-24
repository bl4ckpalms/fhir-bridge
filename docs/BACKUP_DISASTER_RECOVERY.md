# FHIR Bridge Backup and Disaster Recovery Guide

## Overview

This comprehensive guide covers backup strategies, disaster recovery procedures, and business continuity planning for the FHIR Bridge application. It ensures HIPAA compliance, data integrity, and minimal downtime during disasters.

## 📋 Backup Strategy Overview

### Backup Types
- **Full Backups**: Complete database snapshots
- **Incremental Backups**: Changes since last backup
- **Differential Backups**: Changes since last full backup
- **Point-in-Time Recovery**: Restore to specific timestamp
- **Cross-Region Backups**: Geographic redundancy

### Recovery Objectives
- **RTO (Recovery Time Objective)**: 1 hour
- **RPO (Recovery Point Objective)**: 15 minutes
- **Maximum Tolerable Downtime**: 4 hours
- **Data Loss Tolerance**: 0% for critical data

## 🗄️ Database Backup Procedures

### 1. PostgreSQL Backup Strategies

#### Automated Daily Backups
```bash
#!/bin/bash
# backup-database.sh - Daily backup script

set -e

# Configuration
BACKUP_DIR="/app/backups/postgresql"
S3_BUCKET="fhir-bridge-backups"
RETENTION_DAYS=30
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DB_NAME="fhir_bridge"
DB_USER="fhir_user"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"

# Create backup directory
mkdir -p "$BACKUP_DIR"

# Full database backup
echo "🔄 Starting PostgreSQL backup..."
pg_dump \
  -h "$DB_HOST" \
  -p "$DB_PORT" \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --verbose \
  --format=custom \
  --compress=9 \
  --file="$BACKUP_DIR/fhir_bridge_${TIMESTAMP}.dump"

# Verify backup
if [ -f "$BACKUP_DIR/fhir_bridge_${TIMESTAMP}.dump" ]; then
  echo "✅ Backup created: fhir_bridge_${TIMESTAMP}.dump"
  
  # Upload to S3
  aws s3 cp "$BACKUP_DIR/fhir_bridge_${TIMESTAMP}.dump" \
    "s3://${S3_BUCKET}/postgresql/$(date +%Y/%m)/"
  
  # Clean old backups
  find "$BACKUP_DIR" -name "*.dump" -mtime +$RETENTION_DAYS -delete
  
  # Clean S3 backups
  aws s3 rm "s3://${S3_BUCKET}/postgresql/" \
    --recursive \
    --exclude "*" \
    --include "*.dump" \
    --older-than ${RETENTION_DAYS}days
  
  echo "✅ Backup completed and uploaded to S3"
else
  echo "❌ Backup failed"
  exit 1
fi
```

#### Continuous Archiving (WAL Shipping)
```bash
#!/bin/bash
# setup-wal-archiving.sh

# PostgreSQL configuration for continuous archiving
cat >> /var/lib/postgresql/data/postgresql.conf << EOF
# WAL archiving
wal_level = replica
archive_mode = on
archive_command = 'test ! -f /var/lib/postgresql/wal_archive/%f && cp %p /var/lib/postgresql/wal_archive/%f'
archive_timeout = 300

# Replication slots
max_replication_slots = 10
max_wal_senders = 10
EOF

# Create archive directory
mkdir -p /var/lib/postgresql/wal_archive
chown postgres:postgres /var/lib/postgresql/wal_archive

# Upload WAL files to S3
cat > /usr/local/bin/upload-wal.sh << 'EOF'
#!/bin/bash
aws s3 cp /var/lib/postgresql/wal_archive/ s3://fhir-bridge-backups/wal/ \
  --recursive \
  --include "*.backup" \
  --include "*.history"
EOF
chmod +x /usr/local/bin/upload-wal.sh
```

#### Point-in-Time Recovery Setup
```bash
#!/bin/bash
# setup-pitr.sh

# Create base backup
pg_basebackup \
  -h localhost \
  -U fhir_user \
  -D /var/lib/postgresql/backups/base_backup \
  -Ft \
  -z \
  -P \
  -X stream \
  -c fast

# Create recovery configuration
cat > /var/lib/postgresql/data/recovery.conf << EOF
restore_command = 'aws s3 cp s3://fhir-bridge-backups/wal/%f %p'
recovery_target_time = '$(date -d '1 hour ago' -Iseconds)'
recovery_target_timeline = 'latest'
standby_mode = off
EOF
```

### 2. Redis Backup Procedures

#### Redis Data Backup
```bash
#!/bin/bash
# backup-redis.sh

BACKUP_DIR="/app/backups/redis"
S3_BUCKET="fhir-bridge-backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Create Redis backup
redis-cli BGSAVE
sleep 10  # Wait for background save to complete

# Copy RDB file
cp /var/lib/redis/dump.rdb "$BACKUP_DIR/redis_${TIMESTAMP}.rdb"

# Upload to S3
aws s3 cp "$BACKUP_DIR/redis_${TIMESTAMP}.rdb" \
  "s3://${S3_BUCKET}/redis/$(date +%Y/%m)/"

# Clean old backups
find "$BACKUP_DIR" -name "*.rdb" -mtime +7 -delete

echo "✅ Redis backup completed"
```

#### Redis AOF Persistence
```bash
#!/bin/bash
# setup-redis-aof.sh

# Enable AOF persistence
redis-cli CONFIG SET appendonly yes
redis-cli CONFIG SET appendfsync everysec

# Create AOF backup
cat > /usr/local/bin/backup-redis-aof.sh << 'EOF'
#!/bin/bash
redis-cli BGREWRITEAOF
aws s3 cp /var/lib/redis/appendonly.aof \
  s3://fhir-bridge-backups/redis/aof/$(date +%Y%m%d_%H%M%S).aof
EOF
chmod +x /usr/local/bin/backup-redis-aof.sh
```

### 3. Application Configuration Backup

#### Configuration Backup
```bash
#!/bin/bash
# backup-config.sh

BACKUP_DIR="/app/backups/config"
S3_BUCKET="fhir-bridge-backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p "$BACKUP_DIR"

# Backup application configuration
tar -czf "$BACKUP_DIR/config_${TIMESTAMP}.tar.gz" \
  src/main/resources/application*.yml \
  infra/terraform/*.tf \
  infra/terraform/*.tfvars \
  docker-compose*.yml

# Backup environment variables
env | grep -E "(DB_|REDIS_|JWT_|SSL_)" > "$BACKUP_DIR/env_${TIMESTAMP}.txt"

# Upload to S3
aws s3 cp "$BACKUP_DIR/config_${TIMESTAMP}.tar.gz" \
  "s3://${S3_BUCKET}/config/$(date +%Y/%m)/"

aws s3 cp "$BACKUP_DIR/env_${TIMESTAMP}.txt" \
  "s3://${S3_BUCKET}/config/$(date +%Y/%m)/"

echo "✅ Configuration backup completed"
```

## 🔄 Disaster Recovery Procedures

### 1. Disaster Recovery Levels

#### Level 1: Service Restart (0-5 minutes)
```bash
#!/bin/bash
# level1-recovery.sh

echo "🔄 Level 1 Recovery: Service Restart"

# Check service status
if ! docker-compose ps | grep -q "Up"; then
  echo "Starting services..."
  docker-compose up -d
  
  # Wait for startup
  sleep 30
  
  # Verify health
  if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ Service recovered"
  else
    echo "❌ Service restart failed - escalating to Level 2"
    exit 1
  fi
fi
```

#### Level 2: Container Recovery (5-15 minutes)
```bash
#!/bin/bash
# level2-recovery.sh

echo "🔄 Level 2 Recovery: Container Recovery"

# Stop all services
docker-compose down

# Remove containers and volumes
docker-compose down -v

# Rebuild and start
docker-compose up --build -d

# Wait for startup
sleep 60

# Verify recovery
if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
  echo "✅ Container recovery completed"
else
  echo "❌ Container recovery failed - escalating to Level 3"
  exit 1
fi
```

#### Level 3: Data Recovery (15-60 minutes)
```bash
#!/bin/bash
# level3-recovery.sh

BACKUP_DATE=${1:-$(date -d '1 day ago' +%Y%m%d)}

echo "🔄 Level 3 Recovery: Data Recovery"

# Stop services
docker-compose down

# Restore database
./infra/db/backup/restore-database.sh --backup-date=$BACKUP_DATE

# Restore Redis
./infra/db/backup/restore-redis.sh --backup-date=$BACKUP_DATE

# Start services
docker-compose up -d

# Verify recovery
sleep 60
curl -f http://localhost:8081/actuator/health
```

#### Level 4: Infrastructure Recovery (1-4 hours)
```bash
#!/bin/bash
# level4-recovery.sh

ENVIRONMENT=${1:-production}

echo "🔄 Level 4 Recovery: Infrastructure Recovery"

# Deploy new infrastructure
cd infra/terraform
terraform workspace select $ENVIRONMENT
terraform apply -auto-approve

# Update DNS
./scripts/update-dns.sh $ENVIRONMENT

# Verify deployment
./scripts/verify-deployment.sh $ENVIRONMENT
```

### 2. Database Recovery Procedures

#### Full Database Recovery
```bash
#!/bin/bash
# restore-database.sh

BACKUP_FILE=$1
TARGET_DB=${2:-fhir_bridge}

echo "🔄 Restoring database from $BACKUP_FILE..."

# Stop application
docker-compose stop fhir-bridge

# Drop and recreate database
docker-compose exec postgres psql -U postgres -c "DROP DATABASE IF EXISTS $TARGET_DB;"
docker-compose exec postgres psql -U postgres -c "CREATE DATABASE $TARGET_DB OWNER fhir_user;"

# Restore from backup
if [[ $BACKUP_FILE == *.sql ]]; then
  # Plain SQL backup
  docker-compose exec -T postgres psql -U fhir_user -d $TARGET_DB < "$BACKUP_FILE"
elif [[ $BACKUP_FILE == *.dump ]]; then
  # Custom format backup
  docker-compose exec -T postgres pg_restore -U fhir_user -d $TARGET_DB "$BACKUP_FILE"
fi

# Run migrations
docker-compose run --rm fhir-bridge ./mvnw flyway:migrate

# Start application
docker-compose start fhir-bridge

echo "✅ Database recovery completed"
```

#### Point-in-Time Recovery
```bash
#!/bin/bash
# pit-recovery.sh

TARGET_TIME=$1  # Format: "2024-01-15 14:30:00"

echo "🔄 Performing point-in-time recovery to $TARGET_TIME..."

# Stop PostgreSQL
docker-compose stop postgres

# Create recovery configuration
cat > /tmp/recovery.conf << EOF
restore_command = 'aws s3 cp s3://fhir-bridge-backups/wal/%f %p'
recovery_target_time = '$TARGET_TIME'
recovery_target_timeline = 'latest'
standby_mode = off
EOF

# Start PostgreSQL in recovery mode
docker-compose run --rm postgres \
  postgres --single -D /var/lib/postgresql/data \
  -c "restore_command='aws s3 cp s3://fhir-bridge-backups/wal/%f %p'" \
  -c "recovery_target_time='$TARGET_TIME'"

# Verify recovery
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT now();"
```

### 3. Redis Recovery Procedures

#### Redis Data Recovery
```bash
#!/bin/bash
# restore-redis.sh

BACKUP_FILE=$1

echo "🔄 Restoring Redis from $BACKUP_FILE..."

# Stop Redis
docker-compose stop redis

# Clear Redis data
docker-compose exec redis redis-cli FLUSHALL

# Restore from backup
if [[ $BACKUP_FILE == *.rdb ]]; then
  # RDB file
  cp "$BACKUP_FILE" /var/lib/redis/dump.rdb
  chown redis:redis /var/lib/redis/dump.rdb
elif [[ $BACKUP_FILE == *.aof ]]; then
  # AOF file
  cp "$BACKUP_FILE" /var/lib/redis/appendonly.aof
  chown redis:redis /var/lib/redis/appendonly.aof
fi

# Start Redis
docker-compose start redis

# Verify recovery
docker-compose exec redis redis-cli ping
```

## 🏥 Business Continuity Planning

### 1. High Availability Architecture

#### Multi-Region Setup
```yaml
# multi-region-deployment.yml
regions:
  primary:
    region: us-east-1
    database: rds-primary
    redis: elasticache-primary
    
  secondary:
    region: us-west-2
    database: rds-replica
    redis: elasticache-replica
    
  disaster-recovery:
    region: eu-west-1
    database: rds-snapshot
    redis: elasticache-restore
```

#### Load Balancer Configuration
```hcl
# alb-multi-region.tf
resource "aws_route53_health_check" "primary" {
  fqdn              = aws_lb.primary.dns_name
  port              = 443
  type              = "HTTPS"
  resource_path     = "/actuator/health"
  failure_threshold = "3"
  request_interval  = "30"
}

resource "aws_route53_record" "failover" {
  zone_id = aws_route53_zone.main.zone_id
  name    = "api.yourcompany.com"
  type    = "A"
  
  failover_routing_policy {
    type = "PRIMARY"
  }
  
  set_identifier = "Primary"
  records        = [aws_lb.primary.dns_name]
  health_check_id = aws_route53_health_check.primary.id
}
```

### 2. Disaster Recovery Testing

#### Monthly DR Drill
```bash
#!/bin/bash
# dr-drill.sh

echo "🧪 Starting disaster recovery drill..."

# 1. Create test environment
./scripts/create-dr-test-env.sh

# 2. Simulate failure
./scripts/simulate-failure.sh

# 3. Execute recovery
./scripts/execute-dr-recovery.sh

# 4. Verify functionality
./scripts/verify-dr-recovery.sh

# 5. Generate report
./scripts/generate-dr-report.sh

echo "✅ DR drill completed"
```

#### Recovery Time Testing
```bash
#!/bin/bash
# measure-recovery-time.sh

START_TIME=$(date +%s)

# Trigger failure
docker-compose down

# Execute recovery
./scripts/execute-recovery.sh

# Wait for service
while ! curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; do
  sleep 10
done

END_TIME=$(date +%s)
RECOVERY_TIME=$((END_TIME - START_TIME))

echo "Recovery time: ${RECOVERY_TIME} seconds"
```

## 📊 Backup Monitoring and Validation

### 1. Backup Verification

#### Automated Backup Testing
```bash
#!/bin/bash
# test-backup-restore.sh

BACKUP_FILE=$1

echo "🧪 Testing backup restoration..."

# Create test environment
docker-compose -f docker-compose.test.yml up -d postgres-test

# Restore backup
./infra/db/backup/restore-database.sh "$BACKUP_FILE" fhir_bridge_test

# Run validation queries
docker-compose exec postgres-test psql -U fhir_user -d fhir_bridge_test -c "
  SELECT 
    'consent_records' as table_name, 
    COUNT(*) as record_count 
  FROM consent_records
  UNION ALL
  SELECT 
    'audit_events' as table_name, 
    COUNT(*) as record_count 
  FROM audit_events
  UNION ALL
  SELECT 
    'fhir_resources' as table_name, 
    COUNT(*) as record_count 
  FROM fhir_resources;
"

# Cleanup
docker-compose -f docker-compose.test.yml down

echo "✅ Backup test completed"
```

#### Backup Integrity Checks
```bash
#!/bin/bash
# verify-backup-integrity.sh

BACKUP_FILE=$1

echo "🔍 Verifying backup integrity..."

# Check file size
FILE_SIZE=$(stat -c%s "$BACKUP_FILE")
if [ $FILE_SIZE -lt 1000 ]; then
  echo "❌ Backup file too small"
  exit 1
fi

# Check file format
if [[ $BACKUP_FILE == *.dump ]]; then
  pg_restore -l "$BACKUP_FILE" > /dev/null 2>&1 || {
    echo "❌ Invalid dump format"
    exit 1
  }
elif [[ $BACKUP_FILE == *.sql ]]; then
  head -n 1 "$BACKUP_FILE" | grep -q "PostgreSQL" || {
    echo "❌ Invalid SQL format"
    exit 1
  }
fi

# Checksum verification
BACKUP_CHECKSUM=$(aws s3api head-object \
  --bucket fhir-bridge-backups \
  --key "postgresql/$(basename "$BACKUP_FILE")" \
  --query 'Metadata.checksum' \
  --output text)

LOCAL_CHECKSUM=$(md5sum "$BACKUP_FILE" | cut -d' ' -f1)

if [ "$BACKUP_CHECKSUM" != "$LOCAL_CHECKSUM" ]; then
  echo "❌ Checksum mismatch"
  exit 1
fi

echo "✅ Backup integrity verified"
```

### 2. Backup Monitoring

#### CloudWatch Alarms
```bash
#!/bin/bash
# setup-backup-monitoring.sh

# Create backup failure alarm
aws cloudwatch put-metric-alarm \
  --alarm-name fhir-bridge-backup-failure \
  --alarm-description "Backup job failure" \
  --metric-name BackupFailure \
  --namespace FHIRBridge/Backup \
  --statistic Sum \
  --period 3600 \
  --threshold 1 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1

# Create backup age alarm
aws cloudwatch put-metric-alarm \
  --alarm-name fhir-bridge-backup-stale \
  --alarm-description "Backup older than 25 hours" \
  --metric-name BackupAge \
  --namespace FHIRBridge/Backup \
  --statistic Maximum \
  --period 3600 \
  --threshold 90000 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 1
```

#### Backup Dashboard
```json
{
  "widgets": [
    {
      "type": "metric",
      "properties": {
        "metrics": [
          ["FHIRBridge/Backup", "BackupSuccess", {"stat": "Sum"}],
          [".", "BackupFailure", {"stat": "Sum"}],
          [".", "BackupSize", {"stat": "Average"}]
        ],
        "period": 3600,
        "stat": "Average",
        "region": "us-east-1",
        "title": "Backup Metrics"
      }
    }
  ]
}
```

## 🚨 Emergency Response Procedures

### 1. Incident Response Playbook

#### Severity Levels
- **P0 - Critical**: Complete service outage, data loss
- **P1 - High**: Major functionality impaired
- **P2 - Medium**: Minor functionality impaired
- **P3 - Low**: Cosmetic issues, enhancement requests

#### Response Procedures
```bash
#!/bin/bash
# incident-response.sh

SEVERITY=$1
INCIDENT_ID=$2

echo "🚨 Incident Response: $SEVERITY - $INCIDENT_ID"

case $SEVERITY in
  P0)
    echo "Critical incident - immediate escalation"
    ./scripts/escalate-p0.sh $INCIDENT_ID
    ./scripts/activate-war-room.sh $INCIDENT_ID
    ;;
  P1)
    echo "High priority incident"
    ./scripts/escalate-p1.sh $INCIDENT_ID
    ;;
  P2|P3)
    echo "Standard incident handling"
    ./scripts/create-incident-ticket.sh $INCIDENT_ID
    ;;
esac
```

### 2. Communication Plan

#### Stakeholder Notification
```bash
#!/bin/bash
# notify-stakeholders.sh

INCIDENT_TYPE=$1
IMPACT=$2
ETA=$3

# Send notifications
aws sns publish \
  --topic-arn arn:aws:sns:us-east-1:123456789012:fhir-bridge-alerts \
  --message "INCIDENT: $INCIDENT_TYPE - Impact: $IMPACT - ETA: $ETA" \
  --subject "FHIR Bridge Incident Alert"

# Update status page
curl -X POST https://status.yourcompany.com/api/incidents \
  -H "Authorization: Bearer $STATUS_PAGE_TOKEN" \
  -d "{
    \"name\": \"$INCIDENT_TYPE\",
    \"status\": \"investigating\",
    \"impact\": \"$IMPACT\",
    \"components\": [\"api\", \"database\"]
  }"
```

## 📋 Recovery Checklists

### 1. Pre-Recovery Checklist
- [ ] Incident severity assessed
- [ ] Recovery team assembled
- [ ] Stakeholders notified
- [ ] Latest backup identified
- [ ] Recovery environment prepared
- [ ] Rollback plan ready

### 2. Recovery Execution Checklist
- [ ] Services stopped gracefully
- [ ] Data backup verified
- [ ] Recovery procedure executed
- [ ] Services restarted
- [ ] Health checks passed
- [ ] Data integrity verified
- [ ] Performance validated

### 3. Post-Recovery Checklist
- [ ] Stakeholders notified of recovery
- [ ] Monitoring re-enabled
- [ ] Incident report created
- [ ] Lessons learned documented
- [ ] Recovery time measured
- [ ] Backup strategy reviewed

## 🎯 Quick Reference

### Emergency Contacts
- **On-call Engineer**: +1-XXX-XXX-XXXX
- **Database Admin**: +1-XXX-XXX-XXXX
- **Infrastructure Lead**: +1-XXX-XXX-XXXX
- **Security Team**: security@yourcompany.com

### Key Commands
```bash
# Emergency backup
./infra/db/backup/backup-database.sh --emergency

# Quick restore
./infra/db/backup/restore-database.sh --latest

# Health check
curl -f http://localhost:8081/actuator/health

# Recovery status
./scripts/check-recovery-status.sh
```

### Recovery Time Targets
- **Service Restart**: 5 minutes
- **Container Recovery**: 15 minutes
- **Database Recovery**: 60 minutes
- **Infrastructure Recovery**: 4 hours
- **Multi-region Recovery**: 24 hours