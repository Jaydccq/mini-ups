-- Add OAuth2 support to users table
-- This migration adds fields necessary for Google OAuth integration

-- Add auth provider column with default LOCAL for existing users
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- Add provider-specific ID column for storing Google's unique identifier
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255);

-- Make password column nullable to support OAuth-only users
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- Add unique constraint for provider + provider_id combination
-- This ensures each OAuth provider ID is unique within its provider
ALTER TABLE users ADD CONSTRAINT uk_users_provider_id UNIQUE (auth_provider, provider_id);

-- Create index for efficient provider-based queries
CREATE INDEX idx_users_auth_provider ON users(auth_provider);

-- Create index for provider_id lookups
CREATE INDEX idx_users_provider_id ON users(provider_id) WHERE provider_id IS NOT NULL;

-- Update existing users to ensure they have the LOCAL auth provider
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;