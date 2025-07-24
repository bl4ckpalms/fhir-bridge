# FHIR Bridge Troubleshooting and Maintenance Guide

## Overview

This comprehensive guide provides troubleshooting procedures, maintenance tasks, and operational procedures for the FHIR Bridge application across all environments.

## 🚨 Quick Diagnostic Commands

### Health Check Commands
```bash
# Application health
curl -f http://localhost:8081/actuator/health

# Detailed health with components
curl http://localhost:8081/actuator/health | jq .

# Individual component health
curl http://localhost:8081/actuator/health/db
curl http://localhost:8081/actuator/health/redis
curl http://localhost:8081/actuator/health/diskSpace
```

### System Status Commands
```bash
# Docker containers
docker-compose ps

# Resource usage
docker stats

# Database connectivity
docker-compose exec postgres pg_isready -U fhir_user

# Redis connectivity
docker-compose exec redis redis-cli ping

# Application logs
docker-compose logs -f fhir-bridge --tail=100
```

## 🔍 Common Issues and Solutions

### 1. Application Startup Issues

#### Issue: Application fails to start
**Symptoms:**
- Container exits immediately
- Error logs show "Failed to configure a DataSource"
- Port binding failures

**Diagnosis:**
```bash
# Check application logs
docker-compose logs fhir-bridge | grep -i error

# Check configuration
docker-compose config

# Verify environment variables
docker-compose exec fhir-bridge env | grep -E "(DB_|REDIS_|JWT_)"
```

**Solutions:**
```bash
# Missing database configuration
export DB_HOST=postgres
export DB_NAME=fhir_bridge
export DB_USERNAME=fhir_user
export DB_PASSWORD=fhir_password

# Port conflicts
# Edit docker-compose.yml to change ports
sed -i 's/8080:8080/8082:8080/g' docker-compose.yml

# Insufficient memory
# Increase Docker Desktop memory allocation
# Or adjust JVM options
export JAVA_OPTS="-Xmx1g -Xms512m"
```

#### Issue: Database connection failures
**Symptoms:**
- "Connection refused" errors
- "FATAL: database does not exist"
- Timeout errors

**Diagnosis:**
```bash
# Test database connectivity
docker-compose exec postgres pg_isready -U fhir_user -d fhir_bridge

# Check database logs
docker-compose logs postgres

# Verify database exists
docker-compose exec postgres psql -U fhir_user -l
```

**Solutions:**
```bash
# Reset database
docker-compose down -v
docker-compose up -d postgres
./scripts/wait-for-postgres.sh

# Manual database creation
docker-compose exec postgres createdb -U fhir_user fhir_bridge

# Check database permissions
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "\du"
```

### 2. Performance Issues

#### Issue: High CPU usage
**Symptoms:**
- Slow response times
- Container CPU usage > 80%
- Application timeouts

**Diagnosis:**
```bash
# Monitor CPU usage
docker stats --no-stream

# Check JVM metrics
curl http://localhost:8081/actuator/metrics/jvm.memory.used
curl http://localhost:8081/actuator/metrics/process.cpu.usage

# Thread dump
docker-compose exec fhir-bridge jstack 1 > thread-dump.txt
```

**Solutions:**
```bash
# Increase memory allocation
export JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"

# Optimize database connections
# Edit application.yml
spring.datasource.hikari.maximum-pool-size: 20

# Enable caching
# Check Redis configuration
docker-compose exec redis redis-cli info stats | grep keyspace
```

#### Issue: Memory leaks
**Symptoms:**
- Increasing memory usage over time
- OutOfMemoryError exceptions
- Container restarts

**Diagnosis:**
```bash
# Memory usage monitoring
docker stats --no-stream

# Heap dump analysis
docker-compose exec fhir-bridge jmap -dump:format=b,file=/tmp/heap-dump.hprof 1

# GC analysis
docker-compose exec fhir-bridge jstat -gc 1 5s
```

