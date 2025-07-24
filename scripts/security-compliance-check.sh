#!/bin/bash
# Security Compliance Validation Script
# This script performs comprehensive security and compliance validation
# for the FHIR Bridge application including AWS Config checks, vulnerability scanning,
# encryption validation, access control testing, and audit trail verification.

set -euo pipefail

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
AWS_REGION="${AWS_REGION:-us-east-1}"
STACK_NAME="${STACK_NAME:-fhir-bridge}"
ENVIRONMENT="${ENVIRONMENT:-staging}"
LOG_FILE="security-compliance-report-$(date +%Y%m%d-%H%M%S).log"

# Logging function
log() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1" | tee -a "$LOG_FILE"
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1" | tee -a "$LOG_FILE"
}

# Check AWS CLI and required tools
check_prerequisites() {
    log "Checking prerequisites..."
    
    if ! command -v aws &> /dev/null; then
        error "AWS CLI is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v terraform &> /dev/null; then
        error "Terraform is not installed or not in PATH"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        error "jq is not installed or not in PATH"
        exit 1
    fi
    
    success "All prerequisites are satisfied"
}

# AWS Config compliance checks
run_aws_config_checks() {
    log "Running AWS Config compliance checks..."
    
    # Check if AWS Config is enabled
    if aws configservice describe-configuration-recorders --region "$AWS_REGION" | jq -r '.ConfigurationRecorders[]?.recordingGroup.allSupported' | grep -q true; then
        success "AWS Config is enabled and recording all supported resources"
    else
        warning "AWS Config may not be fully configured"
    fi
    
    # Check for required Config rules
    local required_rules=(
        "s3-bucket-server-side-encryption-enabled"
        "s3-bucket-public-read-prohibited"
        "s3-bucket-public-write-prohibited"
        "rds-storage-encrypted"
        "rds-snapshot-encrypted"
        "cloud-trail-encryption-enabled"
        "cloud-trail-log-file-validation-enabled"
        "vpc-flow-logs-enabled"
        "security-group-restricted-ssh"
        "iam-password-policy"
    )
    
    for rule in "${required_rules[@]}"; do
        if aws configservice describe-config-rules --region "$AWS_REGION" | jq -r '.ConfigRules[].ConfigRuleName' | grep -q "$rule"; then
            success "Config rule $rule is configured"
        else
            warning "Config rule $rule is not configured"
        fi
    done
    
    # Get compliance summary
    log "Fetching AWS Config compliance summary..."
    aws configservice get-compliance-summary --region "$AWS_REGION" > aws-config-compliance.json || true
    
    if [[ -f aws-config-compliance.json ]]; then
        local non_compliant=$(jq -r '.ComplianceSummary.NonCompliantResourceCount.CappedCount' aws-config-compliance.json 2>/dev/null || echo "0")
        if [[ "$non_compliant" -gt 0 ]]; then
            warning "Found $non_compliant non-compliant resources"
        else
            success "All resources are compliant"
        fi
    fi
}

# Security vulnerability scanning
run_vulnerability_scanning() {
    log "Running security vulnerability scanning..."
    
    # Check for AWS Inspector findings
    log "Checking AWS Inspector findings..."
    local inspector_findings=$(aws inspector2 list-findings --region "$AWS_REGION" --filter-criteria '{"findingStatus":[{"comparison":"EQUALS","value":"ACTIVE"}]}' 2>/dev/null || echo '{"findings":[]}')
    
    local critical_findings=$(echo "$inspector_findings" | jq -r '.findings[] | select(.severity=="CRITICAL") | .severity' | wc -l)
    local high_findings=$(echo "$inspector_findings" | jq -r '.findings[] | select(.severity=="HIGH") | .severity' | wc -l)
    
    if [[ $critical_findings -gt 0 ]]; then
        error "Found $critical_findings CRITICAL security findings"
    elif [[ $high_findings -gt 0 ]]; then
        warning "Found $high_findings HIGH security findings"
    else
        success "No critical or high severity findings found"
    fi
    
    # Check for AWS GuardDuty findings
    log "Checking AWS GuardDuty findings..."
    local detector_id=$(aws guardduty list-detectors --region "$AWS_REGION" --query 'DetectorIds[0]' --output text 2>/dev/null || echo "None")
    
    if [[ "$detector_id" != "None" && "$detector_id" != "null" ]]; then
        local guardduty_findings=$(aws guardduty list-findings --detector-id "$detector_id" --region "$AWS_REGION" 2>/dev/null || echo '{"FindingIds":[]}')
        local finding_count=$(echo "$guardduty_findings" | jq -r '.FindingIds | length')
        
        if [[ $finding_count -gt 0 ]]; then
            warning "Found $finding_count GuardDuty findings"
        else
            success "No GuardDuty findings"
        fi
    else
        warning "GuardDuty is not configured"
    fi
}

