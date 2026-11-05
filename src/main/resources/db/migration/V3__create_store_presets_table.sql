CREATE TABLE IF NOT EXISTS store_presets (
    id                 VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title              VARCHAR(255) NOT NULL,
    description        TEXT,
    genre              VARCHAR(100),
    tags               VARCHAR(255),
    bpm                INTEGER,
    tonality           VARCHAR(50),
    time_signature     VARCHAR(20),
    configuration_data JSONB        NOT NULL,
    downloads_count    INTEGER      NOT NULL DEFAULT 0,
    likes_count        INTEGER      NOT NULL DEFAULT 0,
    user_id            VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    author_name        VARCHAR(255) NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_store_presets_user_id ON store_presets(user_id);
CREATE INDEX IF NOT EXISTS idx_store_presets_created_at ON store_presets(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_store_presets_downloads ON store_presets(downloads_count DESC);
