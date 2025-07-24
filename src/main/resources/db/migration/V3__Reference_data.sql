-- =============================================================================
-- Reference Data for FHIR Bridge
-- Version: 3.0
-- Description: Inserts initial reference data for consent categories and system configuration
-- =============================================================================

-- Insert default consent categories (these map to DataCategory enum values)
-- Note: These are reference values that map to the enum in com.bridge.model.DataCategory

-- Insert sample consent records for testing/development
-- These should be removed or updated in production environments

-- Sample active consent for patient-12345 with organization-org-67890
INSERT INTO consent_records (patient_id, organization_id, status, effective_date, expiration_date, policy_reference)
SELECT 'patient-12345', 'org-67890', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '1 year', 'HIPAA-Policy-v2.1'
WHERE NOT EXISTS (
    SELECT 1 FROM consent_records 
    WHERE patient_id = 'patient-12345' AND organization_id = 'org-67890'
);

-- Get the ID of the inserted consent record
WITH inserted_consent AS (
    SELECT id FROM consent_records 
    WHERE patient_id = 'patient-12345' AND organization_id = 'org-67890'
)
INSERT INTO consent_allowed_categories (consent_id, data_category)
SELECT id, 'medical-records' FROM inserted_consent
WHERE NOT EXISTS (
    SELECT 1 FROM consent_allowed_categories 
    WHERE consent_id = (SELECT id FROM inserted_consent) AND data_category = 'medical-records'
);

INSERT INTO consent_allowed_categories (consent_id, data_category)
SELECT id, 'lab-results' FROM inserted_consent
WHERE NOT EXISTS (
    SELECT 1 FROM consent_allowed_categories 
    WHERE consent_id = (SELECT id FROM inserted_consent) AND data_category = 'lab-results'
);

-- Sample pending consent for patient-54321 with organization-org-67890
INSERT INTO consent_records (patient_id, organization_id, status, effective_date, policy_reference)
SELECT 'patient-54321', 'org-67890', 'PENDING', CURRENT_TIMESTAMP, 'HIPAA-Policy-v2.1'
WHERE NOT EXISTS (
    SELECT 1 FROM consent_records 
    WHERE patient_id = 'patient-54321' AND organization_id = 'org-67890'
);

-- Sample expired consent for testing expiration logic
INSERT INTO consent_records (patient_id, organization_id, status, effective_date, expiration_date, policy_reference)
SELECT 'patient-11111', 'org-67890', 'ACTIVE', CURRENT_TIMESTAMP - INTERVAL '2 years', CURRENT_TIMESTAMP - INTERVAL '1 year', 'HIPAA-Policy-v1.0'
WHERE NOT EXISTS (
    SELECT 1 FROM consent_records 
    WHERE patient_id = 'patient-11111' AND organization_id = 'org-67890'
);

-- Insert sample audit events for testing
INSERT INTO audit_events (event_id, user_id, action, resource_type, resource_id, outcome, details)
SELECT 'evt-' || gen_random_uuid()::text, 'user-12345', 'CONSENT_CREATE', 'Consent', 'consent-1', 'SUCCESS', '{"method":"POST","endpoint":"/api/consent"}'
WHERE NOT EXISTS (SELECT 1 FROM audit_events WHERE action = 'CONSENT_CREATE' AND resource_id = 'consent-1');

INSERT INTO audit_events (event_id, user_id, action, resource_type, resource_id, outcome, details)
SELECT 'evt-' || gen_random_uuid()::text, 'user-12345', 'FHIR_TRANSFORM', 'HL7Message', 'msg-12345', 'SUCCESS', '{"transformationTime":150,"validationPassed":true}'
WHERE NOT EXISTS (SELECT 1 FROM audit_events WHERE action = 'FHIR_TRANSFORM' AND resource_id = 'msg-12345');

INSERT INTO audit_events (event_id, user_id, action, resource_type, resource_id, outcome, details)
SELECT 'evt-' || gen_random_uuid()::text, 'user-54321', 'CONSENT_VERIFY', 'Consent', 'consent-1', 'FAILURE', '{"reason":"EXPIRED_CONSENT"}'
WHERE NOT EXISTS (SELECT 1 FROM audit_events WHERE action = 'CONSENT_VERIFY' AND resource_id = 'consent-1' AND outcome = 'FAILURE');

-- Create a view for active consents
CREATE OR REPLACE VIEW active_consents AS
SELECT 
    cr.*,
    array_agg(cac.data_category) as allowed_categories_array
FROM consent_records cr
LEFT JOIN consent_allowed_categories cac ON cr.id = cac.consent_id
WHERE cr.status = 'ACTIVE' 
  AND cr.effective_date <= CURRENT_TIMESTAMP
  AND (cr.expiration_date IS NULL OR cr.expiration_date > CURRENT_TIMESTAMP)
GROUP BY cr.id;

-- Create a view for audit summary
CREATE OR REPLACE VIEW audit_summary AS
SELECT 
    DATE(timestamp) as audit_date,
    action,
    outcome,
    COUNT(*) as event_count,
    COUNT(DISTINCT user_id) as unique_users
FROM audit_events
WHERE timestamp >= CURRENT_TIMESTAMP - INTERVAL '30 days'
GROUP BY DATE(timestamp), action, outcome
ORDER BY audit_date DESC, action, outcome;

-- Create function to get consent status for a patient
CREATE OR REPLACE FUNCTION get_patient_consent_status(p_patient_id VARCHAR, p_organization_id VARCHAR)
RETURNS TABLE (
    consent_id BIGINT,
    status VARCHAR,
    is_active BOOLEAN,
    allowed_categories TEXT[],
    effective_date TIMESTAMP,
    expiration_date TIMESTAMP
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        cr.id,
        cr.status,
        (cr.status = 'ACTIVE' AND cr.effective_date <= CURRENT_TIMESTAMP 
         AND (cr.expiration_date IS NULL OR cr.expiration_date > CURRENT_TIMESTAMP)),
        array_agg(cac.data_category),
        cr.effective_date,
        cr.expiration_date
    FROM consent_records cr
    LEFT JOIN consent_allowed_categories cac ON cr.id = cac.consent_id
    WHERE cr.patient_id = p_patient_id AND cr.organization_id = p_organization_id
    GROUP BY cr.id, cr.status, cr.effective_date, cr.expiration_date;
END;
$$ LANGUAGE plpgsql;

-- Create function to log audit events
CREATE OR REPLACE FUNCTION log_audit_event(
    p_user_id VARCHAR,
    p_action VARCHAR,
    p_resource_type VARCHAR,
    p_resource_id VARCHAR,
    p_outcome VARCHAR,
    p_details JSONB
) RETURNS VARCHAR AS $$
DECLARE
    v_event_id VARCHAR;
BEGIN
    v_event_id := 'evt-' || gen_random_uuid()::text;
    
    INSERT INTO audit_events (event_id, user_id, action, resource_type, resource_id, outcome, details)
    VALUES (v_event_id, p_user_id, p_action, p_resource_type, p_resource_id, p_outcome, p_details);
    
    RETURN v_event_id;
END;
$$ LANGUAGE plpgsql;