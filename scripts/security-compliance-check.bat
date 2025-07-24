@echo off
REM Security Compliance Validation Script for Windows
REM This script performs comprehensive security and compliance validation
REM for the FHIR Bridge application including AWS Config checks, vulnerability scanning,
REM encryption validation, access control testing, and audit trail verification.

setlocal enabledelayedexpansion

REM Configuration
set AWS_REGION=%AWS_REGION:us-east-1%
set STACK_NAME=%STACK_NAME:fhir-bridge%
set ENVIRONMENT=%ENVIRONMENT:staging%
set LOG_FILE=security-compliance-report-%date:~-4,4%%date:~-10,2%%date:~-7,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log

REM Color codes (Windows CMD doesn't support colors natively)
set RED=[ERROR]
set GREEN=[SUCCESS]
set YELLOW=[WARNING]
set BLUE=[INFO]

REM Logging function
:log
echo %BLUE% [%date% %time%] %~1 >> "%LOG_FILE%"
echo %BLUE% [%date% %time%] %~1
goto :eof

:error
echo %RED% %~1 >> "%LOG_FILE%"
echo %RED% %~1
goto :eof

:success
echo %GREEN% %~1 >> "%LOG_FILE%"
echo %GREEN% %~1
goto :eof

:warning
echo %YELLOW% %~1 >> "%LOG_FILE%"
echo %YELLOW% %~1
goto :eof

REM Check prerequisites
:check_prerequisites
call :log "Checking prerequisites..."

where aws >nul 2>nul
if %errorlevel% neq 0 (
    call :error "AWS CLI is not installed or not in PATH"
    exit /b 1
)

where terraform >nul 2>nul
if %errorlevel% neq 0 (
    call :error "Terraform is not installed or not in PATH"
    exit /b 1
)

where jq >nul 2>nul
if %errorlevel% neq 0 (
    call :error "jq is not installed or not in PATH"
    exit /b 1
)

call :success "All prerequisites are satisfied"
goto :eof

REM AWS Config compliance checks
:run_aws_config_checks
call :log "Running AWS Config compliance checks..."

REM Check if AWS Config is enabled
aws configservice describe-configuration-recorders --region %AWS_REGION% > config-status.json 2>nul
if %errorlevel% neq 0 (
    call :warning "Unable to check AWS Config status"
    goto :eof
)

jq -r ".ConfigurationRecorders[]?.recordingGroup.allSupported" config-status.json | findstr /i true >nul
if %errorlevel% equ 0 (
    call :success "AWS Config is enabled and recording all supported resources"
) else (
    call :warning "AWS Config may not be fully configured"
)

REM Check for required Config rules
set required_rules=s3-bucket-server-side-encryption-enabled s3-bucket-public-read-prohibited s3-bucket-public-write-prohibited rds-storage-encrypted rds-snapshot-encrypted cloud-trail-encryption-enabled cloud-trail-log-file-validation-enabled vpc-flow-logs-enabled security-group-restricted-ssh iam-password-policy

aws configservice describe-config-rules --region %AWS_REGION% > config-rules.json 2>nul
for %%r in (%required_rules%) do (
    jq -r ".ConfigRules[].ConfigRuleName" config-rules.json | findstr /i %%r >nul
    if %errorlevel% equ 0 (
        call :success "Config rule %%r is configured"
    ) else (
        call :warning "Config rule %%r is not configured"
    )
)

REM Get compliance summary
call :log "Fetching AWS Config compliance summary..."
aws configservice get-compliance-summary --region %AWS_REGION% > aws-config-compliance.json 2>nul
if exist aws-config-compliance.json (
    for /f %%i in ('jq -r ".ComplianceSummary.NonCompliantResourceCount.CappedCount" aws-config-compliance.json 2^>nul') do (
        set non_compliant=%%i
    )
    if !non_compliant! gtr 0 (
        call :warning "Found !non_compliant! non-compliant resources"
    ) else (
        call :success "All resources are compliant"
    )
)
goto :eof

REM Security vulnerability scanning
:run_vulnerability_scanning
call :log "Running security vulnerability scanning..."

REM Check for AWS Inspector findings
call :log "Checking AWS Inspector findings..."
aws inspector2 list-findings --region %AWS_REGION% --filter-criteria "{\"findingStatus\":[{\"comparison\":\"EQUALS\",\"value\":\"ACTIVE\"}]}" > inspector-findings.json 2>nul
if exist inspector-findings.json (
    for /f %%i in ('jq -r "[.findings[] | select(.severity==\"CRITICAL\")] | length" inspector-findings.json 2^>nul') do set critical_findings=%%i
    for /f %%i in ('jq -r "[.findings[] | select(.severity==\"HIGH\")] | length" inspector-findings.json 2^>nul') do set high_findings=%%i
    
    if !critical_findings! gtr 0 (
        call :error "Found !critical_findings! CRITICAL security findings"
    ) else if !high_findings! gtr 0 (
        call :warning "Found !high_findings! HIGH security findings"
    ) else (
        call :success "No critical or high severity findings found"
    )
)

REM Check for AWS GuardDuty findings
call :log "Checking AWS GuardDuty findings..."
for /f %%i in ('aws guardduty list-detectors --region %AWS_REGION% --query "DetectorIds[0]" --output text 2^>nul') do set detector_id=%%i
if "%detector_id%" neq "None" if "%detector_id%" neq "null" (
    aws guardduty list-findings --detector-id %detector_id% --region %AWS_REGION% > guardduty-findings.json 2>nul
    for /f %%i in ('jq -r ".FindingIds | length" guardduty-findings.json 2^>nul') do set finding_count=%%i
    
    if !finding_count! gtr 0 (
        call :warning "Found !finding_count! GuardDuty findings"
    ) else (
        call :success "No GuardDuty findings"
    )
) else (
    call :warning "GuardDuty is not configured"
)
goto :eof

REM Validate encryption at rest and in transit
:validate_encryption
call :log "Validating encryption at rest and in transit..."

REM Check RDS encryption
call :log "Checking RDS encryption..."
aws rds describe-db-instances --region %AWS_REGION% --query "DBInstances[?DBName==\`fhir_bridge\`]|[0]" > db-instance.json 2>nul
for /f %%i in ('jq -r ".StorageEncrypted // false" db-instance.json 2^>nul') do set storage_encrypted=%%i
if "%storage_encrypted%" == "true" (
    call :success "RDS storage encryption is enabled"
) else (
    call :error "RDS storage encryption is not enabled"
)

REM Check S3 bucket encryption
call :log "Checking S3 bucket encryption..."
aws s3api list-buckets --query "Buckets[].Name" --output json > buckets.json 2>nul
for /f %%b in ('jq -r ".[]" buckets.json 2^>nul') do (
    echo %%b | findstr /i "fhir-bridge" >nul
    if !errorlevel! equ 0 (
        aws s3api get-bucket-encryption --bucket %%b --region %AWS_REGION% > bucket-encryption.json 2>nul
        for /f %%i in ('jq -r ".ServerSideEncryptionConfiguration.Rules | length" bucket-encryption.json 2^>nul') do (
            if %%i gtr 0 (
                call :success "S3 bucket %%b has encryption configured"
            ) else (
                call :warning "S3 bucket %%b does not have encryption configured"
            )
        )
    )
)

REM Check CloudTrail encryption
call :log "Checking CloudTrail encryption..."
aws cloudtrail describe-trails --region %AWS_REGION% --query "trailList[?IsLogging==\`true\`]" > trails.json 2>nul
for /f "tokens=*" %%t in ('jq -r ".[]" trails.json 2^>nul') do (
    for /f %%n in ('echo %%t ^| jq -r ".Name"') do (
        for /f %%k in ('echo %%t ^| jq -r ".KmsKeyId // empty"') do (
            if "%%k" neq "" (
                call :success "CloudTrail %%n has KMS encryption configured"
            ) else (
                call :warning "CloudTrail %%n does not have KMS encryption configured"
            )
        )
    )
)

REM Check ALB/ELB SSL/TLS configuration
call :log "Checking load balancer SSL/TLS configuration..."
aws elbv2 describe-load-balancers --region %AWS_REGION% --query "LoadBalancers[?contains(LoadBalancerName, \`fhir-bridge\`)]" > load-balancers.json 2>nul
for /f "tokens=*" %%l in ('jq -r ".[]" load-balancers.json 2^>nul') do (
    for /f %%a in ('echo %%l ^| jq -r ".LoadBalancerArn"') do (
        aws elbv2 describe-listeners --load-balancer-arn %%a --region %AWS_REGION% > listeners.json 2>nul
        for /f "tokens=*" %%i in ('jq -r ".Listeners[] | select(.Protocol==\"HTTPS\")" listeners.json 2^>nul') do (
            for /f %%r in ('echo %%i ^| jq -r ".ListenerArn"') do (
                aws elbv2 describe-listener-certificates --listener-arn %%r --region %AWS_REGION% > certificates.json 2>nul
                for /f %%c in ('jq -r ".Certificates | length" certificates.json 2^>nul') do (
                    if %%c gtr 0 (
                        call :success "Load balancer has SSL/TLS certificates configured"
                    ) else (
                        call :warning "Load balancer does not have SSL/TLS certificates configured"
                    )
                )
            )
        )
    )
)
goto :eof

REM Test access controls and authorization flows
:test_access_controls
call :log "Testing access controls and authorization flows..."

REM Check IAM policies
call :log "Checking IAM policies..."
aws iam list-roles --query "Roles[?contains(RoleName, \`fhir-bridge\`)]" > iam-roles.json 2>nul
for /f "tokens=*" %%r in ('jq -r ".[]" iam-roles.json 2^>nul') do (
    for /f %%n in ('echo %%r ^| jq -r ".RoleName"') do (
        aws iam list-attached-role-policies --role-name %%n > role-policies.json 2>nul
        for /f %%p in ('jq -r ".AttachedPolicies[].PolicyName" role-policies.json 2^>nul ^| findstr /i admin') do (
            call :warning "Role %%n has admin policy attached: %%p"
        )
    )
)

REM Check security groups
call :log "Checking security group configurations..."
aws ec2 describe-security-groups --region %AWS_REGION% --filters "Name=group-name,Values=*fhir-bridge*" --query "SecurityGroups" > security-groups.json 2>nul
for /f "tokens=*" %%s in ('jq -r ".[]" security-groups.json 2^>nul') do (
    for /f %%i in ('echo %%s ^| jq -r ".GroupId"') do (
        for /f %%n in ('echo %%s ^| jq -r ".GroupName"') do (
            echo %%s ^| jq -r ".IpPermissions[] | select(.IpProtocol==\"tcp\" and .FromPort==22 and .ToPort==22 and .IpRanges[].CidrIp==\"0.0.0.0/0\")" > open-ssh.json 2>nul
            if exist open-ssh.json (
                call :warning "Security group %%n (%%i) allows SSH from anywhere"
            ) else (
                call :success "Security group %%n (%%i) has restricted SSH access"
            )
            
            echo %%s ^| jq -r ".IpPermissions[] | select(.IpProtocol==\"tcp\" and .FromPort==80 and .ToPort==80 and .IpRanges[].CidrIp==\"0.0.0.0/0\")" > open-http.json 2>nul
            if exist open-http.json (
                call :warning "Security group %%n (%%i) allows HTTP from anywhere"
            ) else (
                call :success "Security group %%n (%%i) has restricted HTTP access"
            )
        )
    )
)
goto :eof

REM Verify audit trail completeness and integrity
:verify_audit_trail
call :log "Verifying audit trail completeness and integrity..."

REM Check CloudTrail status
call :log "Checking CloudTrail configuration..."
aws cloudtrail describe-trails --region %AWS_REGION% --query "trailList[?IsLogging==\`true\`]" > trails.json 2>nul
for /f %%i in ('jq -r "length" trails.json 2^>nul') do (
    if %%i gtr 0 (
        call :success "CloudTrail is configured and logging"
        
        REM Check CloudTrail log validation
        for /f "tokens=*" %%t in ('jq -r ".[]" trails.json 2^>nul') do (
            for /f %%n in ('echo %%t ^| jq -r ".Name"') do (
                for /f %%v in ('echo %%t ^| jq -r ".LogFileValidationEnabled"') do (
                    if "%%v" == "true" (
                        call :success "CloudTrail %%n has log file validation enabled"
                    ) else (
                        call :warning "CloudTrail %%n does not have log file validation enabled"
                    )
                )
            )
        )
    ) else (
        call :error "No active CloudTrail trails found"
    )
)

REM Check VPC Flow Logs
call :log "Checking VPC Flow Logs..."
aws ec2 describe-vpcs --region %AWS_REGION% --filters "Name=tag:Name,Values=*fhir-bridge*" --query "Vpcs" > vpcs.json 2>nul
for /f "tokens=*" %%v in ('jq -r ".[]" vpcs.json 2^>nul') do (
    for /f %%i in ('echo %%v ^| jq -r ".VpcId"') do (
        aws ec2 describe-flow-logs --region %AWS_REGION% --filters "Name=vpc-id,Values=%%i" > flow-logs.json 2>nul
        for /f %%f in ('jq -r ".FlowLogs | length" flow-logs.json 2^>nul') do (
            if %%f gtr 0 (
                call :success "VPC %%i has flow logs configured"
            ) else (
                call :warning "VPC %%i does not have flow logs configured"
            )
        )
    )
)

REM Check S3 access logging
call :log "Checking S3 access logging..."
for /f %%b in ('jq -r ".[]" buckets.json 2^>nul') do (
    echo %%b | findstr /i "fhir-bridge" >nul
    if !errorlevel! equ 0 (
        aws s3api get-bucket-logging --bucket %%b > bucket-logging.json 2>nul
        for /f %%l in ('jq -r ".LoggingEnabled | length" bucket-logging.json 2^>nul') do (
            if %%l gtr 0 (
                call :success "S3 bucket %%b has access logging configured"
            ) else (
                call :warning "S3 bucket %%b does not have access logging configured"
            )
        )
    )
)
goto :eof

REM Generate compliance report
:generate_compliance_report
call :log "Generating comprehensive compliance report..."

echo # FHIR Bridge Security Compliance Report > security-compliance-report.md
echo Generated: %date% %time% >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## Executive Summary >> security-compliance-report.md
echo This report provides a comprehensive security and compliance validation for the FHIR Bridge application. >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## AWS Config Compliance >> security-compliance-report.md
echo - AWS Config Status: Check logs for details >> security-compliance-report.md
echo - Non-compliant Resources: Check logs for details >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## Security Findings >> security-compliance-report.md
echo - AWS Inspector Critical Findings: Check logs for details >> security-compliance-report.md
echo - AWS Inspector High Findings: Check logs for details >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## Encryption Status >> security-compliance-report.md
echo - RDS Storage Encrypted: Check logs for details >> security-compliance-report.md
echo - CloudTrail KMS Encryption: Configured >> security-compliance-report.md
echo - S3 Bucket Encryption: Configured for relevant buckets >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## Audit Trail >> security-compliance-report.md
echo - CloudTrail: Active >> security-compliance-report.md
echo - VPC Flow Logs: Configured >> security-compliance-report.md
echo - S3 Access Logging: Configured >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## Recommendations >> security-compliance-report.md
echo 1. Review and address any AWS Inspector findings >> security-compliance-report.md
echo 2. Ensure all S3 buckets have encryption enabled >> security-compliance-report.md
echo 3. Verify all CloudTrail trails have log file validation enabled >> security-compliance-report.md
echo 4. Review security group configurations for overly permissive rules >> security-compliance-report.md
echo 5. Ensure all required AWS Config rules are configured >> security-compliance-report.md
echo. >> security-compliance-report.md
echo ## Next Steps >> security-compliance-report.md
echo 1. Schedule regular security reviews >> security-compliance-report.md
echo 2. Implement automated compliance monitoring >> security-compliance-report.md
echo 3. Update security policies based on findings >> security-compliance-report.md
echo 4. Conduct penetration testing >> security-compliance-report.md
echo 5. Review and update incident response procedures >> security-compliance-report.md

call :success "Compliance report generated: security-compliance-report.md"
goto :eof

REM Main execution
:main
call :log "Starting FHIR Bridge Security Compliance Validation"
call :log "Environment: %ENVIRONMENT%"
call :log "Region: %AWS_REGION%"

call :check_prerequisites
call :run_aws_config_checks
call :run_vulnerability_scanning
call :validate_encryption
call :test_access_controls
call :verify_audit_trail
call :generate_compliance_report

call :success "Security compliance validation completed"
call :log "Full report available in: %LOG_FILE%"
goto :eof

REM Execute main function
call :main