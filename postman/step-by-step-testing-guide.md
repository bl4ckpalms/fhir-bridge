# FHIR Bridge Step-by-Step Testing Guide

## Overview
This guide provides a comprehensive walkthrough for testing the FHIR Bridge application using Postman, focusing on HL7 v2 to FHIR R4 transformation with TEFCA compliance.

## Prerequisites

### 1. System Requirements
- **Java 17+** installed
- **PostgreSQL 14+** running
- **Postman** installed (latest version)
- **FHIR Bridge** application running on `http://localhost:8080`

### 2. Initial Setup
```bash
# Start the FHIR Bridge application
./scripts/dev-setup.sh

# Verify application is running
curl http://localhost:8080/actuator/health
```

### 3. Postman Setup
1. **Import Collection**: File → Import → `postman/FHIR_Bridge_Testing_Collection.json`
2. **Import Environment**: File → Import → `postman/FHIR_Bridge_Environment.json`
3. **Select Environment**: Choose "FHIR Bridge Testing Environment" from dropdown

## Step 1: Authentication Setup

### 1.1 Get JWT Token
1. Open Postman collection
2. Navigate to `Authentication → Get JWT Token`
3. Click **Send**
4. Verify response contains token
5. Token is automatically saved to environment variable `jwt_token`

### 1.2 Verify Authentication
```bash
# Test token validity
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/v1/transform/hl7v2-to-fhir
```

## Step 2: Health Checks

### 2.1 Application Health
1. Run `Health Checks → Health Check`
2. Expected response: `{"status":"UP"}`
3. Verify all components are healthy

### 2.2 FHIR Capability Statement
1. Run `Health Checks → FHIR Capability Statement`
2. Verify FHIR server capabilities
3. Check supported resource types

## Step 3: Basic Transformation Testing

### 3.1 Patient Admission (ADT_A01)
**Test Case**: Transform patient admission message

1. Navigate to `HL7 to FHIR Transformation → Transform ADT_A01 - Patient Admission`
2. **Pre-execution Checklist**:
   - [ ] JWT token is set
   - [ ] Base URL is correct
   - [ ] HL7 message is loaded

3. **Execute Test**:
   - Click **Send**
   - Verify status code 200
   - Check response contains FHIR Bundle

4. **Validation Steps**:
   - [ ] Response contains Patient resource
   - [ ] Response contains Encounter resource
   - [ ] Patient has correct demographics
   - [ ] Encounter has correct status

### 3.2 Lab Results (ORU_R01)
**Test Case**: Transform lab results message

1. Navigate to `HL7 to FHIR Transformation → Transform ORU_R01 - Lab Results`
2. **Expected Resources**:
   - Patient
   - Multiple Observations
   - DiagnosticReport

3. **Validation Checklist**:
   - [ ] LOINC codes are correct
   - [ ] Reference ranges are included
   - [ ] Units are properly mapped
   - [ ] Observation values are accurate

### 3.3 Document Notification (MDM_T02)
**Test Case**: Transform document notification

1. Navigate to `HL7 to FHIR Transformation → Transform MDM_T02 - Document Notification`
2. **Expected Resources**:
   - Patient
   - DocumentReference
   - Composition (if applicable)

3. **Validation Checklist**:
   - [ ] Document type is correct
   - [ ] Author information is present
   - [ ] Document content is included
   - [ ] Security labels are applied

## Step 4: TEFCA Compliance Testing

### 4.1 US Core Profile Validation
**Test Case**: Validate US Core Patient profile

1. Navigate to `TEFCA Compliance Testing → Validate TEFCA Format`
2. **Required Elements**:
   - Patient.identifier
   - Patient.name.family
   - Patient.name.given
   - Patient.gender
   - Patient.birthDate
   - Patient.address

3. **Validation Steps**:
   - [ ] Response includes US Core Patient profile
   - [ ] All required fields are present
   - [ ] Race and ethnicity codes are valid
   - [ ] Address format is correct

### 4.2 Security Headers Validation
1. Check response headers for:
   - `X-TEFCA-Compliant: true`
   - `X-TEFCA-Version: 1.0.0`
   - `X-HIPAA-Compliant: true`

### 4.3 Audit Logging Verification
1. Check application logs for audit events
2. Verify all transactions are logged
3. Confirm user access is tracked

## Step 5: Advanced Testing Scenarios

### 5.1 Multi-Patient Testing
**Test Case**: Process multiple patients

1. **Patient Selection**:
   - PAT-000001: Alex Smith (Male, 39)
   - PAT-000003: Casey Williams (Female, 46)
   - PAT-000005: Avery Jones (Other, 23)

2. **Execution Steps**:
   - Update `patient_id` environment variable
   - Run transformation for each patient
   - Verify unique patient IDs

### 5.2 Edge Cases Testing
**Test Cases**:
1. **Special Characters**: Names with apostrophes, hyphens
2. **Long Names**: Maximum field length testing
3. **Null Values**: Optional fields missing
4. **Invalid Dates**: Edge case date handling

