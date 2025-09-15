-- Transactional Outbox Pattern Implementation
-- 
-- This migration creates the outbox_events table that implements the
-- Transactional Outbox pattern for reliable event publishing in the
-- Mini-UPS distributed system.
--
-- The outbox pattern eliminates the dual-write problem by ensuring
-- that business operations and event publishing are part of the same
-- database transaction. This guarantees eventual consistency and
-- at-least-once delivery semantics.
--
-- Performance optimizations:
-- - Strategic indexing for polling operations
-- - Partitioning preparation for high-volume scenarios
-- - Efficient data types for storage optimization
--
-- Author: Mini-UPS Development Team
-- Version: 1.0

CREATE TABLE IF NOT EXISTS outbox_events (
    -- Primary key and unique identifiers
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    
    -- Business entity correlation
    aggregate_id VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(50) NOT NULL,
    
    -- Event metadata
    event_type VARCHAR(100) NOT NULL,
    routing_key VARCHAR(200) NOT NULL,
    source_service VARCHAR(50) NOT NULL,
    
    -- Event payload (JSON stored as TEXT for PostgreSQL efficiency)
    payload TEXT NOT NULL,
    
    -- Processing status and retry logic
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 5,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    error_message VARCHAR(1000),
    
    -- Distributed tracing
    correlation_id VARCHAR(36),
    
    -- Timestamps for monitoring and cleanup
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE,
    
    -- Constraints
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED', 'ARCHIVED')),
    CONSTRAINT chk_retry_count CHECK (retry_count >= 0),
    CONSTRAINT chk_max_retries CHECK (max_retries >= 0)
);

-- Performance Indexes
-- 
-- These indexes are optimized for the outbox polling pattern and
-- common query patterns in event-driven architectures.

-- Primary polling index: status + created_at for FIFO processing
-- This is the most critical index for outbox polling performance
CREATE INDEX IF NOT EXISTS idx_outbox_status_created 
ON outbox_events (status, created_at) 
WHERE status IN ('PENDING', 'PROCESSING');

-- Aggregate correlation index for business entity event ordering
CREATE INDEX IF NOT EXISTS idx_outbox_aggregate_id 
ON outbox_events (aggregate_id);

-- Event type index for monitoring and filtering
CREATE INDEX IF NOT EXISTS idx_outbox_event_type 
ON outbox_events (event_type);

-- Correlation ID index for distributed tracing
CREATE INDEX IF NOT EXISTS idx_outbox_correlation_id 
ON outbox_events (correlation_id) 
WHERE correlation_id IS NOT NULL;

-- Cleanup index for published events (supports efficient deletion)
CREATE INDEX IF NOT EXISTS idx_outbox_published_cleanup 
ON outbox_events (status, published_at) 
WHERE status = 'PUBLISHED';

-- Retry processing index for failed events with exponential backoff
CREATE INDEX IF NOT EXISTS idx_outbox_retry_ready 
ON outbox_events (status, next_retry_at) 
WHERE status = 'PENDING' AND next_retry_at IS NOT NULL;

-- Monitoring index for stuck processing events
CREATE INDEX IF NOT EXISTS idx_outbox_stuck_processing 
ON outbox_events (status, updated_at) 
WHERE status = 'PROCESSING';

-- Partial index for failed events (operational monitoring)
CREATE INDEX IF NOT EXISTS idx_outbox_failed_events 
ON outbox_events (updated_at DESC) 
WHERE status = 'FAILED';

-- Comments for documentation
COMMENT ON TABLE outbox_events IS 'Transactional Outbox pattern implementation for reliable event publishing';
COMMENT ON COLUMN outbox_events.event_id IS 'Unique UUID for event deduplication and correlation';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'Business entity ID this event relates to';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type of business entity (Shipment, User, Truck, etc.)';
COMMENT ON COLUMN outbox_events.payload IS 'JSON event data stored as TEXT for PostgreSQL optimization';
COMMENT ON COLUMN outbox_events.routing_key IS 'RabbitMQ routing key for message routing';
COMMENT ON COLUMN outbox_events.retry_count IS 'Number of processing attempts for exponential backoff';
COMMENT ON COLUMN outbox_events.next_retry_at IS 'Scheduled time for next retry attempt';
COMMENT ON COLUMN outbox_events.correlation_id IS 'Distributed tracing correlation identifier';

-- Update trigger for automatic updated_at timestamp
CREATE OR REPLACE FUNCTION update_outbox_events_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_outbox_events_updated_at
    BEFORE UPDATE ON outbox_events
    FOR EACH ROW
    EXECUTE FUNCTION update_outbox_events_updated_at();

COMMENT ON TRIGGER trigger_outbox_events_updated_at ON outbox_events IS 'Automatically updates updated_at timestamp on row modifications';

-- Table statistics update for query planner optimization
-- This helps PostgreSQL choose optimal execution plans for outbox queries
ALTER TABLE outbox_events SET (
    fillfactor = 85,  -- Leave 15% free space for updates
    autovacuum_vacuum_scale_factor = 0.1,  -- More aggressive cleanup
    autovacuum_analyze_scale_factor = 0.05  -- Frequent statistics updates
);

-- Security: Ensure proper permissions
-- (Permissions should be managed by the application connection user)

-- Performance verification query (for manual testing)
-- SELECT count(*) FROM outbox_events WHERE status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= NOW());