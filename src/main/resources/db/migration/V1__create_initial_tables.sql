CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    email       VARCHAR(255) UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_sessions (
    id               VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    title            VARCHAR(255) NOT NULL DEFAULT 'Nova Composição',
    user_id          VARCHAR(36)  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    composition_data JSONB
);

CREATE TABLE IF NOT EXISTS messages (
    id         VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid()::text,
    role       VARCHAR(20)  NOT NULL,
    content    TEXT         NOT NULL,
    session_id VARCHAR(36)  NOT NULL REFERENCES chat_sessions(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);