CREATE TABLE authentication_gate (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    failed_attempts INTEGER NOT NULL CHECK (failed_attempts >= 0),
    locked_until TEXT
);
