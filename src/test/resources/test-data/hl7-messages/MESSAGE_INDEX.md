# HL7 v2 Message Sample Index

## Complete List of Test Messages

### ADT Messages (8 samples)
1. `sample-adt-a01.hl7` - Basic patient admission
2. `sample-adt-a03.hl7` - Patient discharge with diagnosis
3. `sample-adt-a04.hl7` - Patient registration with insurance
4. `sample-adt-a08.hl7` - Patient information update with allergies
5. `sample-adt-a01-complex.hl7` - Complex admission (full demographics, vitals, allergies, diagnosis, insurance)
6. `sample-adt-a01-pediatric.hl7` - Pediatric patient admission
7. `sample-adt-a01-geriatric.hl7` - Geriatric patient admission
8. `sample-adt-minimal.hl7` - Minimal message for edge case testing
9. `sample-adt-transfer.hl7` - Patient transfer (A02)

### ORM Messages (3 samples)
1. `sample-orm-o01.hl7` - Basic lab order (Lipid Panel)
2. `sample-orm-o02.hl7` - Order cancellation
3. `sample-orm-o01-complex.hl7` - Multiple orders with notes

### ORU Messages (5 samples)
1. `sample-oru-r01.hl7` - Basic CBC results
2. `sample-oru-r03.hl7` - Lipid panel results
3. `sample-oru-r01-complex.hl7` - Comprehensive metabolic panel
4. `sample-oru-microbiology.hl7` - Microbiology culture results
5. `sample-oru-pathology.hl7` - Surgical pathology report

### MDM Messages (2 samples)
1. `sample-mdm-t02.hl7` - Progress note document
2. `sample-mdm-t04.hl7` - Discharge summary document

### SIU Messages (2 samples)
1. `sample-siu-s12.hl7` - New appointment scheduling
2. `sample-siu-s15.hl7` - Appointment cancellation

## Total: 20 HL7 v2 Message Samples

## Coverage Summary

### Message Types Covered:
- ✅ ADT (Admission, Discharge, Transfer) - 9 samples
- ✅ ORM (Order Message) - 3 samples  
- ✅ ORU (Observation Result) - 5 samples
- ✅ MDM (Medical Document Management) - 2 samples
- ✅ SIU (Scheduling Information Unsolicited) - 2 samples

### Trigger Events Covered:
- ADT^A01 (Patient Admit) - 4 variations
- ADT^A02 (Patient Transfer) - 1 sample
- ADT^A03 (Patient Discharge) - 1 sample
- ADT^A04 (Patient Registration) - 1 sample
- ADT^A08 (Patient Information Update) - 1 sample
- ORM^O01 (General Order) - 2 samples
- ORM^O02 (Order Cancellation) - 1 sample
- ORU^R01 (Observation Result) - 4 samples
- ORU^R03 (Observation Result) - 1 sample
- MDM^T02 (Document Status Change) - 1 sample
- MDM^T04 (Document Status Change) - 1 sample
- SIU^S12 (New Appointment) - 1 sample
- SIU^S15 (Appointment Cancellation) - 1 sample

### Clinical Scenarios Covered:
- Emergency department admissions
- Inpatient admissions and transfers
- Outpatient registrations and visits
- Laboratory orders and results
- Microbiology and pathology reports
- Medical documentation
- Appointment scheduling
- Pediatric and geriatric cases
- Complex multi-condition patients
- Minimal/edge case scenarios

### Data Elements Covered:
- Patient demographics and identifiers
- Next of kin and emergency contacts
- Insurance information
- Vital signs and observations
- Laboratory results with reference ranges
- Allergies and adverse reactions
- Diagnoses with ICD-10 codes
- Orders with priorities and status
- Clinical notes and documentation
- Appointment scheduling details

This comprehensive set of test messages ensures thorough testing of the FHIR Bridge's HL7 v2 to FHIR R4 transformation capabilities across all supported message types and common healthcare scenarios.