**Solutions:**
```bash
# Increase heap size
export JAVA_OPTS="-Xmx4g -Xms2g"

# Enable GC logging
export JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:file=/app/logs/gc.log"

# Restart with memory profiling
docker-compose restart fhir-bridge
```

### 3. Database Issues

#### Issue: Database connection pool exhaustion
**Symptoms:**
- "Connection pool is at maximum size" errors
- Database timeout errors
- Application hangs

**Diagnosis:**
```bash
# Check connection pool metrics
curl http://localhost:8081/actuator/metrics/hikaricp.connections

# Database connection count
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT count(*) FROM pg_stat_activity;"

# Check for long-running queries
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT pid, now() - pg_stat_activity.query_start AS duration, query FROM pg_stat_activity WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes';"
```

**Solutions:**
```bash
# Increase connection pool size
# Edit application.yml
spring.datasource.hikari.maximum-pool-size: 50
spring.datasource.hikari.connection-timeout: 30000

# Optimize slow queries
# Check query performance
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "EXPLAIN ANALYZE SELECT * FROM consent_records WHERE created_date > NOW() - INTERVAL '1 day';"

# Add database indexes
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "CREATE INDEX idx_consent_created_date ON consent_records(created_date);"
```

#### Issue: Database corruption
**Symptoms:**
- Data integrity errors
- Application crashes on specific queries
- PostgreSQL logs show corruption warnings

**Diagnosis:**
```bash
# Check database integrity
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT pg_database.datname, pg_database_size(pg_database.datname) FROM pg_database WHERE datname = 'fhir_bridge';"

# Check for corrupted indexes
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "REINDEX DATABASE fhir_bridge;"
```

**Solutions:**
```bash
# Restore from backup
./infra/db/backup/restore-database.sh --backup-file=latest-backup.sql

# Repair corrupted indexes
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "REINDEX DATABASE fhir_bridge;"

# Verify data integrity
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT COUNT(*) FROM consent_records;"
```

### 4. Redis Cache Issues

#### Issue: Cache connectivity problems
**Symptoms:**
- "Redis connection refused" errors
- Cache misses increasing
- Performance degradation

**Diagnosis:**
```bash
# Test Redis connectivity
docker-compose exec redis redis-cli ping

# Check Redis info
docker-compose exec redis redis-cli info

# Monitor Redis commands
docker-compose exec redis redis-cli monitor
```

**Solutions:**
```bash
# Restart Redis
docker-compose restart redis

# Clear Redis cache
docker-compose exec redis redis-cli FLUSHALL

# Check Redis memory usage
docker-compose exec redis redis-cli info memory
```

#### Issue: Cache key conflicts
**Symptoms:**
- Wrong data returned from cache
- Cache invalidation not working
- Data inconsistency

**Diagnosis:**
```bash
# Check cache keys
docker-compose exec redis redis-cli KEYS "*consent*"

# Check TTL for keys
docker-compose exec redis redis-cli TTL "consent:12345"

# Monitor cache operations
docker-compose exec redis redis-cli --csv pubsub channels
```

**Solutions:**
```bash
# Clear specific cache keys
docker-compose exec redis redis-cli DEL "consent:*"

# Set proper TTL
# Update cache configuration
spring.cache.redis.time-to-live: 3600000

# Use cache namespaces
# Update cache names to include environment prefix
```

### 5. Security Issues

#### Issue: JWT token validation failures
**Symptoms:**
- "Invalid token" errors
- "Token expired" errors
- Authentication failures

**Diagnosis:**
```bash
# Check JWT configuration
curl http://localhost:8081/actuator/configprops | jq '.contexts.application.beans."jwt-config"'

# Verify JWT issuer
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8081/actuator/health

# Check token expiration
echo "YOUR_TOKEN" | cut -d. -f2 | base64 -d | jq '.exp'
```

