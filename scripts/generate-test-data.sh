#!/bin/bash
# =============================================================================
# Test Data Generation Script
# Generates synthetic test data for FHIR Bridge testing
# =============================================================================

set -e

echo "🔧 Generating test data for FHIR Bridge..."

# Configuration
FHIR_BRIDGE_URL="http://fhir-bridge-test:8080"
DB_HOST="postgres-test"
DB_PORT="5432"
DB_NAME="fhir_bridge_test"
DB_USER="fhir_test_user"
DB_PASSWORD="fhir_test_password"

# Wait for services to be ready
echo "⏳ Waiting for FHIR Bridge to be ready..."
for i in {1..30}; do
    if curl -f "$FHIR_BRIDGE_URL/actuator/health" >/dev/null 2>&1; then
        echo "✅ FHIR Bridge is ready"
        break
    fi
    echo "   Attempt $i/30 - waiting 5 seconds..."
    sleep 5
done

# Generate consent records
echo "📝 Generating consent records..."
PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << 'EOF'
-- Insert test consent records
INSERT INTO consent_records (id, patient_id, consent_status, consent_categories, granted_date, expiry_date, created_at, updated_at) VALUES
('consent-001', 'patient-001', 'ACTIVE', 'GENERAL,CLINICAL', '2024-01-01 00:00:00', '2025-12-31 23:59:59', NOW(), NOW()),
('consent-002', 'patient-002', 'ACTIVE', 'GENERAL', '2024-01-01 00:00:00', '2025-12-31 23:59:59', NOW(), NOW()),
('consent-003', 'patient-003', 'REVOKED', 'GENERAL,CLINICAL', '2024-01-01 00:00:00', '2024-06-30 23:59:59', NOW(), NOW()),
('consent-004', 'patient-004', 'ACTIVE', 'CLINICAL,RESEARCH', '2024-01-01 00:00:00', '2025-12-31 23:59:59', NOW(), NOW()),
('consent-005', 'patient-005', 'EXPIRED', 'GENERAL', '2023-01-01 00:00:00', '2023-12-31 23:59:59', NOW(), NOW());

-- Insert test user accounts
INSERT INTO users (id, username, email, roles, created_at, updated_at) VALUES
('user-001', 'test-clinician', 'clinician@test.local', 'CLINICIAN', NOW(), NOW()),
('user-002', 'test-admin', 'admin@test.local', 'ADMIN,CLINICIAN', NOW(), NOW()),
('user-003', 'test-researcher', 'researcher@test.local', 'RESEARCHER', NOW(), NOW()),
('user-004', 'test-viewer', 'viewer@test.local', 'VIEWER', NOW(), NOW());

-- Insert test audit events
INSERT INTO audit_events (id, event_type, user_id, patient_id, resource_type, action, timestamp, details) VALUES
('audit-001', 'TRANSFORMATION', 'user-001', 'patient-001', 'Patient', 'CREATE', NOW(), '{"source": "HL7v2", "target": "FHIR"}'),
('audit-002', 'CONSENT_CHECK', 'user-001', 'patient-002', 'Consent', 'READ', NOW(), '{"result": "GRANTED"}'),
('audit-003', 'DATA_ACCESS', 'user-002', 'patient-003', 'Observation', 'READ', NOW(), '{"filtered": true}'),
('audit-004', 'AUTHENTICATION', 'user-003', NULL, 'User', 'LOGIN', NOW(), '{"method": "JWT"}');

EOF

echo "✅ Database test data generated successfully"

# Generate test HL7 messages
echo "📄 Generating test HL7 messages..."
mkdir -p /test-data/hl7-messages

# ADT^A01 - Patient Admission
cat > /test-data/hl7-messages/adt-a01-sample.hl7 << 'EOF'
MSH|^~\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240101120000||ADT^A01^ADT_A01|MSG001|P|2.4
EVN||20240101120000|||USER001|20240101120000
PID|1||PATIENT001^^^MRN^MR||DOE^JOHN^MIDDLE^^MR||19800101|M|||123 MAIN ST^^ANYTOWN^ST^12345^USA||(555)123-4567|(555)987-6543||S||PATIENT001|123-45-6789|||||||||||20240101120000
NK1|1|DOE^JANE^||SPOUSE|123 MAIN ST^^ANYTOWN^ST^12345^USA|(555)123-4567|(555)987-6543|EC
PV1|1|I|ICU^101^01^MAIN|||DOCTOR001^SMITH^JOHN^^^DR|DOCTOR002^JONES^MARY^^^DR|MED||||19|VIP|||DOCTOR001^SMITH^JOHN^^^DR|INP|INSURANCE001|2|||||||||||||||||||MAIN|||||20240101120000
AL1|1||PENICILLIN^PENICILLIN|MO|RASH
DG1|1|I10|Z51.11^Encounter for antineoplastic chemotherapy|Encounter for antineoplastic chemotherapy|20240101120000|A
EOF

