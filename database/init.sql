-- Mini UPS Database Initialization Script

-- Create database if not exists
SELECT 'CREATE DATABASE mini_ups'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'mini_ups')\gexec

-- Connect to the database
\c mini_ups;

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
GRANT ALL PRIVILEGES ON DATABASE mini_ups TO ups_user;
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

-- Insert default admin user (password: admin123)
INSERT INTO users (username, email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'admin',
    'admin@miniups.com',
    '$2a$10$DQw4w9WgXcQ/L1h1ZGMW/.xPgFEKqCv1CgOYNqQ5Z5y4UZ8LgGZCi', -- admin123
    'System',
    'Administrator',
    'ADMIN',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Insert default operator user (password: operator123)
INSERT INTO users (username, email, password, first_name, last_name, role, enabled, created_at, updated_at)
VALUES (
    'operator',
    'operator@miniups.com',
    '$2a$10$8Y8Y8Y8YXcQ/L1h1ZGMW/.xPgFEKqCv1CgOYNqQ5Z5y4UZ8LgGZCi', -- operator123
    'UPS',
    'Operator',
    'OPERATOR',
    true,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Insert some initial trucks
INSERT INTO trucks (truck_id, status, current_x, current_y, capacity, created_at, updated_at)
VALUES 
    (generate_truck_id(), 'IDLE', 0, 0, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (generate_truck_id(), 'IDLE', 10, 10, 150, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (generate_truck_id(), 'IDLE', 20, 20, 200, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (generate_truck_id(), 'IDLE', 30, 30, 120, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (generate_truck_id(), 'IDLE', 40, 40, 180, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (truck_id) DO NOTHING;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE INDEX IF NOT EXISTS idx_shipments_shipment_id ON shipments(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipments_tracking_id ON shipments(ups_tracking_id);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments(status);
CREATE INDEX IF NOT EXISTS idx_shipments_user_id ON shipments(user_id);
CREATE INDEX IF NOT EXISTS idx_shipments_created_at ON shipments(created_at);

CREATE INDEX IF NOT EXISTS idx_trucks_truck_id ON trucks(truck_id);
CREATE INDEX IF NOT EXISTS idx_trucks_status ON trucks(status);

CREATE INDEX IF NOT EXISTS idx_packages_package_id ON packages(package_id);
CREATE INDEX IF NOT EXISTS idx_packages_shipment_id ON packages(shipment_id);

CREATE INDEX IF NOT EXISTS idx_shipment_status_history_shipment_id ON shipment_status_history(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_status_history_timestamp ON shipment_status_history(timestamp);

CREATE INDEX IF NOT EXISTS idx_address_changes_shipment_id ON address_changes(shipment_id);
CREATE INDEX IF NOT EXISTS idx_address_changes_status ON address_changes(status);

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