**Solutions:**
```bash
# Update JWT secret
export JWT_SECRET=your-new-256-bit-secret

# Sync system clocks
docker-compose exec fhir-bridge date

# Update JWT issuer configuration
# Edit application.yml
jwt.issuer-uri: https://correct-auth-server.com
```

#### Issue: SSL/TLS certificate problems
**Symptoms:**
- SSL handshake failures
- Certificate validation errors
- HTTPS not working

**Diagnosis:**
```bash
# Check SSL certificate
openssl s_client -connect localhost:8080 -servername localhost

# Verify certificate chain
openssl x509 -in certificate.pem -text -noout

# Check certificate expiration
echo | openssl s_client -connect localhost:8080 2>/dev/null | openssl x509 -noout -dates
```

**Solutions:**
```bash
# Update SSL certificates
# Copy new certificates to /app/ssl/
docker-compose restart fhir-bridge

# Disable SSL verification (development only)
export SSL_VERIFY=false

# Update certificate paths
# Edit application.yml
server.ssl.key-store: /app/ssl/new-keystore.p12
```

## 🔧 Maintenance Procedures

### 1. Regular Maintenance Tasks

#### Daily Tasks
```bash
#!/bin/bash
# daily-maintenance.sh

echo "🔄 Starting daily maintenance..."

# Check application health
if ! curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
  echo "❌ Application health check failed"
  exit 1
fi

# Check disk space
DISK_USAGE=$(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
if [ $DISK_USAGE -gt 80 ]; then
  echo "⚠️  Disk usage is ${DISK_USAGE}%"
fi

# Check memory usage
MEMORY_USAGE=$(free | grep Mem | awk '{printf "%.0f", $3/$2 * 100.0}')
if [ $MEMORY_USAGE -gt 85 ]; then
  echo "⚠️  Memory usage is ${MEMORY_USAGE}%"
fi

# Rotate logs
find /app/logs -name "*.log" -size +100M -exec logrotate {} \;

echo "✅ Daily maintenance completed"
```

#### Weekly Tasks
```bash
#!/bin/bash
# weekly-maintenance.sh

echo "🔄 Starting weekly maintenance..."

# Database maintenance
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "VACUUM ANALYZE;"
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "REINDEX DATABASE fhir_bridge;"

# Cache maintenance
docker-compose exec redis redis-cli --csv BGREWRITEAOF

# Security updates
docker-compose pull
docker-compose up -d

# Backup verification
./infra/db/backup/test-backup-restore.sh

echo "✅ Weekly maintenance completed"
```

#### Monthly Tasks
```bash
#!/bin/bash
# monthly-maintenance.sh

echo "🔄 Starting monthly maintenance..."

# Full database backup
./infra/db/backup/backup-database.sh --full-backup

# Security audit
./scripts/security-compliance-check.sh

# Performance review
./scripts/performance-analysis.sh

# Update dependencies
./mvnw versions:display-dependency-updates

echo "✅ Monthly maintenance completed"
```

### 2. Log Management

#### Log Rotation Configuration
```bash
# Create logrotate configuration
cat > /etc/logrotate.d/fhir-bridge << EOF
/app/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 fhir fhir
    postrotate
        docker-compose kill -s USR1 fhir-bridge
    endscript
}
EOF
```

#### Log Analysis
```bash
# Common log analysis commands
# Error analysis
grep -i error /app/logs/fhir-bridge.log | tail -20

# Performance analysis
grep "took.*ms" /app/logs/fhir-bridge.log | awk '{print $(NF-1)}' | sort -n | tail -10

# Security analysis
grep -i "unauthorized\|forbidden\|invalid.*token" /app/logs/fhir-bridge.log | tail -20
```

### 3. Performance Monitoring

