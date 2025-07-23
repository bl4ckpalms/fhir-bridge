-- =============================================================================
-- Database Initialization Script for FHIR Bridge
-- =============================================================================

-- Create database if it doesn't exist (handled by POSTGRES_DB env var)
-- This script runs after the database is created

-- Set timezone to UTC
SET timezone = 'UTC';

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Create schema for application tables (optional, using public schema for simplicity)
-- CREATE SCHEMA IF NOT EXISTS fhir_bridge;

-- Grant permissions to the application user
GRANT ALL PRIVILEGES ON DATABASE fhir_bridge TO fhir_user;
GRANT ALL PRIVILEGES ON SCHEMA public TO fhir_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO fhir_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO fhir_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO fhir_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO fhir_user;

-- Create a simple health check function
CREATE OR REPLACE FUNCTION health_check()
RETURNS TEXT AS $$
BEGIN
    RETURN 'Database is healthy at ' || NOW();
END;
$$ LANGUAGE plpgsql;

-- Log initialization completion
DO $$
BEGIN
    RAISE NOTICE 'FHIR Bridge database initialization completed successfully';
END $$;