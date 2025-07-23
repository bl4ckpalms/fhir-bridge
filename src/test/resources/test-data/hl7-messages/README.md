# HL7 v2 Test Message Samples

This directory contains comprehensive HL7 v2 message samples covering all supported message types and common scenarios for testing the FHIR Bridge transformation engine.

## Supported Message Types

The FHIR Bridge supports the following HL7 v2 message types:
- **ADT** (Admission, Discharge, Transfer)
- **ORM** (Order Message)
- **ORU** (Observation Result)
- **MDM** (Medical Document Management)
- **SIU** (Scheduling Information Unsolicited)

## Message Samples Overview

### ADT Messages (Admission, Discharge, Transfer)

| File | Message Type | Trigger Event | Description |
|------|-------------|---------------|-------------|
| `sample-adt-a01.hl7` | ADT^A01 | Patient Admit | Basic patient admission with observations |
| `sample-adt-a03.hl7` | ADT^A03 | Patient Discharge | Emergency discharge with diagnosis |
| `sample-adt-a04.hl7` | ADT^A04 | Patient Registration | Outpatient registration with insurance |
| `sample-adt-a08.hl7` | ADT^A08 | Patient Information Update | Patient update with allergies |
| `sample-adt-a01-complex.hl7` | ADT^A01 | Patient Admit | Complex admission with full demographics, vitals, allergies, diagnosis, insurance |
| `sample-adt-a01-pediatric.hl7` | ADT^A01 | Patient Admit | Pediatric patient admission with age-appropriate vitals |
| `sample-adt-a01-geriatric.hl7` | ADT^A01 | Patient Admit | Geriatric patient admission with multiple conditions |
| `sample-adt-minimal.hl7` | ADT^A01 | Patient Admit | Minimal message for edge case testing |

### ORM Messages (Order Message)

| File | Message Type | Trigger Event | Description |
|------|-------------|---------------|-------------|
| `sample-orm-o01.hl7` | ORM^O01 | General Order | Basic lab order (Lipid Panel) |
| `sample-orm-o02.hl7` | ORM^O02 | Order Cancellation | Cancelled lab order with note |
| `sample-orm-o01-complex.hl7` | ORM^O01 | General Order | Multiple orders (CBC, BMP, PT) with notes |

### ORU Messages (Observation Result)

| File | Message Type | Trigger Event | Description |
|------|-------------|---------------|-------------|
| `sample-oru-r01.hl7` | ORU^R01 | Observation Result | Basic CBC results with abnormal values |
| `sample-oru-r03.hl7` | ORU^R03 | Observation Result | Lipid panel results with notes |
| `sample-oru-r01-complex.hl7` | ORU^R01 | Observation Result | Comprehensive metabolic panel with critical values |
| `sample-oru-microbiology.hl7` | ORU^R01 | Observation Result | Microbiology culture results with susceptibilities |

### MDM Messages (Medical Document Management)

| File | Message Type | Trigger Event | Description |
|------|-------------|---------------|-------------|
| `sample-mdm-t02.hl7` | MDM^T02 | Document Status Change | Progress note document |
| `sample-mdm-t04.hl7` | MDM^T04 | Document Status Change | Discharge summary document |

### SIU Messages (Scheduling Information Unsolicited)

| File | Message Type | Trigger Event | Description |
|------|-------------|---------------|-------------|
| `sample-siu-s12.hl7` | SIU^S12 | New Appointment | New appointment scheduling |
| `sample-siu-s15.hl7` | SIU^S15 | Appointment Cancellation | Cancelled appointment with reason |

## Message Features Covered

### Patient Demographics
- Basic demographics (name, DOB, gender, address, phone)
- Complex name structures with multiple components
- Multiple identifiers (MRN, SSN, etc.)
- Next of kin and emergency contacts
- Insurance information
- Race and ethnicity data

### Clinical Data
- Vital signs (blood pressure, heart rate, temperature, respiratory rate)
- Laboratory results (chemistry, hematology, microbiology)
- Allergies and adverse reactions
- Diagnoses with ICD-10 codes
- Medications and drug allergies

### Visit Information
- Patient class (inpatient, outpatient, emergency)
- Location details (room, bed, unit)
- Admission and discharge dates
- Attending and referring physicians
- Visit numbers and encounter identifiers

### Orders and Results
- Laboratory orders with priorities
- Order status tracking (new, cancelled, completed)
- Result values with reference ranges
- Critical value notifications
- Microbiology results with susceptibilities

### Special Populations
- Pediatric patients with age-appropriate reference ranges
- Geriatric patients with multiple comorbidities
- Emergency department scenarios
- Intensive care unit cases

### Edge Cases and Error Scenarios
- Minimal messages with required fields only
- Missing optional data elements
- Complex multi-segment messages
- Critical values and alerts
- Cancelled orders and appointments

## Usage in Testing

These message samples are designed to test:

1. **Parsing Accuracy**: Verify correct extraction of all HL7 segments and fields
2. **FHIR Transformation**: Ensure proper mapping to FHIR R4 resources
3. **Data Validation**: Test handling of valid and edge case data
4. **Error Handling**: Verify graceful handling of minimal or malformed messages
5. **Clinical Scenarios**: Cover real-world healthcare workflows
6. **Compliance**: Ensure HIPAA and TEFCA compliance requirements

## Message Structure Standards

All messages follow HL7 v2.4 standards with:
- Proper MSH (Message Header) segments
- Standard field separators (|^~\&)
- Appropriate segment ordering
- Valid data types and formats
- Realistic clinical data (synthetic/test data only)

## Data Privacy Note

All patient data in these samples is synthetic and created specifically for testing purposes. No real patient information is included in any of these test messages.