#### Performance Metrics Collection
```bash
#!/bin/bash
# collect-metrics.sh

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
METRICS_FILE="/app/metrics/metrics_${TIMESTAMP}.json"

# Collect system metrics
cat > $METRICS_FILE << EOF
{
  "timestamp": "$(date -Iseconds)",
  "system": {
    "cpu_usage": $(top -bn1 | grep "Cpu(s)" | awk '{print $2}' | sed 's/%us,//'),
    "memory_usage": $(free | grep Mem | awk '{printf "%.2f", $3/$2 * 100.0}'),
    "disk_usage": $(df -h / | awk 'NR==2 {print $5}' | sed 's/%//')
  },
  "application": {
    "health": $(curl -s http://localhost:8081/actuator/health | jq -r '.status'),
    "uptime": $(curl -s http://localhost:8081/actuator/metrics/process.uptime | jq -r '.measurements[0].value'),
    "memory_used": $(curl -s http://localhost:8081/actuator/metrics/jvm.memory.used | jq -r '.measurements[0].value')
  }
}
EOF

# Upload to monitoring system
aws s3 cp $METRICS_FILE s3://fhir-bridge-metrics/
```

#### Performance Alerting
```bash
#!/bin/bash
# performance-alerts.sh

# Check response time
RESPONSE_TIME=$(curl -w "%{time_total}" -s -o /dev/null http://localhost:8080/api/health)
if (( $(echo "$RESPONSE_TIME > 2.0" | bc -l) )); then
  echo "⚠️  High response time: ${RESPONSE_TIME}s"
  # Send alert
fi

# Check error rate
ERROR_RATE=$(grep -c "ERROR" /app/logs/fhir-bridge.log | tail -100 | awk '{print $1/100}')
if (( $(echo "$ERROR_RATE > 0.05" | bc -l) )); then
  echo "⚠️  High error rate: ${ERROR_RATE}%"
  # Send alert
fi
```

## 🚨 Emergency Procedures

### 1. Service Outage Response

#### Immediate Response
```bash
#!/bin/bash
# emergency-response.sh

echo "🚨 Emergency response initiated..."

# 1. Check service status
if ! curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
  echo "❌ Service is down"
  
  # 2. Restart services
  docker-compose restart
  
  # 3. Wait for startup
  sleep 30
  
  # 4. Verify recovery
  if curl -f http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "✅ Service recovered"
  else
    echo "❌ Service still down - escalating"
    # Send emergency alert
  fi
fi
```

#### Rollback Procedures
```bash
#!/bin/bash
# rollback-deployment.sh

PREVIOUS_VERSION=$1

echo "🔄 Rolling back to version $PREVIOUS_VERSION..."

# Stop current services
docker-compose down

# Pull previous image
docker pull fhir-bridge:$PREVIOUS_VERSION

# Update docker-compose.yml
sed -i "s/image: fhir-bridge:.*/image: fhir-bridge:$PREVIOUS_VERSION/g" docker-compose.yml

# Restart services
docker-compose up -d

# Verify rollback
sleep 30
curl -f http://localhost:8081/actuator/health
```

### 2. Data Recovery Procedures

#### Database Recovery
```bash
#!/bin/bash
# database-recovery.sh

BACKUP_FILE=$1

echo "🔄 Starting database recovery..."

# Stop application
docker-compose stop fhir-bridge

# Create backup of current state
docker-compose exec postgres pg_dump -U fhir_user fhir_bridge > pre-recovery-backup.sql

# Restore from backup
./infra/db/backup/restore-database.sh --backup-file=$BACKUP_FILE

# Restart application
docker-compose start fhir-bridge

# Verify recovery
sleep 30
curl -f http://localhost:8081/actuator/health
```

#### Cache Recovery
```bash
#!/bin/bash
# cache-recovery.sh

echo "🔄 Starting cache recovery..."

# Clear Redis cache
docker-compose exec redis redis-cli FLUSHALL

# Restart Redis
docker-compose restart redis

# Warm up cache
./scripts/cache-warmup.sh
```

## 📊 Monitoring and Alerting

### 1. Health Check Endpoints

