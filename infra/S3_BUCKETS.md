# S3 Buckets Configuration for FHIR Bridge

This document describes the S3 buckets created for the FHIR Bridge application and their HIPAA-compliant configuration.

## Overview

The FHIR Bridge application uses four S3 buckets for different purposes:

1. **Audit Logs Bucket** - Stores application audit logs for compliance
2. **Backups Bucket** - Stores database backups and application backups
3. **Artifacts Bucket** - Stores application artifacts, configurations, and deployment files
4. **Access Logs Bucket** - Stores S3 access logs for the other buckets

## Bucket Details

### 1. Audit Logs Bucket
- **Purpose**: Store comprehensive audit logs for HIPAA compliance
- **Naming**: `tefca-fhir-bridge-audit-logs-{random-suffix}`
- **Encryption**: KMS encryption with customer-managed key
- **Versioning**: Enabled
- **Lifecycle**: 
  - Standard → IA (30 days)
  - IA → Glacier (90 days)
  - Glacier → Deep Archive (365 days)
  - Delete non-current versions (90 days)

### 2. Backups Bucket
- **Purpose**: Store database backups and application configuration backups
- **Naming**: `tefca-fhir-bridge-backups-{random-suffix}`
- **Encryption**: KMS encryption with customer-managed key
- **Versioning**: Enabled
- **Lifecycle**:
  - Standard → IA (7 days)
  - IA → Glacier (30 days)
  - Retention: 7 years (2555 days) for compliance
  - Delete non-current versions (30 days)

### 3. Artifacts Bucket
- **Purpose**: Store application artifacts, JAR files, configuration templates
- **Naming**: `tefca-fhir-bridge-artifacts-{random-suffix}`
- **Encryption**: KMS encryption with customer-managed key
- **Versioning**: Enabled
- **Lifecycle**:
  - Keep latest 10 versions
  - Delete non-current versions (30 days)
  - Clean up incomplete multipart uploads (7 days)

### 4. Access Logs Bucket
- **Purpose**: Store S3 access logs for audit trail
- **Naming**: `tefca-fhir-bridge-access-logs-{random-suffix}`
- **Encryption**: AES256 server-side encryption
- **Lifecycle**: Delete logs after 90 days

## Security Features

### Encryption
- All buckets use encryption at rest
- Audit, Backups, and Artifacts buckets use KMS customer-managed keys
- Access logs bucket uses AES256 encryption
- Key rotation is enabled for KMS keys

### Access Control
- All buckets block public access
- Bucket policies enforce HTTPS-only access
- IAM roles provide least-privilege access
- ECS task role has specific permissions for each bucket

### Monitoring and Logging
- S3 access logging enabled for all primary buckets
- CloudWatch integration for monitoring
- Access logs stored in dedicated bucket

## Application Integration

### Environment Variables
The following environment variables are provided to the application:
- `S3_AUDIT_LOGS_BUCKET` - Name of the audit logs bucket
- `S3_BACKUPS_BUCKET` - Name of the backups bucket
- `S3_ARTIFACTS_BUCKET` - Name of the artifacts bucket
- `AWS_REGION` - AWS region (us-east-1)

### IAM Permissions
The ECS task role has the following S3 permissions:
- `s3:PutObject` - Upload files to buckets
- `s3:PutObjectAcl` - Set object ACLs
- `s3:GetObject` - Download files from buckets
- `s3:ListBucket` - List bucket contents
- `kms:Decrypt` - Decrypt KMS-encrypted objects
- `kms:GenerateDataKey` - Generate data keys for encryption

## Usage Examples

### Audit Logging
```java
// Example: Store audit log in S3
String bucketName = System.getenv("S3_AUDIT_LOGS_BUCKET");
String key = "audit-logs/" + LocalDate.now() + "/audit-" + UUID.randomUUID() + ".json";
s3Client.putObject(bucketName, key, auditLogJson);
```

### Database Backup
```java
// Example: Store database backup
String bucketName = System.getenv("S3_BACKUPS_BUCKET");
String key = "database-backups/" + LocalDate.now() + "/backup-" + timestamp + ".sql";
s3Client.putObject(bucketName, key, backupFile);
```

### Configuration Artifacts
```java
// Example: Store configuration file
String bucketName = System.getenv("S3_ARTIFACTS_BUCKET");
String key = "configurations/application-" + version + ".yml";
s3Client.putObject(bucketName, key, configurationFile);
```

## Compliance Considerations

### HIPAA Compliance
- All buckets are configured with encryption at rest
- Access logging provides audit trail
- Lifecycle policies ensure appropriate data retention
- Public access is blocked on all buckets
- HTTPS-only access is enforced

### Data Retention
- Audit logs: Transitioned to cheaper storage classes over time
- Backups: Retained for 7 years as required by healthcare regulations
- Artifacts: Version-controlled with automatic cleanup
- Access logs: Retained for 90 days

### Security Best Practices
- Customer-managed KMS keys for sensitive data
- Least-privilege IAM policies
- Bucket policies enforce secure transport
- Regular access pattern monitoring through CloudWatch

## Monitoring and Alerts

### CloudWatch Metrics
- Bucket size and object count
- Request metrics and error rates
- Data transfer metrics

### Recommended Alerts
- Unusual access patterns
- Failed authentication attempts
- Large data transfers
- Bucket policy violations

## Disaster Recovery

### Cross-Region Replication
Consider enabling cross-region replication for critical buckets in production:
- Audit logs should be replicated for compliance
- Backups should be replicated for disaster recovery
- Artifacts can be replicated for high availability

### Backup Strategy
- Database backups stored in S3 with lifecycle management
- Application configuration backed up to artifacts bucket
- Audit logs preserved with long-term retention
- Regular restore testing recommended

## Cost Optimization

### Storage Classes
- Automatic transition to cheaper storage classes
- Intelligent tiering for unpredictable access patterns
- Deep Archive for long-term retention

### Lifecycle Management
- Automatic deletion of old versions
- Cleanup of incomplete multipart uploads
- Transition policies based on access patterns

## Troubleshooting

### Common Issues
1. **Access Denied**: Check IAM permissions and bucket policies
2. **Encryption Errors**: Verify KMS key permissions
3. **Lifecycle Issues**: Check lifecycle policy configuration
4. **Cost Concerns**: Review storage class transitions

### Useful Commands
```bash
# List bucket contents
aws s3 ls s3://bucket-name/

# Check bucket policy
aws s3api get-bucket-policy --bucket bucket-name

# View lifecycle configuration
aws s3api get-bucket-lifecycle-configuration --bucket bucket-name

# Check encryption configuration
aws s3api get-bucket-encryption --bucket bucket-name
```