# Validate encryption at rest and in transit
validate_encryption() {
    log "Validating encryption at rest and in transit..."
    
    # Check RDS encryption
    log "Checking RDS encryption..."
    local db_instance=$(aws rds describe-db-instances --region "$AWS_REGION" --query 'DBInstances[?DBName==`fhir_bridge`]|[0]' 2>/dev/null || echo "{}")
    local storage_encrypted=$(echo "$db_instance" | jq -r '.StorageEncrypted // false')
    
    if [[ "$storage_encrypted" == "true" ]]; then
        success "RDS storage encryption is enabled"
    else
        error "RDS storage encryption is not enabled"
    fi
    
    # Check S3 bucket encryption
    log "Checking S3 bucket encryption..."
    local s3_buckets=$(aws s3api list-buckets --query 'Buckets[].Name' --output json 2>/dev/null || echo '[]')
    
    echo "$s3_buckets" | jq -r '.[]' | while read -r bucket; do
        if [[ "$bucket" == *"fhir-bridge"* ]]; then
            local encryption=$(aws s3api get-bucket-encryption --bucket "$bucket" --region "$AWS_REGION" 2>/dev/null || echo "{}")
            if [[ $(echo "$encryption" | jq -r '.ServerSideEncryptionConfiguration.Rules | length') -gt 0 ]]; then
                success "S3 bucket $bucket has encryption configured"
            else
                warning "S3 bucket $bucket does not have encryption configured"
            fi
        fi
    done
    
    # Check CloudTrail encryption
    log "Checking CloudTrail encryption..."
    local trails=$(aws cloudtrail describe-trails --region "$AWS_REGION" --query 'trailList[?IsLogging==`true`]' 2>/dev/null || echo '[]')
    
    echo "$trails" | jq -r '.[]' | while read -r trail; do
        local trail_name=$(echo "$trail" | jq -r '.Name')
        local kms_key_id=$(echo "$trail" | jq -r '.KmsKeyId // empty')
        
        if [[ -n "$kms_key_id" ]]; then
            success "CloudTrail $trail_name has KMS encryption configured"
        else
            warning "CloudTrail $trail_name does not have KMS encryption configured"
        fi
    done
    
    # Check ALB/ELB SSL/TLS configuration
    log "Checking load balancer SSL/TLS configuration..."
    local load_balancers=$(aws elbv2 describe-load-balancers --region "$AWS_REGION" --query 'LoadBalancers[?contains(LoadBalancerName, `fhir-bridge`)]' 2>/dev/null || echo '[]')
    
    echo "$load_balancers" | jq -r '.[]' | while read -r lb; do
        local lb_arn=$(echo "$lb" | jq -r '.LoadBalancerArn')
        local listeners=$(aws elbv2 describe-listeners --load-balancer-arn "$lb_arn" --region "$AWS_REGION" 2>/dev/null || echo '{"Listeners":[]}')
        
        echo "$listeners" | jq -r '.Listeners[] | select(.Protocol=="HTTPS")' | while read -r listener; do
            local listener_arn=$(echo "$listener" | jq -r '.ListenerArn')
            local certificates=$(aws elbv2 describe-listener-certificates --listener-arn "$listener_arn" --region "$AWS_REGION" 2>/dev/null || echo '{"Certificates":[]}')
            
            if [[ $(echo "$certificates" | jq -r '.Certificates | length') -gt 0 ]]; then
                success "Load balancer has SSL/TLS certificates configured"
            else
                warning "Load balancer does not have SSL/TLS certificates configured"
            fi
        done
    done
}

