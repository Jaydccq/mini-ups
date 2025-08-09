-- Mini UPS Database Initialization Script

-- Create database if not exists
SELECT 'CREATE DATABASE ups_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'ups_db')\gexec

-- Connect to the database
\c ups_db;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create application user
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_user WHERE usename = 'ups_user') THEN
        CREATE USER ups_user WITH PASSWORD 'ups_password';
    END IF;
END
$$;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ups_db TO ups_user;
GRANT ALL ON SCHEMA public TO ups_user;

-- Create sequences for custom ID generation
CREATE SEQUENCE IF NOT EXISTS shipment_id_seq START WITH 1000001;
CREATE SEQUENCE IF NOT EXISTS tracking_id_seq START WITH 1000000001;
CREATE SEQUENCE IF NOT EXISTS truck_id_seq START WITH 1001;

-- Create custom functions for ID generation
CREATE OR REPLACE FUNCTION generate_shipment_id()
RETURNS TEXT AS $$
BEGIN
    RETURN 'SH' || LPAD(nextval('shipment_id_seq')::TEXT, 8, '0');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_tracking_id()
RETURNS TEXT AS $$
BEGIN
    RETURN 'UPS' || LPAD(nextval('tracking_id_seq')::TEXT, 10, '0');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_truck_id()
RETURNS INTEGER AS $$
BEGIN
    RETURN nextval('truck_id_seq')::INTEGER;
END;
$$ LANGUAGE plpgsql;

-- Note: Table creation and data insertion will be handled by Spring Boot JPA
-- This script only sets up database-level configurations and functions

-- Create audit triggers for logging changes
CREATE OR REPLACE FUNCTION audit_trigger_function()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.updated_at = CURRENT_TIMESTAMP;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Note: Actual audit triggers will be created by JPA/Hibernate based on @EntityListeners

-- Grant necessary permissions to ups_user
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO ups_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ups_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ups_user;

-- Set default privileges for future objects
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ups_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO ups_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO ups_user;

-- Note: Test users and initial data will be created by Spring Boot on first startup
