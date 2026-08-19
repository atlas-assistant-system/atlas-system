CREATE INDEX idx_face_templates_profile ON face_templates (profile_id);
CREATE INDEX idx_sessions_active ON sessions (status, expires_at, profile_id);
CREATE INDEX idx_authentication_attempts_recent ON authentication_attempts (occurred_on DESC, id DESC);
