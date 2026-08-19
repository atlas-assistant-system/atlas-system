CREATE TABLE face_templates (
    id TEXT PRIMARY KEY,
    profile_id INTEGER NOT NULL,
    descriptor BLOB NOT NULL,
    model_version TEXT NOT NULL,
    captured_at TEXT NOT NULL,
    FOREIGN KEY (profile_id) REFERENCES profiles(id)
);
