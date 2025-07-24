-- =============================================================================
-- Initial Database Schema for FHIR Bridge
-- Version: 1.0
-- Description: Creates initial tables for audit events and consent management
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create audit_events table
CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(100),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50),
    resource_id VARCHAR(100),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    outcome VARCHAR(20) NOT NULL,
    details JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create consent_records table
CREATE TABLE IF NOT EXISTS consent_records (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(100) NOT NULL,
    organization_id VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    effective_date TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP,
    policy_reference VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_patient_org UNIQUE (patient_id, organization_id)
);

-- Create consent_allowed_categories table (for many-to-many relationship)
CREATE TABLE IF NOT EXISTS consent_allowed_categories (
    consent_id BIGINT NOT NULL,
    data_category VARCHAR(50) NOT NULL,
    PRIMARY KEY (consent_id, data_category),
    FOREIGN KEY (consent_id) REFERENCES consent_records(id) ON DELETE CASCADE
);

-- Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_audit_event_id ON audit_events(event_id);
CREATE INDEX IF NOT EXISTS idx_audit_user_id ON audit_events(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_action ON audit_events(action);
CREATE INDEX IF NOT EXISTS idx_audit_resource ON audit_events(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_events(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_outcome ON audit_events(outcome);

CREATE INDEX IF NOT EXISTS idx_consent_patient ON consent_records(patient_id);
CREATE INDEX IF NOT EXISTS idx_consent_org ON consent_records(organization_id);
CREATE INDEX IF NOT EXISTS idx_consent_status ON consent_records(status);
CREATE INDEX IF NOT EXISTS idx_consent_effective ON consent_records(effective_date);
CREATE INDEX IF NOT EXISTS idx_consent_expiration ON consent_records(expiration_date);

CREATE INDEX IF NOT EXISTS idx_consent_categories ON consent_allowed_categories(consent_id);

-- Create function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create triggers for automatic updated_at updates
CREATE TRIGGER update_audit_events_updated_at BEFORE UPDATE ON audit_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_consent_records_updated_at BEFORE UPDATE ON consent_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Create health check function
CREATE OR REPLACE FUNCTION health_check()
RETURNS TEXT AS $$
BEGIN
    RETURN 'Database is healthy at ' || NOW();
END;
$$ LANGUAGE plpgsql;

-- Insert initial reference data
INSERT INTO consent_allowed_categories (consent_id, data_category) 
SELECT 1, 'medical-records' WHERE NOT EXISTS (SELECT 1 FROM consent_allowed_categories WHERE consent_id = 1);

-- Grant permissions to application user (will be set via environment variables)
-- These permissions are handled by the initialization script in infra/db/init/