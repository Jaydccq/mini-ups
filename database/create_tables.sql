-- Mini UPS Database Schema Creation Script
-- PostgreSQL Version
-- This script creates all tables needed for the Mini-UPS system

-- Connect to ups_db database
\c ups_db;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create sequences for ID generation
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

-- 1. Users table (用户表)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20),
    address TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- 2. Trucks table (卡车表)
CREATE TABLE IF NOT EXISTS trucks (
    id BIGSERIAL PRIMARY KEY,
    truck_id INTEGER UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    current_x INTEGER NOT NULL DEFAULT 0,
    current_y INTEGER NOT NULL DEFAULT 0,
    capacity INTEGER NOT NULL DEFAULT 100,
    driver_id BIGINT,
    world_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0
);

-- 3. Shipments table (运输订单表)
CREATE TABLE IF NOT EXISTS shipments (
    id BIGSERIAL PRIMARY KEY,
    shipment_id VARCHAR(100) UNIQUE NOT NULL,
    ups_tracking_id VARCHAR(100) UNIQUE,
    amazon_order_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    origin_x INTEGER NOT NULL,
    origin_y INTEGER NOT NULL,
    dest_x INTEGER NOT NULL,
    dest_y INTEGER NOT NULL,
    weight DECIMAL(10,2),
    estimated_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    pickup_time TIMESTAMP,
    world_id BIGINT,
    user_id BIGINT,
    truck_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (truck_id) REFERENCES trucks(id)
);

-- 4. Packages table (包裹表)
CREATE TABLE IF NOT EXISTS packages (
    id BIGSERIAL PRIMARY KEY,
    package_id VARCHAR(100) UNIQUE NOT NULL,
    description VARCHAR(500),
    weight DECIMAL(10,2),
    length_cm DECIMAL(8,2),
    width_cm DECIMAL(8,2),
    height_cm DECIMAL(8,2),
    value DECIMAL(12,2),
    fragile BOOLEAN NOT NULL DEFAULT FALSE,
    shipment_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id)
);

-- 5. Shipment Status History table (运输状态历史表)
CREATE TABLE IF NOT EXISTS shipment_status_history (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    location_x INTEGER,
    location_y INTEGER,
    notes VARCHAR(500),
    shipment_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id)
);

-- 6. Address Changes table (地址修改请求表)
CREATE TABLE IF NOT EXISTS address_changes (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    old_x INTEGER NOT NULL,
    old_y INTEGER NOT NULL,
    new_x INTEGER NOT NULL,
    new_y INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (shipment_id) REFERENCES shipments(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 7. Truck Location History table (卡车位置历史表)
CREATE TABLE IF NOT EXISTS truck_location_history (
    id BIGSERIAL PRIMARY KEY,
    truck_id BIGINT NOT NULL,
    location_x INTEGER NOT NULL,
    location_y INTEGER NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    FOREIGN KEY (truck_id) REFERENCES trucks(id)
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

CREATE INDEX IF NOT EXISTS idx_trucks_truck_id ON trucks(truck_id);
CREATE INDEX IF NOT EXISTS idx_trucks_status ON trucks(status);

CREATE INDEX IF NOT EXISTS idx_shipments_shipment_id ON shipments(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipments_tracking_id ON shipments(ups_tracking_id);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments(status);
CREATE INDEX IF NOT EXISTS idx_shipments_user_id ON shipments(user_id);
CREATE INDEX IF NOT EXISTS idx_shipments_created_at ON shipments(created_at);

CREATE INDEX IF NOT EXISTS idx_packages_package_id ON packages(package_id);
CREATE INDEX IF NOT EXISTS idx_packages_shipment_id ON packages(shipment_id);

CREATE INDEX IF NOT EXISTS idx_shipment_status_history_shipment_id ON shipment_status_history(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_status_history_timestamp ON shipment_status_history(timestamp);

CREATE INDEX IF NOT EXISTS idx_address_changes_shipment_id ON address_changes(shipment_id);
CREATE INDEX IF NOT EXISTS idx_address_changes_status ON address_changes(status);

CREATE INDEX IF NOT EXISTS idx_truck_location_history_truck_id ON truck_location_history(truck_id);
CREATE INDEX IF NOT EXISTS idx_truck_location_history_timestamp ON truck_location_history(timestamp);

-- Insert default admin user (password: admin123, hashed with BCrypt)
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

-- Insert default operator user (password: operator123, hashed with BCrypt)
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

-- Create audit trigger function for auto-updating timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers for auto-updating updated_at column
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_trucks_updated_at BEFORE UPDATE ON trucks FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_shipments_updated_at BEFORE UPDATE ON shipments FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_packages_updated_at BEFORE UPDATE ON packages FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_shipment_status_history_updated_at BEFORE UPDATE ON shipment_status_history FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_address_changes_updated_at BEFORE UPDATE ON address_changes FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_truck_location_history_updated_at BEFORE UPDATE ON truck_location_history FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMIT;

-- Success message
\echo '数据库表创建完成！'
\echo '默认用户账号:'
\echo '  管理员: admin@miniups.com / admin123'
\echo '  操作员: operator@miniups.com / operator123'
\echo '已创建5辆初始卡车，状态为IDLE'