-- Initialize test database for RAG integration tests
CREATE EXTENSION IF NOT EXISTS vector;

-- Create the RAG document chunk table
CREATE TABLE IF NOT EXISTS rag_document_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id VARCHAR(255) NOT NULL,
    source VARCHAR(500) NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    content_tsv TSVECTOR,
    metadata JSONB,
    embedding VECTOR(1536),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for optimal performance
CREATE INDEX IF NOT EXISTS idx_rag_chunk_embedding ON rag_document_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_rag_chunk_content_tsv ON rag_document_chunk USING gin(content_tsv);
CREATE INDEX IF NOT EXISTS idx_rag_chunk_document ON rag_document_chunk (document_id);
CREATE INDEX IF NOT EXISTS idx_rag_chunk_source ON rag_document_chunk (source);

-- Function to update tsvector on content changes
CREATE OR REPLACE FUNCTION update_content_tsv() RETURNS TRIGGER AS $$
BEGIN
    NEW.content_tsv = to_tsvector('simple', NEW.content);
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update content_tsv
DROP TRIGGER IF EXISTS trig_update_content_tsv ON rag_document_chunk;
CREATE TRIGGER trig_update_content_tsv
    BEFORE INSERT OR UPDATE OF content ON rag_document_chunk
    FOR EACH ROW
    EXECUTE FUNCTION update_content_tsv();