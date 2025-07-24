# FHIR Bridge Test Data Guide

This directory contains comprehensive test data for validating HL7 v2 to FHIR R4 transformations with TEFCA compliance.

## Test Data Structure

### 1. Synthetic Patients
- **Location**: `src/test/resources/test-data/patients/synthetic-patients.json`
- **Count**: 20 diverse patient records
- **Demographics**: Covers all major race/ethnicity categories, age ranges, and gender identities
- **TEFCA Compliance**: All patients include required US Core Patient profile elements

### 2. HL7 v2 Messages
- **Location**: `src/test/resources/test-data/hl7-messages/`
- **Message Types**:
  - ADT_A01: Patient admission
  - ADT_A03: Patient discharge
  - ADT_A04: Patient registration
  - ADT_A08: Patient information update
  - ORU_R01: Lab results
  - MDM_T02: Document notification
  - ORM_O01: Order message
  - SIU_S12: Scheduling information

### 3. FHIR Resources
- **Location**: `src/test/resources/test-data/fhir-resources/`
- **Resource Types**:
  - Patient (US Core Patient)
  - Observation (US Core Observation)
  - Encounter (US Core Encounter)
  - DocumentReference (US Core DocumentReference)

## TEFCA Compliance Testing

### Required Elements for TEFCA
1. **Patient Demographics**
   - Legal name (PID.5)
   - Date of birth (PID.7)
   - Gender (PID.8)
   - Race (PID.10)
   - Ethnicity (PID.22)
   - Address (PID.11)
   - Phone number (PID.13)

2. **Clinical Data**
   - LOINC-coded observations
   - SNOMED CT-coded procedures
   - ICD-10-CM diagnoses
   - NDC medications

3. **Security Labels**
   - Confidentiality codes
   - Sensitivity labels
   - Provenance information

## Quick Start Testing

### 1. Basic Patient Admission Test
```bash
# Use patient: PAT-000001 (Alex Smith)
# Message type: ADT_A01
# Expected: Patient resource + Encounter resource
```

### 2. Lab Results Test
```bash
# Use patient: PAT-000003 (Casey Williams)
# Message type: ORU_R01
# Expected: Patient + Observation resources
```

### 3. Document Notification Test
```bash
# Use patient: PAT-000009 (Casey Thompson)
# Message type: MDM_T02
# Expected: Patient + DocumentReference resources
```

## Performance Testing Data

### Load Testing Scenarios
- **Small Load**: 10 messages
- **Medium Load**: 100 messages
- **Large Load**: 1000 messages
- **Stress Test**: 5000 messages

### Data Variations
- Different patient demographics
- Various message types
- Mixed clinical scenarios
- Edge cases (special characters, long names, etc.)

## HIPAA Compliance Test Cases

### Privacy Scenarios
1. **Minimum Necessary**: Test data minimization
2. **Access Controls**: Role-based access
3. **Audit Logging**: All access tracked
4. **Encryption**: Data in transit and at rest

### Security Test Cases
1. **Authentication**: JWT token validation
2. **Authorization**: Role-based permissions
3. **Data Integrity**: Message hashing
4. **Audit Trail**: Complete transaction logging

## Usage Instructions

### Postman Collection
1. Import `FHIR_Bridge_Testing_Collection.json`
2. Import `FHIR_Bridge_Environment.json`
3. Set environment variables
4. Run authentication first
5. Execute test cases in order

### Manual Testing
1. Start FHIR Bridge application
2. Use curl commands or Postman
3. Verify FHIR resources in response
4. Check TEFCA compliance headers

## Validation Checklist

### Pre-Transformation Validation
- [ ] HL7 message structure valid
- [ ] Required fields present
- [ ] Patient identifier exists
- [ ] Date formats correct

### Post-Transformation Validation
- [ ] FHIR Bundle structure valid
- [ ] US Core profiles applied
- [ ] Required extensions present
- [ ] Coding systems correct

### TEFCA Compliance Validation
- [ ] US Core Patient profile
- [ ] US Core Observation profile
- [ ] Security labels applied
- [ ] Provenance information
- [ ] Audit event creation