# ORM^O01 - Order Message
cat > /test-data/hl7-messages/orm-o01-sample.hl7 << 'EOF'
MSH|^~\&|SENDING_APP|SENDING_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240101130000||ORM^O01^ORM_O01|MSG002|P|2.4
PID|1||PATIENT002^^^MRN^MR||SMITH^MARY^ANN^^MS||19750515|F|||456 OAK AVE^^SOMEWHERE^ST^54321^USA||(555)234-5678|(555)876-5432||M||PATIENT002|987-65-4321|||||||||||20240101130000
PV1|1|O|CLINIC^201^01^OUTPATIENT|||DOCTOR003^BROWN^ROBERT^^^DR|DOCTOR003^BROWN^ROBERT^^^DR|FAM||||11||||DOCTOR003^BROWN^ROBERT^^^DR|OUT|INSURANCE002|3|||||||||||||||||||OUTPATIENT|||||20240101130000
ORC|NW|ORDER001|ORDER001|GROUP001|SC||1^ONCE^^^^S||20240101130000|USER002^NURSE^BETTY^^^RN|DOCTOR003^BROWN^ROBERT^^^DR||||CLINIC^OUTPATIENT CLINIC
OBR|1|ORDER001|ORDER001|CBC^COMPLETE BLOOD COUNT^L|||20240101140000|20240101140000|||||ROUTINE||DOCTOR003^BROWN^ROBERT^^^DR||||||20240101140000||F|||1^ONCE^^^^S
EOF

# ORU^R01 - Observation Result
cat > /test-data/hl7-messages/oru-r01-sample.hl7 << 'EOF'
MSH|^~\&|LAB_SYSTEM|LAB_FACILITY|RECEIVING_APP|RECEIVING_FACILITY|20240101150000||ORU^R01^ORU_R01|MSG003|P|2.4
PID|1||PATIENT003^^^MRN^MR||JOHNSON^ROBERT^WILLIAM^^MR||19650320|M|||789 PINE ST^^ELSEWHERE^ST^98765^USA||(555)345-6789|(555)765-4321||M||PATIENT003|456-78-9012|||||||||||20240101150000
PV1|1|O|LAB^301^01^LABORATORY|||DOCTOR004^WILSON^SARAH^^^DR|DOCTOR004^WILSON^SARAH^^^DR|LAB||||22||||DOCTOR004^WILSON^SARAH^^^DR|OUT|INSURANCE003|4|||||||||||||||||||LABORATORY|||||20240101150000
OBR|1|ORDER002|ORDER002|GLUCOSE^GLUCOSE RANDOM^L|||20240101140000|20240101150000|||||STAT||DOCTOR004^WILSON^SARAH^^^DR||||||20240101150000||F|||1^ONCE^^^^S
OBX|1|NM|GLUCOSE^GLUCOSE RANDOM^L|1|95|mg/dL|70-110|N|||F|||20240101150000
OBX|2|NM|HGB^HEMOGLOBIN^L|2|14.5|g/dL|12.0-16.0|N|||F|||20240101150000
OBX|3|NM|HCT^HEMATOCRIT^L|3|42.1|%|36.0-48.0|N|||F|||20240101150000
EOF

echo "✅ Test HL7 messages generated successfully"

# Generate test FHIR resources
echo "🔬 Generating test FHIR resources..."
mkdir -p /test-data/fhir-resources

cat > /test-data/fhir-resources/patient-sample.json << 'EOF'
{
  "resourceType": "Patient",
  "id": "patient-test-001",
  "identifier": [
    {
      "use": "usual",
      "type": {
        "coding": [
          {
            "system": "http://terminology.hl7.org/CodeSystem/v2-0203",
            "code": "MR"
          }
        ]
      },
      "value": "PATIENT001"
    }
  ],
  "name": [
    {
      "use": "official",
      "family": "Doe",
      "given": ["John", "Middle"]
    }
  ],
  "gender": "male",
  "birthDate": "1980-01-01",
  "address": [
    {
      "use": "home",
      "line": ["123 Main St"],
      "city": "Anytown",
      "state": "ST",
      "postalCode": "12345",
      "country": "USA"
    }
  ],
  "telecom": [
    {
      "system": "phone",
      "value": "(555)123-4567",
      "use": "home"
    }
  ]
}
EOF

echo "✅ Test FHIR resources generated successfully"

echo "🎉 Test data generation completed successfully!"
echo ""
echo "📊 Generated test data includes:"
echo "   • 5 consent records with various statuses"
echo "   • 4 test user accounts with different roles"
echo "   • 4 audit event samples"
echo "   • 3 HL7 v2 message samples (ADT, ORM, ORU)"
echo "   • 1 FHIR Patient resource sample"
echo ""
echo "🔗 Access points:"
echo "   • FHIR Bridge API: http://localhost:8083"
echo "   • Management/Health: http://localhost:8084/actuator"
echo "   • PgAdmin (if enabled): http://localhost:5051"
echo "   • Redis Commander (if enabled): http://localhost:8085"