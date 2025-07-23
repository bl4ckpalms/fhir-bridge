# FHIR Bridge Test Data Documentation

## Overview

This directory contains comprehensive synthetic test data for the FHIR Bridge application. All data is completely synthetic and HIPAA-compliant, designed to support thorough testing of the application's functionality while maintaining privacy and security standards.

## HIPAA Compliance Statement

**IMPORTANT**: All test data in this directory is:
- ✅ **Completely Synthetic**: Generated using algorithms, not derived from real patient data
- ✅ **HIPAA-Compliant**: Contains no real Protected Health Information (PHI)
- ✅ **Safe for Development**: Can be used in development, testing, and demonstration environments
- ✅ **Realistic**: Follows healthcare data patterns and industry standards

## Data Categories

### 1. Patient Data (`patients/synthetic-patients.json`)

Contains 5 synthetic patient records with diverse demographics:

- **Patient IDs**: PAT-000001 through PAT-000005
- **Demographics**: Varied ages, genders, races, and ethnicities
- **Addresses**: Fictional addresses across multiple states
- **Contact Information**: Synthetic phone numbers and addresses
- **Medical Record Numbers**: MRN-00100001 through MRN-00100005

**Key Features**:
- Diverse demographic representation
- Realistic but fictional names
- Valid address formats
- Proper phone number formatting
- Age range from 1965 to 2001 (ages 24-60)

### 2. Consent Records (Multiple Files in `consent/` directory)

Contains comprehensive consent records covering various authorization scenarios:

#### Core Consent Files:
- **`synthetic-consents.json`**: 50+ basic consent records with various statuses and categories
- **`complex-authorization-scenarios.json`**: Complex multi-organization consent scenarios
- **`edge-case-consents.json`**: Edge cases and boundary conditions for consent testing
- **`multi-organization-consents.json`**: Cross-organizational consent relationships
- **`temporal-consent-scenarios.json`**: Time-based consent scenarios and transitions

#### Authorization-Specific Files:
- **`authorization-test-scenarios.json`**: 20 specialized authorization scenarios including:
  - Emergency break-glass access
  - Research study consents with IRB approval
  - Minor patient guardian consents
  - 42 CFR Part 2 substance abuse protections
  - TEFCA-compliant HIE consents
  - Clinical decision support and AI analytics
  - Multi-factor authentication requirements

- **`role-based-authorization-scenarios.json`**: 15 role-based access control scenarios covering:
  - Healthcare provider roles (physicians, nurses, specialists)
  - Administrative roles (registration, billing, case management)
  - Technical roles (system admin, compliance officer)
  - Educational roles (medical students, residents)
  - Emergency and supervised access patterns

- **`tefca-interoperability-scenarios.json`**: 20 interoperability and TEFCA scenarios including:
  - QHIN participant consents for treatment, payment, operations
  - Cross-network data sharing agreements
  - Regional HIE and national network participation
  - FHIR-based interoperability standards
  - Advanced technology scenarios (AI, blockchain, quantum encryption)

**Consent Statuses Covered**: ACTIVE, REVOKED, EXPIRED, PENDING, SUSPENDED, DENIED, INACTIVE
**Organizations**: 50+ different organization types and IDs
**Data Categories**: 15+ healthcare data categories with granular permissions
**Special Authorizations**: Emergency access, research, public health, quality reporting

### 3. Observation Data (`observations/synthetic-observations.json`)

Contains 10 synthetic lab results and vital signs:

- **LOINC Codes**: Standard laboratory test codes
- **Test Types**: Hemoglobin, WBC, Platelets, Electrolytes, Glucose, etc.
- **Values**: Realistic lab values with normal and abnormal results
- **Reference Ranges**: Standard clinical reference ranges
- **Abnormal Flags**: High (H) and Low (L) indicators where appropriate

**Clinical Tests Included**:
- Complete Blood Count (CBC) components
- Basic Metabolic Panel components
- Individual chemistry tests
- Proper units of measure (g/dL, /uL, mmol/L, mg/dL)

### 4. Order Data (`orders/synthetic-orders.json`)

Contains 10 synthetic medical orders:

- **Order Types**: Laboratory tests and imaging studies
- **Statuses**: Active (A) and Complete (C)
- **Priorities**: Routine (R) and Stat (S)
- **Departments**: LABORATORY and RADIOLOGY
- **Providers**: Synthetic doctor identifiers (DR-001 through DR-010)

**Order Types Covered**:
- Laboratory orders (CBC, CMP, Lipid Panel, TSH, HbA1c, PSA)
- Imaging orders (Chest X-Ray, CT Scan, MRI)
- Proper order numbering and tracking

### 5. Visit Data (`visits/synthetic-visits.json`)