### 5.3 Performance Testing
**Test Case**: Load testing with 100 messages

1. Navigate to `Performance Testing → Load Test - 100 Messages`
2. **Configuration**:
   - Set iterations to 100
   - Add delay between requests
   - Monitor response times

3. **Metrics to Track**:
   - Average response time
   - Error rate
   - Throughput (messages/second)

## Step 6: FHIR Resource Verification

### 6.1 Patient Resource Verification
1. **Get Patient by ID**:
   - Run `FHIR Resource Management → Get Patient by ID`
   - Verify patient demographics match source

2. **Search Patients**:
   - Run `FHIR Resource Management → Search Patients`
   - Test various search parameters
   - Verify pagination works

### 6.2 Observation Verification
1. **Get Observations for Patient**:
   - Run `FHIR Resource Management → Get Observations for Patient`
   - Verify all observations are returned
   - Check LOINC codes and values

## Step 7: Error Handling Testing

### 7.1 Invalid HL7 Message
**Test Case**: Send malformed HL7 message

1. **Steps**:
   - Modify HL7 message to be invalid
   - Send to transformation endpoint
   - Verify appropriate error response

2. **Expected Response**:
   - Status code 400
   - Detailed error message
   - Suggestion for correction

### 7.2 Missing Required Fields
**Test Case**: Send HL7 with missing PID segment

1. **Steps**:
   - Remove PID segment from message
   - Send to transformation endpoint
   - Verify error handling

## Step 8: Security Testing

### 8.1 Authentication Failure
**Test Case**: Invalid JWT token

1. **Steps**:
   - Set invalid token in environment
   - Attempt transformation
   - Verify 401 Unauthorized response

### 8.2 Authorization Testing
**Test Case**: Insufficient permissions

1. **Steps**:
   - Use user with limited permissions
   - Attempt restricted operation
   - Verify 403 Forbidden response

## Step 9: Data Validation

### 9.1 Patient Demographics Validation
**Validation Matrix**:

| Field | HL7 Source | FHIR Target | Validation |
|-------|------------|-------------|------------|
| Name | PID.5 | Patient.name | Exact match |
| DOB | PID.7 | Patient.birthDate | Format conversion |
| Gender | PID.8 | Patient.gender | Code mapping |
| Address | PID.11 | Patient.address | Component mapping |
| Phone | PID.13 | Patient.telecom | Type mapping |

### 9.2 Clinical Data Validation
**Observation Validation**:
- LOINC code mapping
- Unit conversion
- Reference range inclusion
- Status mapping

## Step 10: Reporting and Documentation

### 10.1 Test Results Documentation
1. **Success Criteria**:
   - All transformations complete successfully
   - TEFCA compliance verified
   - Performance metrics within acceptable range
   - Security requirements met

### 10.2 Issue Reporting
1. **Template for Issues**:
   ```
   Issue ID: [AUTO_INCREMENT]
   Scenario: [Test Case Name]
   Description: [Detailed description]
   Expected: [Expected behavior]
   Actual: [Actual behavior]
   Steps to Reproduce: [Step-by-step]
   Environment: [Postman, curl, etc.]
   ```

### 10.3 Performance Report
1. **Metrics to Include**:
   - Total test cases executed
   - Success/failure rate
   - Average response time
   - Peak memory usage
   - Error distribution

## Troubleshooting Guide

### Common Issues and Solutions

#### Issue 1: JWT Token Expired
**Symptoms**: 401 Unauthorized errors
**Solution**: 
1. Re-run authentication request
2. Update environment variable
3. Verify token expiration time

#### Issue 2: HL7 Parsing Errors
**Symptoms**: 400 Bad Request with parsing errors
**Solution**:
1. Check HL7 message format
2. Verify segment terminators
3. Validate field separators

#### Issue 3: Missing FHIR Resources
**Symptoms**: Incomplete transformation
**Solution**:
1. Check message completeness
2. Verify required segments
3. Review mapping configuration

#### Issue 4: Performance Issues
**Symptoms**: Slow response times
**Solution**:
1. Check database connection
2. Monitor memory usage
3. Review application logs

## Quick Reference Commands

### cURL Commands
```bash
# Health check
curl http://localhost:8080/actuator/health

# Transform HL7 message
curl -X POST http://localhost:8080/api/v1/transform/hl7v2-to-fhir \
  -H "Content-Type: application/hl7-v2" \
  -H "Accept: application/fhir+json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d @message.hl7

# Get FHIR Patient
curl -H "Accept: application/fhir+json" \
  http://localhost:8080/fhir/Patient/PAT-000001
```

### Postman Collection Runner
1. **Collection Runner**: Runner button in Postman
2. **Environment**: Select "FHIR Bridge Testing Environment"
3. **Iterations**: Set based on test requirements
4. **Delay**: Add delay for load testing

## Next Steps
1. **Integration Testing**: Test with real HL7 sources
2. **User Acceptance Testing**: End-user validation
3. **Production Deployment**: Deploy to staging environment
4. **Monitoring Setup**: Implement health monitoring