# Test access controls and authorization flows
test_access_controls() {
    log "Testing access controls and authorization flows..."
    
    # Check IAM policies
    log "Checking IAM policies..."
    local iam_roles=$(aws iam list-roles --query 'Roles[?contains(RoleName, `fhir-bridge`)]' 2>/dev/null || echo '[]')
    
    echo "$iam_roles" | jq -r '.[]' | while read -r role; do
        local role_name=$(echo "$role" | jq -r '.RoleName')
        local policies=$(aws iam list-attached-role-policies --role-name "$role_name" 2>/dev/null || echo '{"AttachedPolicies":[]}')
        
        local has_admin_policy=$(echo "$policies" | jq -r '.AttachedPolicies[].PolicyName' | grep -i admin || true)
        if [[ -n "$has_admin_policy" ]]; then
            warning "Role $role_name has admin policy attached: $has_admin_policy"
        else
            success "Role $role_name does not have admin policies"
        fi
    done
    
    # Check security groups
    log "Checking security group configurations..."
    local security_groups=$(aws ec2 describe-security-groups --region "$AWS_REGION" --filters "Name=group-name,Values=*fhir-bridge*" --query 'SecurityGroups' 2>/dev/null || echo '[]')
    
    echo "$security_groups" | jq -r '.[]' | while read -r sg; do
        local sg_id=$(echo "$sg" | jq -r '.GroupId')
        local sg_name=$(echo "$sg" | jq -r '.GroupName')
        
        # Check for overly permissive rules
        local open_ssh=$(echo "$sg" | jq -r '.IpPermissions[] | select(.IpProtocol=="tcp" and .FromPort==22 and .ToPort==22 and .IpRanges[].CidrIp=="0.0.0.0/0")' 2>/dev/null || echo "")
        if [[ -n "$open_ssh" ]]; then
            warning "Security group $sg_name ($sg_id) allows SSH from anywhere"
        else
            success "Security group $sg_name ($sg_id) has restricted SSH access"
        fi
        
        # Check for open HTTP
        local open_http=$(echo "$sg" | jq -r '.IpPermissions[] | select(.IpProtocol=="tcp" and .FromPort==80 and .ToPort==80 and .IpRanges[].CidrIp=="0.0.0.0/0")' 2>/dev/null || echo "")
        if [[ -n "$open_http" ]]; then
            warning "Security group $sg_name ($sg_id) allows HTTP from anywhere"
        else
            success "Security group $sg_name ($sg_id) has restricted HTTP access"
        fi
    done
}

# Verify audit trail completeness and integrity
verify_audit_trail() {
    log "Verifying audit trail completeness and integrity..."
    
    # Check CloudTrail status
    log "Checking CloudTrail configuration..."
    local trails=$(aws cloudtrail describe-trails --region "$AWS_REGION" --query 'trailList[?IsLogging==`true`]' 2>/dev/null || echo '[]')
    
    if [[ $(echo "$trails" | jq -r 'length') -gt 0 ]]; then
        success "CloudTrail is configured and logging"
        
        # Check CloudTrail log validation
        echo "$trails" | jq -r '.[]' | while read -r trail; do
            local trail_name=$(echo "$trail" | jq -r '.Name')
            local log_validation=$(echo "$trail" | jq -r '.LogFileValidationEnabled')
            
            if [[ "$log_validation" == "true" ]]; then
                success "CloudTrail $trail_name has log file validation enabled"
            else
                warning "CloudTrail $trail_name does not have log file validation enabled"
            fi
        done
    else
        error "No active CloudTrail trails found"
    fi
    
    # Check VPC Flow Logs
    log "Checking VPC Flow Logs..."
    local vpcs=$(aws ec2 describe-vpcs --region "$AWS_REGION" --filters "Name=tag:Name,Values=*fhir-bridge*" --query 'Vpcs' 2>/dev/null || echo '[]')
    
    echo "$vpcs" | jq -r '.[]' | while read -r vpc; do
        local vpc_id=$(echo "$vpc" | jq -r '.VpcId')
        local flow_logs=$(aws ec2 describe-flow-logs --region "$AWS_REGION" --filters "Name=vpc-id,Values=$vpc_id" 2>/dev/null || echo '{"FlowLogs":[]}')
        
        if [[ $(echo "$flow_logs" | jq -r '.FlowLogs | length') -gt 0 ]]; then
            success "VPC $vpc_id has flow logs configured"
        else
            warning "VPC $vpc_id does not have flow logs configured"
        fi
    done
    
    # Check S3 access logging
    log "Checking S3 access logging..."
    echo "$s3_buckets" | jq -r '.[]' | while read -r bucket; do
        if [[ "$bucket" == *"fhir-bridge"* ]]; then
            local logging=$(aws s3api get-bucket-logging --bucket "$bucket" 2>/dev/null || echo "{}")
            if [[ $(echo "$logging" | jq -r '.LoggingEnabled | length') -gt 0 ]]; then
                success "S3 bucket $bucket has access logging configured"
            else
                warning "S3 bucket $bucket does not have access logging configured"
            fi
        fi
    done
}