Contains 10 synthetic patient encounters:

- **Patient Classes**: Inpatient (I), Outpatient (O), Emergency (E), Newborn (N)
- **Admission Types**: Emergency (E), Routine (R), Urgent (U), Newborn (N)
- **Hospital Services**: MED, SUR, ICU, ER, PED, OBS
- **Locations**: Room and bed assignments
- **Providers**: Attending, referring, and consulting physicians

**Visit Scenarios**:
- Emergency department visits
- Inpatient admissions
- Outpatient clinic visits
- Newborn intensive care
- Various hospital services and departments

### 6. User Data (`users/synthetic-users.json`)

Contains 10 synthetic user accounts with different roles:

- **Roles**: SYSTEM_ADMIN, HEALTHCARE_PROVIDER, LAB_TECHNICIAN, DATA_ANALYST, PATIENT, etc.
- **Organizations**: Distributed across ORG-001 through ORG-005
- **Permissions**: Role-appropriate access levels
- **Status**: Active and inactive accounts

**User Types Covered**:
- System administrators
- Healthcare providers (doctors, nurses)
- Laboratory technicians
- Data analysts and auditors
- Patient portal users
- System integration accounts

### 7. HL7 v2 Messages (`hl7-messages/`)

Contains sample HL7 v2 messages for transformation testing:

- **ADT^A01**: Admission message with patient demographics and visit information
- **ORM^O01**: Order message for laboratory test requests
- **ORU^R01**: Results message with laboratory test results

**Message Features**:
- Proper HL7 v2.4 format
- Standard segment structure (MSH, PID, PV1, OBR, OBX, etc.)
- Realistic field values
- Cross-references to synthetic patient data

### 8. FHIR Resources (`fhir-resources/`)

Contains expected FHIR R4 resources for validation:

- **Patient Resource**: Properly formatted FHIR Patient resource
- **Observation Resource**: Laboratory result in FHIR format
- **US Core Extensions**: Race and ethnicity extensions
- **Proper Coding**: LOINC codes, SNOMED codes, standard terminologies

## Data Relationships

The test data is designed with proper relationships:

1. **Patient-Consent**: All consent records reference valid patient IDs
2. **Patient-Visit**: Visit data can be associated with patient records
3. **Order-Result**: Orders and observations can be linked for workflow testing
4. **User-Organization**: Users are assigned to organizations that match consent records
5. **HL7-FHIR**: HL7 messages contain data that transforms to expected FHIR resources

## Usage Examples

### Loading Test Data in Tests

```java
// Load patient data
List<PatientData> patients = TestDataLoader.loadPatients();

// Get specific patient
PatientData patient = TestDataLoader.getPatientById("PAT-000001");

// Load consent records for a patient
List<ConsentRecord> consents = TestDataLoader.getConsentRecordsForPatient("PAT-000001");

// Load HL7 message
String hl7Message = TestDataLoader.loadHl7Message("sample-adt-a01.hl7");

// Load FHIR resource
String fhirResource = TestDataLoader.loadFhirResource("sample-patient-fhir.json");
```

### Test Scenarios

The test data supports various testing scenarios:

1. **Transformation Testing**: HL7 to FHIR conversion validation
2. **Consent Management**: Testing consent-based data filtering
3. **Security Testing**: Role-based access control validation
4. **Integration Testing**: End-to-end workflow testing
5. **Performance Testing**: Load testing with realistic data volumes
6. **Compliance Testing**: HIPAA and TEFCA compliance validation

## Data Generation

The test data was generated using the following principles:

1. **Synthetic Generation**: All data created algorithmically
2. **Realistic Patterns**: Follows healthcare industry standards
3. **Diverse Representation**: Includes varied demographics and scenarios
4. **Consistent Relationships**: Maintains referential integrity
5. **Compliance Focus**: Designed for HIPAA-compliant testing

## Maintenance

To maintain the test data:

1. **Regular Review**: Periodically review data for continued relevance
2. **Compliance Verification**: Ensure all data remains synthetic and compliant
3. **Scenario Coverage**: Add new scenarios as application features expand
4. **Data Integrity**: Maintain relationships between data entities
5. **Documentation Updates**: Keep this documentation current with changes

## Security Notes

- **No Real PHI**: This data contains no real patient information
- **Safe for Development**: Can be used in any development environment
- **Version Control Safe**: Safe to commit to source control
- **Demonstration Ready**: Suitable for demos and presentations
- **Training Use**: Appropriate for training and education

## Contact

For questions about the test data or to request additional scenarios, contact the development team.

---

**Last Updated**: July 2024  
**Data Version**: 1.0  
**Compliance Status**: HIPAA-Compliant Synthetic Data