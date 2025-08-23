-- World Simulator Database Schema
-- Version 1.0 - Initial schema creation
-- This schema defines the core data model for the World Simulator including
-- warehouses, trucks, packages, and simulation state tracking.

-- Enable UUID extension for generating unique identifiers
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Warehouses table - Physical locations for package storage and truck dispatch
CREATE TABLE warehouses (
    id BIGSERIAL PRIMARY KEY,
    warehouse_id VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    x_coordinate INTEGER NOT NULL,
    y_coordinate INTEGER NOT NULL,
    capacity INTEGER NOT NULL DEFAULT 1000,
    current_inventory INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT warehouses_capacity_positive CHECK (capacity > 0),
    CONSTRAINT warehouses_inventory_non_negative CHECK (current_inventory >= 0),
    CONSTRAINT warehouses_inventory_within_capacity CHECK (current_inventory <= capacity),
    CONSTRAINT warehouses_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE', 'MAINTENANCE'))
);

-- Trucks table - Vehicle entities for package delivery
CREATE TABLE trucks (
    id BIGSERIAL PRIMARY KEY,
    truck_id VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'IDLE',
    current_x INTEGER NOT NULL DEFAULT 0,
    current_y INTEGER NOT NULL DEFAULT 0,
    target_x INTEGER,
    target_y INTEGER,
    speed INTEGER NOT NULL DEFAULT 50,
    capacity INTEGER NOT NULL DEFAULT 100,
    current_load INTEGER NOT NULL DEFAULT 0,
    assigned_warehouse_id BIGINT,
    estimated_arrival_time TIMESTAMP,
    last_position_update TIMESTAMP NOT NULL DEFAULT NOW(),
    sequence_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT trucks_speed_positive CHECK (speed > 0),
    CONSTRAINT trucks_capacity_positive CHECK (capacity > 0),
    CONSTRAINT trucks_load_non_negative CHECK (current_load >= 0),
    CONSTRAINT trucks_load_within_capacity CHECK (current_load <= capacity),
    CONSTRAINT trucks_status_valid CHECK (status IN ('IDLE', 'TRAVELING', 'LOADING', 'UNLOADING', 'DELIVERING', 'RETURNING', 'MAINTENANCE')),
    CONSTRAINT trucks_sequence_non_negative CHECK (sequence_number >= 0),
    FOREIGN KEY (assigned_warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL
);

-- Packages table - Individual items being transported
CREATE TABLE packages (
    id BIGSERIAL PRIMARY KEY,
    package_id VARCHAR(50) UNIQUE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    source_x INTEGER NOT NULL,
    source_y INTEGER NOT NULL,
    destination_x INTEGER NOT NULL,
    destination_y INTEGER NOT NULL,
    weight INTEGER NOT NULL DEFAULT 1,
    priority VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    assigned_truck_id BIGINT,
    current_warehouse_id BIGINT,
    estimated_delivery_time TIMESTAMP,
    actual_delivery_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT packages_weight_positive CHECK (weight > 0),
    CONSTRAINT packages_status_valid CHECK (status IN ('CREATED', 'AT_WAREHOUSE', 'IN_TRANSIT', 'DELIVERED', 'FAILED', 'RETURNED')),
    CONSTRAINT packages_priority_valid CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT')),
    FOREIGN KEY (assigned_truck_id) REFERENCES trucks(id) ON DELETE SET NULL,
    FOREIGN KEY (current_warehouse_id) REFERENCES warehouses(id) ON DELETE SET NULL
);

-- Delivery events table - Historical log of delivery-related events
CREATE TABLE delivery_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(30) NOT NULL,
    event_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    truck_id BIGINT,
    package_id BIGINT,
    warehouse_id BIGINT,
    x_coordinate INTEGER,
    y_coordinate INTEGER,
    details JSONB,
    sequence_number BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT delivery_events_type_valid CHECK (event_type IN (
        'TRUCK_DISPATCHED', 'TRUCK_ARRIVED', 'PACKAGE_LOADED', 'PACKAGE_UNLOADED',
        'DELIVERY_STARTED', 'DELIVERY_COMPLETED', 'DELIVERY_FAILED', 'TRUCK_RETURNED',
        'WAREHOUSE_UPDATED', 'POSITION_UPDATED'
    )),
    CONSTRAINT delivery_events_sequence_non_negative CHECK (sequence_number >= 0),
    FOREIGN KEY (truck_id) REFERENCES trucks(id) ON DELETE CASCADE,
    FOREIGN KEY (package_id) REFERENCES packages(id) ON DELETE CASCADE,
    FOREIGN KEY (warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
);

-- Connection sessions table - Track active client connections
CREATE TABLE connection_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) UNIQUE NOT NULL,
    client_type VARCHAR(10) NOT NULL,
    client_host VARCHAR(255) NOT NULL,
    client_port INTEGER NOT NULL,
    connection_time TIMESTAMP NOT NULL DEFAULT NOW(),
    last_activity TIMESTAMP NOT NULL DEFAULT NOW(),
    sequence_number BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT connection_sessions_client_type_valid CHECK (client_type IN ('UPS', 'AMAZON')),
    CONSTRAINT connection_sessions_status_valid CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONNECTED')),
    CONSTRAINT connection_sessions_sequence_non_negative CHECK (sequence_number >= 0)
);

