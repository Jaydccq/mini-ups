-- Mini-UPS Database Schema
-- This file contains the DDL statements to create all database tables

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    phone VARCHAR(20) UNIQUE,
    address TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for users table
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_user_phone ON users(phone);

-- Trucks table
CREATE TABLE IF NOT EXISTS trucks (
    id BIGSERIAL PRIMARY KEY,
    truck_number VARCHAR(50) NOT NULL UNIQUE,
    current_x INTEGER,
    current_y INTEGER,
    capacity NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Shipments table
CREATE TABLE IF NOT EXISTS shipments (
    id BIGSERIAL PRIMARY KEY,
    shipment_id VARCHAR(100) NOT NULL UNIQUE,
    ups_tracking_id VARCHAR(100) UNIQUE,
    amazon_order_id VARCHAR(100),
    warehouse_id VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    origin_x INTEGER NOT NULL,
    origin_y INTEGER NOT NULL,
    dest_x INTEGER NOT NULL,
    dest_y INTEGER NOT NULL,
    weight NUMERIC(10, 2),
    estimated_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    pickup_time TIMESTAMP,
    world_id BIGINT,
    delivery_address TEXT,
    delivery_city VARCHAR(100),
    delivery_zip_code VARCHAR(20),
    user_id BIGINT REFERENCES users(id),
    truck_id BIGINT REFERENCES trucks(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Create indexes for shipments table
CREATE INDEX IF NOT EXISTS idx_shipment_shipment_id ON shipments(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_tracking_id ON shipments(ups_tracking_id);
CREATE INDEX IF NOT EXISTS idx_shipment_status ON shipments(status);
CREATE INDEX IF NOT EXISTS idx_shipment_user ON shipments(user_id);

-- Drivers table
CREATE TABLE IF NOT EXISTS drivers (
    id BIGSERIAL PRIMARY KEY,
    driver_number VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    truck_id BIGINT REFERENCES trucks(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Tracking sequences table (for Leaf ID generator)
CREATE TABLE IF NOT EXISTS tracking_sequences (
    id BIGSERIAL PRIMARY KEY,
    sequence_name VARCHAR(100) NOT NULL UNIQUE,
    current_value BIGINT NOT NULL DEFAULT 0,
    step INTEGER NOT NULL DEFAULT 1000,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Leaf allocation table
CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL DEFAULT 1,
    step INTEGER NOT NULL DEFAULT 1000,
    description VARCHAR(256),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Outbox events table (for transactional outbox pattern)
CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Shipment status history table
CREATE TABLE IF NOT EXISTS shipment_status_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location_x INTEGER,
    location_y INTEGER,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Communication log table
CREATE TABLE IF NOT EXISTS communication_logs (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT REFERENCES shipments(id) ON DELETE CASCADE,
    message_type VARCHAR(50) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    content TEXT,
    status VARCHAR(20),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Truck location history table
CREATE TABLE IF NOT EXISTS truck_location_history (
    id BIGSERIAL PRIMARY KEY,
    truck_id BIGINT NOT NULL REFERENCES trucks(id) ON DELETE CASCADE,
    location_x INTEGER NOT NULL,
    location_y INTEGER NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Address change table
CREATE TABLE IF NOT EXISTS address_changes (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    old_x INTEGER NOT NULL,
    old_y INTEGER NOT NULL,
    new_x INTEGER NOT NULL,
    new_y INTEGER NOT NULL,
    old_address TEXT,
    new_address TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

-- Shipment packages table
CREATE TABLE IF NOT EXISTS shipment_packages (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    package_id VARCHAR(100) NOT NULL,
    description TEXT,
    weight NUMERIC(10, 2),
    dimensions VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
