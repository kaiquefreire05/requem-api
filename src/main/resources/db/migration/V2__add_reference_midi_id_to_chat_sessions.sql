ALTER TABLE chat_sessions
    ADD COLUMN IF NOT EXISTS reference_midi_id VARCHAR(255);