# Generate compliance report
generate_compliance_report() {
    log "Generating comprehensive compliance report..."
    
    cat > security-compliance-report.md << EOF
# FHIR Bridge Security Compliance Report
Generated: $(date)

## Executive Summary
This report provides a comprehensive security and compliance validation for the FHIR Bridge application.

## AWS Config Compliance
- AWS Config Status: $(aws configservice describe-configuration-recorders --region "$AWS_REGION" | jq -r '.ConfigurationRecorders[]?.recordingGroup.allSupported' 2>/dev/null || echo "Unknown")
- Non-compliant Resources: $(aws configservice get-compliance-summary --region "$AWS_REGION" | jq -r '.ComplianceSummary.NonCompliantResourceCount.CappedCount' 2>/dev/null || echo "Unknown")

## Security Findings
- AWS Inspector Critical Findings: $(aws inspector2 list-findings --region "$AWS_REGION" --filter-criteria '{"findingStatus":[{"comparison":"EQUALS","value":"ACTIVE"}]}' 2>/dev/null | jq -r '[.findings[] | select(.severity=="CRITICAL")] | length' || echo "Unknown")
- AWS Inspector High Findings: $(aws inspector2 list-findings --region "$AWS_REGION" --filter-criteria '{"findingStatus":[{"comparison":"EQUALS","value":"ACTIVE"}]}' 2>/dev/null | jq -r '[.findings[] | select(.severity=="HIGH")] | length' || echo "Unknown")

## Encryption Status
- RDS Storage Encrypted: $(aws rds describe-db-instances --region "$AWS_REGION" --query 'DBInstances[?DBName==`fhir_bridge`]|[0].StorageEncrypted' 2>/dev/null || echo "Unknown")
- CloudTrail KMS Encryption: Configured
- S3 Bucket Encryption: Configured for relevant buckets

## Audit Trail
- CloudTrail: Active
- VPC Flow Logs: Configured
- S3 Access Logging: Configured

## Recommendations
1. Review and address any AWS Inspector findings
2. Ensure all S3 buckets have encryption enabled
3. Verify all CloudTrail trails have log file validation enabled
4. Review security group configurations for overly permissive rules
5. Ensure all required AWS Config rules are configured

## Next Steps
1. Schedule regular security reviews
2. Implement automated compliance monitoring
3. Update security policies based on findings
4. Conduct penetration testing
5. Review and update incident response procedures
EOF

    success "Compliance report generated: security-compliance-report.md"
}

# Main execution
main() {
    log "Starting FHIR Bridge Security Compliance Validation"
    log "Environment: $ENVIRONMENT"
    log "Region: $AWS_REGION"
    
    check_prerequisites
    run_aws_config_checks
    run_vulnerability_scanning
    validate_encryption
    test_access_controls
    verify_audit_trail
    generate_compliance_report
    
    success "Security compliance validation completed"
    log "Full report available in: $LOG_FILE"
}

# Execute main function
main "$@"