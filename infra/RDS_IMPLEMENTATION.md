# RDS PostgreSQL Implementation

## Overview
Added RDS PostgreSQL instance with encryption at rest and automated backups to the FHIR Bridge infrastructure.

## Components Added

### 1. Private Subnets for Database
- `aws_subnet.private_db_1` - Private subnet in us-east-1a (10.0.2.0/24)
- `aws_subnet.private_db_2` - Private subnet in us-east-1b (10.0.3.0/24)

### 2. Security Groups
- `aws_security_group.rds` - Database security group allowing PostgreSQL access (port 5432) from application
- `aws_security_group.app` - Application security group for ECS tasks

### 3. Database Subnet Group
- `aws_db_subnet_group.main` - DB subnet group spanning multiple AZs for high availability

### 4. KMS Encryption
- `aws_kms_key.rds` - Customer-managed KMS key for RDS encryption
- `aws_kms_alias.rds` - Alias for the KMS key (alias/tefca-rds)

### 5. RDS PostgreSQL Instance
- **Engine**: PostgreSQL 15.4
- **Instance Class**: db.t3.micro (suitable for development)
- **Storage**: 20GB GP3 with auto-scaling up to 100GB
- **Encryption**: Enabled with customer-managed KMS key
- **Backups**: 7-day retention with automated backups
- **Monitoring**: Enhanced monitoring with 60-second intervals
- **Performance Insights**: Enabled with KMS encryption
- **CloudWatch Logs**: PostgreSQL logs exported to CloudWatch

### 6. IAM Role for Monitoring
- `aws_iam_role.rds_monitoring` - IAM role for RDS enhanced monitoring
- Attached AWS managed policy for RDS monitoring

## Security Features

### Encryption at Rest
- Storage encrypted using customer-managed KMS key
- Performance Insights encrypted with the same KMS key

### Network Security
- Database deployed in private subnets (no public access)
- Security group restricts access to application layer only
- Multi-AZ deployment capability (currently disabled for development)

### Backup Configuration
- Automated backups with 7-day retention period
- Backup window: 03:00-04:00 UTC
- Maintenance window: Sunday 04:00-05:00 UTC

## Outputs
- `rds_endpoint` - Database connection endpoint
- `rds_port` - Database port (5432)
- `database_name` - Database name (fhirbridge)
- `kms_key_id` - KMS key ID for encryption

## Production Considerations
The following settings should be updated for production:
- Set `multi_az = true` for high availability
- Set `deletion_protection = true` to prevent accidental deletion
- Set `skip_final_snapshot = false` to create final snapshot on deletion
- Replace hardcoded password with AWS Secrets Manager reference
- Consider larger instance class based on performance requirements

## Connection Information
- **Host**: Use the `rds_endpoint` output value
- **Port**: 5432
- **Database**: fhirbridge
- **Username**: fhirbridge_admin
- **Password**: Should be stored in AWS Secrets Manager (currently hardcoded for development)