-- Leaf-Segment Distributed ID Generation Algorithm Implementation
-- 
-- This migration creates the leaf_alloc table that implements the
-- Leaf-Segment algorithm for high-throughput, bottleneck-free distributed
-- ID generation in the Mini-UPS system.
--
-- The Leaf-Segment algorithm provides:
-- - <5ms latency for ID generation through memory-based allocation
-- - 100k+ QPS capability by eliminating database sequence contention
-- - Double-buffering for seamless segment transitions
-- - Automatic segment size adaptation based on usage patterns
--
-- Performance characteristics:
-- - Supports concurrent allocation across multiple application instances
-- - Uses optimistic locking to prevent race conditions
-- - Provides configurable segment sizes for different traffic patterns
-- - Includes monitoring and health check capabilities
--
-- Author: Mini-UPS Development Team
-- Version: 1.0

CREATE TABLE IF NOT EXISTS leaf_alloc (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,
    
    -- Business tag - unique identifier for each ID sequence type
    biz_tag VARCHAR(128) NOT NULL UNIQUE,
    
    -- Current maximum ID allocated for this business tag
    -- This represents the upper bound of all segments allocated to date
    max_id BIGINT NOT NULL DEFAULT 0,
    
    -- Segment size (number of IDs allocated in each database transaction)
    -- Larger values reduce database load but increase memory usage
    step INTEGER NOT NULL DEFAULT 1000,
    
    -- Version for optimistic locking
    -- Prevents race conditions during concurrent segment allocations
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Optional descriptive name for monitoring and administration
    description VARCHAR(256),
    
    -- Whether this allocation is currently active
    active BOOLEAN NOT NULL DEFAULT true,
    
    -- Automatic timestamp management
    update_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Optional constraints for automatic step size tuning
    min_step INTEGER,
    max_step INTEGER,
    
    -- Performance monitoring fields
    avg_rate DOUBLE PRECISION, -- Average allocation rate (allocations/second)
    last_alloc_time TIMESTAMP WITH TIME ZONE, -- Last segment allocation time
    
    -- Validation constraints
    CONSTRAINT chk_leaf_step_positive CHECK (step > 0),
    CONSTRAINT chk_leaf_max_id_non_negative CHECK (max_id >= 0),
    CONSTRAINT chk_leaf_version_non_negative CHECK (version >= 0),
    CONSTRAINT chk_leaf_step_bounds CHECK (
        (min_step IS NULL AND max_step IS NULL) OR
        (min_step IS NOT NULL AND max_step IS NOT NULL AND min_step <= max_step)
    ),
    CONSTRAINT chk_leaf_step_within_bounds CHECK (
        (min_step IS NULL OR step >= min_step) AND
        (max_step IS NULL OR step <= max_step)
    )
);

-- Performance Indexes
-- 
-- These indexes are optimized for the Leaf-Segment allocation pattern
-- and common administrative/monitoring queries.

-- Primary lookup index for ID generation (most critical)
CREATE UNIQUE INDEX IF NOT EXISTS idx_leaf_alloc_biz_tag 
ON leaf_alloc (biz_tag) 
WHERE active = true;

-- Monitoring and cleanup index
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_update_time 
ON leaf_alloc (update_time);

-- Index for finding inactive allocations
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_last_alloc 
ON leaf_alloc (active, last_alloc_time) 
WHERE active = true;

-- Index for performance monitoring queries
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_rate 
ON leaf_alloc (active, avg_rate) 
WHERE active = true AND avg_rate IS NOT NULL;

-- Index for auto-tuning queries
CREATE INDEX IF NOT EXISTS idx_leaf_alloc_tuning 
ON leaf_alloc (active, min_step, max_step, last_alloc_time) 
WHERE active = true AND min_step IS NOT NULL AND max_step IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE leaf_alloc IS 'Leaf-Segment distributed ID generation allocation table';
COMMENT ON COLUMN leaf_alloc.biz_tag IS 'Business tag uniquely identifying the ID sequence type';
COMMENT ON COLUMN leaf_alloc.max_id IS 'Current maximum ID allocated for this business tag';
COMMENT ON COLUMN leaf_alloc.step IS 'Number of IDs allocated in each segment (batch size)';
COMMENT ON COLUMN leaf_alloc.version IS 'Version field for optimistic locking during concurrent allocations';
COMMENT ON COLUMN leaf_alloc.avg_rate IS 'Average allocation rate in allocations per second for monitoring';
COMMENT ON COLUMN leaf_alloc.last_alloc_time IS 'Timestamp of the last segment allocation';

-- Update trigger for automatic timestamp management
CREATE OR REPLACE FUNCTION update_leaf_alloc_update_time()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_leaf_alloc_update_time
    BEFORE UPDATE ON leaf_alloc
    FOR EACH ROW
    EXECUTE FUNCTION update_leaf_alloc_update_time();

COMMENT ON TRIGGER trigger_leaf_alloc_update_time ON leaf_alloc IS 'Automatically updates update_time on row modifications';

-- Table optimization settings for high-performance workloads
ALTER TABLE leaf_alloc SET (
    fillfactor = 90,  -- Leave 10% free space for updates
    autovacuum_vacuum_scale_factor = 0.05,  -- Aggressive cleanup for high update rate
    autovacuum_analyze_scale_factor = 0.02   -- Frequent statistics updates for optimal query plans
);

-- Initialize default business tag allocations for Mini-UPS system
-- These provide starting points for the main ID sequences

INSERT INTO leaf_alloc (biz_tag, max_id, step, version, description, active) VALUES
    ('shipment', 0, 10000, 0, 'Shipment ID sequence for package tracking', true),
    ('user', 0, 1000, 0, 'User ID sequence for customer accounts', true),
    ('truck', 0, 100, 0, 'Truck ID sequence for fleet management', true),
    ('tracking_number', 0, 50000, 0, 'Tracking number sequence for package identification', true),
    ('order', 0, 10000, 0, 'Order ID sequence for e-commerce transactions', true),
    ('audit_log', 0, 100000, 0, 'Audit log ID sequence for system events', true),
    ('outbox_event', 0, 50000, 0, 'Outbox event ID sequence for reliable messaging', true)
ON CONFLICT (biz_tag) DO NOTHING;

-- Performance verification queries for manual testing and monitoring
-- 
-- Test segment allocation performance:
-- SELECT biz_tag, max_id, step FROM leaf_alloc WHERE biz_tag = 'shipment';
--
-- Monitor allocation statistics:
-- SELECT biz_tag, max_id, step, avg_rate, last_alloc_time 
-- FROM leaf_alloc WHERE active = true ORDER BY avg_rate DESC NULLS LAST;
--
-- Check for inactive allocations:
-- SELECT biz_tag, last_alloc_time, 
--        EXTRACT(EPOCH FROM (NOW() - last_alloc_time))/3600 as hours_since_last_alloc
-- FROM leaf_alloc 
-- WHERE active = true AND last_alloc_time < NOW() - INTERVAL '1 hour';

-- Security: Ensure proper permissions
-- (Permissions should be managed by the application connection user)
-- GRANT SELECT, INSERT, UPDATE ON leaf_alloc TO miniups_app_user;
-- GRANT USAGE ON SEQUENCE leaf_alloc_id_seq TO miniups_app_user;