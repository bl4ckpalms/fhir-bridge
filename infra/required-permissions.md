# Required AWS IAM Policies for FHIR Bridge Deployment

## Current Permission Issues

Based on the terraform plan errors, the IAM user `User_1` is missing these critical permissions:
- `elasticloadbalancing:DescribeLoadBalancerAttributes`
- `elasticloadbalancing:DescribeTargetGroupAttributes`
- ElastiCache permissions for Redis cluster management

## AWS Managed Policies to Attach to Your User

**REQUIRED - These must be added to resolve current errors:**

1. **AmazonVPCFullAccess** - For VPC, subnets, security groups
2. **IAMFullAccess** - For creating IAM roles and policies  
3. **AmazonRDSFullAccess** - For RDS database creation
4. **ElastiCacheFullAccess** - For Redis cluster ⚠️ **MISSING - CAUSES ERRORS**
5. **AmazonECS_FullAccess** - For ECS cluster and services
6. **AmazonEC2ContainerRegistryFullAccess** - For ECR repository
7. **SecretsManagerReadWrite** - For storing credentials
8. **CloudWatchFullAccess** - For logging and monitoring
9. **AWSKeyManagementServicePowerUser** - For KMS encryption keys
10. **ElasticLoadBalancingFullAccess** - For Application Load Balancer ⚠️ **MISSING - CAUSES ERRORS**
11. **AmazonS3FullAccess** - For S3 buckets (audit logs, backups, artifacts)

## Alternative: Create Custom Policy

If you prefer more restrictive permissions, create a custom policy with these actions:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "ec2:*",
                "iam:*",
                "rds:*",
                "elasticache:*",
                "ecs:*",
                "ecr:*",
                "secretsmanager:*",
                "logs:*",
                "kms:*",
                "elasticloadbalancing:*",
                "application-autoscaling:*",
                "s3:*",
                "sts:GetCallerIdentity"
            ],
            "Resource": "*"
        }
    ]
}
```

## Specific Missing Permissions Causing Current Errors

The terraform plan is failing because `User_1` lacks these specific permissions:

**Elastic Load Balancing:**
- `elasticloadbalancing:DescribeLoadBalancerAttributes`
- `elasticloadbalancing:DescribeTargetGroupAttributes`
- `elasticloadbalancing:DescribeLoadBalancers`
- `elasticloadbalancing:DescribeTargetGroups`

**ElastiCache (Redis):**
- `elasticache:DescribeReplicationGroups`
- `elasticache:DescribeCacheClusters`
- `elasticache:DescribeSubnetGroups`

**S3 (for new buckets):**
- `s3:CreateBucket`
- `s3:PutBucketPolicy`
- `s3:PutBucketEncryption`
- `s3:PutBucketVersioning`

## Steps to Add Missing Permissions (URGENT)

**To resolve current terraform errors, you MUST add these policies:**

1. Go to AWS Console → IAM
2. Click "Users" → Find "User_1"
3. Click "Add permissions"
4. Select "Attach policies directly"
5. **Search and attach these MISSING policies:**
   - `ElasticLoadBalancingFullAccess`
   - `ElastiCacheFullAccess`
   - `AmazonS3FullAccess`
6. Click "Add permissions"

## Quick Fix Command

If you have AWS CLI configured with admin access, run:
```bash
aws iam attach-user-policy --user-name User_1 --policy-arn arn:aws:iam::aws:policy/ElasticLoadBalancingFullAccess
aws iam attach-user-policy --user-name User_1 --policy-arn arn:aws:iam::aws:policy/ElastiCacheFullAccess
aws iam attach-user-policy --user-name User_1 --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess
```

## Verify Permissions

After adding permissions, test with:
```bash
aws sts get-caller-identity
aws iam list-attached-user-policies --user-name User_1
terraform plan  # Should now work without permission errors
```

## Why These Permissions Are Required

- **ElasticLoadBalancingFullAccess**: The infrastructure includes an Application Load Balancer and Target Groups that terraform needs to read/manage
- **ElastiCacheFullAccess**: The infrastructure includes a Redis cluster for caching that terraform needs to manage
- **AmazonS3FullAccess**: The new S3 buckets for audit logs, backups, and artifacts require S3 permissions

Without these permissions, terraform cannot read the current state of existing resources, causing the plan/apply operations to fail.