-- Message acknowledgments table - Track message delivery and acknowledgment
CREATE TABLE message_acknowledgments (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    message_sequence BIGINT NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    sent_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    ack_timestamp TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message_data JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT message_acks_retry_count_non_negative CHECK (retry_count >= 0),
    CONSTRAINT message_acks_sequence_positive CHECK (message_sequence > 0),
    CONSTRAINT message_acks_status_valid CHECK (status IN ('PENDING', 'ACKNOWLEDGED', 'TIMEOUT', 'FAILED')),
    FOREIGN KEY (session_id) REFERENCES connection_sessions(session_id) ON DELETE CASCADE,
    UNIQUE (session_id, message_sequence)
);

-- Simulation state table - Track overall simulation status and configuration
CREATE TABLE simulation_state (
    id BIGSERIAL PRIMARY KEY,
    simulation_time TIMESTAMP NOT NULL DEFAULT NOW(),
    tick_count BIGINT NOT NULL DEFAULT 0,
    total_trucks INTEGER NOT NULL DEFAULT 0,
    active_trucks INTEGER NOT NULL DEFAULT 0,
    total_packages INTEGER NOT NULL DEFAULT 0,
    delivered_packages INTEGER NOT NULL DEFAULT 0,
    total_warehouses INTEGER NOT NULL DEFAULT 0,
    active_connections INTEGER NOT NULL DEFAULT 0,
    flakiness_percentage INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT simulation_state_tick_count_non_negative CHECK (tick_count >= 0),
    CONSTRAINT simulation_state_counts_non_negative CHECK (
        total_trucks >= 0 AND active_trucks >= 0 AND total_packages >= 0 AND
        delivered_packages >= 0 AND total_warehouses >= 0 AND active_connections >= 0
    ),
    CONSTRAINT simulation_state_flakiness_valid CHECK (flakiness_percentage BETWEEN 0 AND 99),
    CONSTRAINT simulation_state_status_valid CHECK (status IN ('RUNNING', 'PAUSED', 'STOPPED', 'ERROR'))
);

-- Indexes for performance optimization
CREATE INDEX idx_trucks_status ON trucks(status);
CREATE INDEX idx_trucks_position ON trucks(current_x, current_y);
CREATE INDEX idx_trucks_warehouse ON trucks(assigned_warehouse_id);
CREATE INDEX idx_trucks_sequence ON trucks(sequence_number);

CREATE INDEX idx_packages_status ON packages(status);
CREATE INDEX idx_packages_truck ON packages(assigned_truck_id);
CREATE INDEX idx_packages_warehouse ON packages(current_warehouse_id);
CREATE INDEX idx_packages_destination ON packages(destination_x, destination_y);

CREATE INDEX idx_delivery_events_timestamp ON delivery_events(event_timestamp);
CREATE INDEX idx_delivery_events_truck ON delivery_events(truck_id);
CREATE INDEX idx_delivery_events_package ON delivery_events(package_id);
CREATE INDEX idx_delivery_events_type ON delivery_events(event_type);
CREATE INDEX idx_delivery_events_sequence ON delivery_events(sequence_number);

CREATE INDEX idx_connection_sessions_client_type ON connection_sessions(client_type);
CREATE INDEX idx_connection_sessions_status ON connection_sessions(status);
CREATE INDEX idx_connection_sessions_activity ON connection_sessions(last_activity);

CREATE INDEX idx_message_acks_session ON message_acknowledgments(session_id);
CREATE INDEX idx_message_acks_status ON message_acknowledgments(status);
CREATE INDEX idx_message_acks_timestamp ON message_acknowledgments(sent_timestamp);

CREATE INDEX idx_warehouses_position ON warehouses(x_coordinate, y_coordinate);
CREATE INDEX idx_warehouses_status ON warehouses(status);

-- Insert initial simulation state record
INSERT INTO simulation_state (
    simulation_time, 
    tick_count, 
    status,
    flakiness_percentage
) VALUES (
    NOW(), 
    0, 
    'STOPPED',
    0
);

-- Insert sample warehouses for testing
INSERT INTO warehouses (warehouse_id, name, x_coordinate, y_coordinate, capacity) VALUES
('WH001', 'Central Warehouse', 0, 0, 1000),
('WH002', 'North Warehouse', 100, 200, 500),
('WH003', 'South Warehouse', -50, -100, 750),
('WH004', 'East Warehouse', 300, 50, 600),
('WH005', 'West Warehouse', -200, 100, 400);

-- Add trigger to update updated_at timestamp automatically
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_warehouses_updated_at BEFORE UPDATE ON warehouses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_trucks_updated_at BEFORE UPDATE ON trucks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_packages_updated_at BEFORE UPDATE ON packages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_delivery_events_updated_at BEFORE UPDATE ON delivery_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_connection_sessions_updated_at BEFORE UPDATE ON connection_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_message_acknowledgments_updated_at BEFORE UPDATE ON message_acknowledgments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_simulation_state_updated_at BEFORE UPDATE ON simulation_state
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();