#### Application Health
```bash
# Basic health check
curl -f http://localhost:8081/actuator/health

# Detailed health check
curl http://localhost:8081/actuator/health | jq '.components'

# Liveness probe
curl -f http://localhost:8081/actuator/health/liveness

# Readiness probe
curl -f http://localhost:8081/actuator/health/readiness
```

### 2. Custom Health Checks

#### Database Health Check Script
```bash
#!/bin/bash
# health-check-db.sh

DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5432}
DB_NAME=${DB_NAME:-fhir_bridge}
DB_USERNAME=${DB_USERNAME:-fhir_user}
DB_PASSWORD=${DB_PASSWORD:-fhir_password}

# Test database connection
if pg_isready -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" -t 5; then
  echo "✅ Database is healthy"
  exit 0
else
  echo "❌ Database is unhealthy"
  exit 1
fi
```

#### Redis Health Check Script
```bash
#!/bin/bash
# health-check-redis.sh

REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}

# Test Redis connection
if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping | grep -q PONG; then
  echo "✅ Redis is healthy"
  exit 0
else
  echo "❌ Redis is unhealthy"
  exit 1
fi
```

## 📚 Troubleshooting Resources

### 1. Log Locations

#### Application Logs
- **Local**: `/app/logs/fhir-bridge.log`
- **Docker**: `docker-compose logs fhir-bridge`
- **AWS CloudWatch**: `/ecs/fhir-bridge`

#### Database Logs
- **PostgreSQL**: `/var/log/postgresql/postgresql-*.log`
- **Docker**: `docker-compose logs postgres`

#### System Logs
- **System**: `/var/log/syslog` or `/var/log/messages`
- **Docker**: `journalctl -u docker`

### 2. Useful Commands Reference

#### Docker Commands
```bash
# View container logs
docker-compose logs -f fhir-bridge

# Execute commands in container
docker-compose exec fhir-bridge bash

# Copy files from container
docker-compose cp fhir-bridge:/app/logs/fhir-bridge.log ./

# Restart specific service
docker-compose restart fhir-bridge
```

#### Database Commands
```bash
# Connect to database
docker-compose exec postgres psql -U fhir_user -d fhir_bridge

# Check database size
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT pg_size_pretty(pg_database_size('fhir_bridge'));"

# Check table sizes
docker-compose exec postgres psql -U fhir_user -d fhir_bridge -c "SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) FROM pg_tables WHERE schemaname NOT IN ('information_schema', 'pg_catalog') ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC LIMIT 10;"
```

#### Performance Commands
```bash
# JVM memory usage
docker-compose exec fhir-bridge jstat -gc 1

# Thread dump
docker-compose exec fhir-bridge jstack 1

# Heap dump
docker-compose exec fhir-bridge jmap -dump:format=b,file=/tmp/heap-dump.hprof 1

# GC logs
docker-compose exec fhir-bridge jstat -gcutil 1 5s
```

### 3. Support Contacts

#### Internal Support
- **Development Team**: dev-team@yourcompany.com
- **Operations Team**: ops-team@yourcompany.com
- **Security Team**: security@yourcompany.com

#### External Support
- **AWS Support**: AWS Support Center
- **Database Support**: PostgreSQL Community
- **Security Advisories**: CVE Database

## 🎯 Quick Reference Card

### Emergency Contacts
- **On-call Engineer**: +1-XXX-XXX-XXXX
- **Escalation Manager**: +1-XXX-XXX-XXXX
- **Security Incident**: security@yourcompany.com

### Key URLs
- **Health Check**: http://localhost:8081/actuator/health
- **Metrics**: http://localhost:8081/actuator/metrics
- **pgAdmin**: http://localhost:5050
- **Redis Commander**: http://localhost:8082

### Key Commands
```bash
# Quick health check
curl -f http://localhost:8081/actuator/health && echo "✅ OK" || echo "❌ DOWN"

# Restart services
docker-compose restart

# View logs
docker-compose logs -f --tail=100

# Database backup
./infra/db/backup/backup-database.sh

# Performance check
docker stats --no-stream