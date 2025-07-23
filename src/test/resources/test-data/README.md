# Test Data

This directory contains synthetic, HIPAA-compliant test data for the FHIR Bridge application.

## Data Compliance

All test data in this directory is:
- **Synthetic**: Generated using algorithms, not derived from real patient data
- **HIPAA-compliant**: Contains no real PHI (Protected Health Information)
- **Realistic**: Follows healthcare data patterns and standards
- **Diverse**: Includes various demographics, conditions, and scenarios

## Data Categories

- `patients/` - Patient demographic and identification data
- `consent/` - Patient consent records with various authorization scenarios
- `hl7-messages/` - Sample HL7 v2 messages for transformation testing
- `fhir-resources/` - Expected FHIR R4 resources for validation
- `users/` - Test user accounts with different roles and permissions

## Usage

This test data is used for:
- Unit and integration testing
- End-to-end validation
- Performance testing
- Security and compliance validation
- Development and debugging

## Data Generation

Test data is generated using the `TestDataGenerator` utility class which ensures:
- Consistent synthetic data patterns
- HIPAA compliance
- Realistic healthcare scenarios
- Proper data relationships