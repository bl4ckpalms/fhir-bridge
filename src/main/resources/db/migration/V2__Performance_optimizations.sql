-- =============================================================================
-- Performance Optimizations for FHIR Bridge Database
-- Version: 2.0
-- Description: Adds additional indexes and optimizations for production use
-- =============================================================================

-- Create composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_audit_user_action ON audit_events(user_id, action);
CREATE INDEX IF NOT EXISTS idx_audit_user_timestamp ON audit_events(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_resource_timestamp ON audit_events(resource_type, resource_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action_timestamp ON audit_events(action, timestamp DESC);

-- Create partial indexes for specific query patterns
CREATE INDEX IF NOT EXISTS idx_audit_recent ON audit_events(timestamp DESC) 
WHERE timestamp > CURRENT_TIMESTAMP - INTERVAL '30 days';

CREATE INDEX IF NOT EXISTS idx_consent_active ON consent_records(status, effective_date, expiration_date)
WHERE status = 'ACTIVE' AND (expiration_date IS NULL OR expiration_date > CURRENT_TIMESTAMP);

-- Create indexes for JSONB queries on details column
CREATE INDEX IF NOT EXISTS idx_audit_details_gin ON audit_events USING GIN (details);

-- Create function-based index for case-insensitive searches
CREATE INDEX IF NOT EXISTS idx_audit_action_lower ON audit_events(LOWER(action));
CREATE INDEX IF NOT EXISTS idx_audit_user_lower ON audit_events(LOWER(user_id));

-- Create indexes for consent management queries
CREATE INDEX IF NOT EXISTS idx_consent_patient_status ON consent_records(patient_id, status);
CREATE INDEX IF NOT EXISTS idx_consent_org_status ON consent_records(organization_id, status);

-- Create index for consent expiration monitoring
CREATE INDEX IF NOT EXISTS idx_consent_expires_soon ON consent_records(expiration_date)
WHERE expiration_date IS NOT NULL 
  AND expiration_date > CURRENT_TIMESTAMP 
  AND expiration_date < CURRENT_TIMESTAMP + INTERVAL '30 days';

-- Add table statistics update
ANALYZE audit_events;
ANALYZE consent_records;
ANALYZE consent_allowed_categories;

-- Create materialized view for audit analytics (optional, for reporting)
DROP MATERIALIZED VIEW IF EXISTS audit_analytics_mv;
CREATE MATERIALIZED VIEW audit_analytics_mv AS
SELECT 
    DATE_TRUNC('day', timestamp) as date,
    action,
    outcome,
    resource_type,
    COUNT(*) as event_count,
    COUNT(DISTINCT user_id) as unique_users
FROM audit_events
WHERE timestamp > CURRENT_TIMESTAMP - INTERVAL '90 days'
GROUP BY DATE_TRUNC('day', timestamp), action, outcome, resource_type;

-- Create index on materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_audit_analytics_mv 
ON audit_analytics_mv(date, action, outcome, resource_type);

-- Create function to refresh materialized view
CREATE OR REPLACE FUNCTION refresh_audit_analytics()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY audit_analytics_mv;
END;
$$ LANGUAGE plpgsql;

-- Grant execute permissions on refresh function (will be handled by initialization script)
-- GRANT EXECUTE ON FUNCTION refresh_audit_analytics() TO fhir_user;