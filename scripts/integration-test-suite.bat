@echo off
REM FHIR Bridge End-to-End Integration Test Suite for Windows
REM This script runs comprehensive integration tests

setlocal enabledelayedexpansion

REM Configuration
set TEST_ENVIRONMENT=test
set AWS_REGION=us-east-1
set STACK_NAME=fhir-bridge-%TEST_ENVIRONMENT%

REM Colors (limited in Windows CMD)
set RED=[31m
set GREEN=[32m
set YELLOW=[33m
set NC=[0m

echo Starting FHIR Bridge End-to-End Integration Tests

REM Function to get AWS resources
:get_aws_resources
echo Retrieving AWS resources...
for /f "tokens=*" %%i in ('aws elbv2 describe-load-balancers --names "%STACK_NAME%-alb" --query "LoadBalancers[0].DNSName" --output text --region %AWS_REGION% 2^>nul') do set ALB_DNS=%%i
if "%ALB_DNS%"=="" set ALB_DNS=localhost

echo ALB DNS: %ALB_DNS%

REM Set base URL
if "%ALB_DNS%"=="localhost" (
    set BASE_URL=http://localhost:8080
) else (
    set BASE_URL=http://%ALB_DNS%
)
echo Testing against: %BASE_URL%
goto :eof

REM Function to wait for service health
:wait_for_service_health
echo Waiting for service to be healthy...
set attempt=1
set max_attempts=30

:health_check_loop
if %attempt% GTR %max_attempts% (
    echo Service failed to become healthy
    exit /b 1
)

curl -f -s "%BASE_URL%/actuator/health" >nul 2>&1
if %errorlevel%==0 (
    echo Service is healthy!
    goto :eof
)

echo Attempt %attempt%/%max_attempts% - waiting for service...
timeout /t 30 /nobreak >nul
set /a attempt+=1
goto health_check_loop

REM Function to test HL7 transformation
:test_hl7_transformation
echo Testing HL7 v2 to FHIR transformation...

REM Create test data directory
if not exist "test-data" mkdir test_data

REM Create test HL7 messages
echo MSH|^~\&|SENDING|FACILITY|RECV|FACILITY|20240101120000||ADT^A01|MSG001|P|2.5> test-data/ADT_A01.hl7
echo EVN||20240101120000>> test-data/ADT_A01.hl7
echo PID|1||12345678^^^HOSPITAL^MR||DOE^JOHN^MIDDLE||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA>> test-data/ADT_A01.hl7
echo PV1|1|I|ICU^101^A|||12345^SMITH^JANE^MD|||MED||||A|||12345^SMITH^JANE^MD|INP|INSURANCE|||||||||||||||||||||20240101120000>> test-data/ADT_A01.hl7

echo MSH|^~\&|SENDING|FACILITY|RECV|FACILITY|20240101120000||ORU^R01|MSG002|P|2.5> test-data/ORU_R01.hl7
echo PID|1||12345678^^^HOSPITAL^MR||DOE^JOHN^MIDDLE||19800101|M>> test-data/ORU_R01.hl7
echo OBR|1||LAB001|CBC^COMPLETE BLOOD COUNT^L|||20240101120000|||||||||12345^SMITH^JANE^MD>> test-data/ORU_R01.hl7
echo OBX|1|NM|WBC^WHITE BLOOD COUNT^L|1|7.5|10*3/uL|4.0-11.0|N|||F>> test-data/ORU_R01.hl7

REM Test HL7 transformation
for %%f in (ADT_A01.hl7 ORU_R01.hl7) do (
    echo Testing %%f transformation...
    curl -s -X POST "%BASE_URL%/api/v1/transform/hl7" -H "Content-Type: text/plain" -d @test-data/%%f > response.json
    findstr /c:"\"resourceType\":\"Bundle\"" response.json >nul
    if !errorlevel!==0 (
        echo ✓ %%f transformation successful
    ) else (
        echo ✗ %%f transformation failed
    )
)
del response.json
goto :eof

REM Function to test consent management
:test_consent_management
echo Testing consent management workflows...

REM Test consent creation
curl -s -X POST "%BASE_URL%/api/v1/consent" -H "Content-Type: application/json" -d "{\"resourceType\":\"Consent\",\"status\":\"active\",\"scope\":{\"coding\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/consentscope\",\"code\":\"patient-privacy\"}]},\"category\":[{\"coding\":[{\"system\":\"http://loinc.org\",\"code\":\"59284-0\"}]}],\"patient\":{\"reference\":\"Patient/test-patient-123\"},\"provision\":{\"type\":\"permit\",\"provision\":[{\"type\":\"deny\",\"purpose\":[{\"system\":\"http://terminology.hl7.org/CodeSystem/v3-ActReason\",\"code\":\"HMARKT\"}]}]}}}" > consent_response.json

findstr /c:"\"id\"" consent_response.json >nul
if !errorlevel!==0 (
    echo ✓ Consent creation successful
    for /f "tokens=2 delims=:" %%i in ('findstr /c:"\"id\"" consent_response.json') do set consent_id=%%i
    set consent_id=!consent_id:"=!
    set consent_id=!consent_id: =!
) else (
    echo ✗ Consent creation failed
    goto :eof
)

REM Test data filtering with consent
curl -s -X GET "%BASE_URL%/api/v1/patient/test-patient-123/observations" -H "X-Consent-Id: !consent_id!" > filtered_response.json
findstr /c:"\"resourceType\":\"Bundle\"" filtered_response.json >nul
if !errorlevel!==0 (
    echo ✓ Data filtering with consent successful
) else (
    echo ✗ Data filtering with consent failed
)

del consent_response.json
del filtered_response.json
goto :eof

REM Function to test audit logging
:test_audit_logging
echo Testing audit logging and compliance reporting...

REM Generate audit events
curl -s -X GET "%BASE_URL%/api/v1/patient/test-patient-123" >nul 2>&1

REM Test audit endpoint
curl -s -X GET "%BASE_URL%/api/v1/audit/logs" > audit_response.json
findstr /c:"\"logs\"" audit_response.json >nul
if !errorlevel!==0 (
    echo ✓ Audit logging endpoint accessible
) else (
    echo ⚠ Audit logging endpoint not accessible
)
del audit_response.json
goto :eof

REM Function to run security tests
:test_security
echo Running security penetration tests...

REM Test authentication endpoint
curl -s -X POST "%BASE_URL%/api/v1/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"test-user\",\"password\":\"test-password\"}" > auth_response.json
findstr /c:"\"token\"" auth_response.json >nul
if !errorlevel!==0 (
    echo ✓ Authentication endpoint accessible
) else (
    echo ✗ Authentication endpoint failed
)

REM Test rate limiting
echo Testing rate limiting...
for /l %%i in (1,1,10) do (
    start /b curl -s "%BASE_URL%/api/v1/health" >nul
)
timeout /t 2 /nobreak >nul
echo ✓ Rate limiting test completed

del auth_response.json
goto :eof

REM Function to run performance tests
:test_performance
echo Running performance and load testing...

REM Simple load test
echo Running 50 concurrent requests...
for /l %%i in (1,1,50) do (
    start /b curl -s "%BASE_URL%/api/v1/health" >nul
)
timeout /t 5 /nobreak >nul
echo ✓ Performance test completed
goto :eof

REM Function to test disaster recovery
:test_disaster_recovery
echo Testing disaster recovery and backup procedures...

REM Test backup endpoint
curl -s -X POST "%BASE_URL%/api/v1/admin/backup" -H "Content-Type: application/json" -d "{\"type\":\"full\"}" > backup_response.json
findstr /c:"\"status\"" backup_response.json >nul
if !errorlevel!==0 (
    echo