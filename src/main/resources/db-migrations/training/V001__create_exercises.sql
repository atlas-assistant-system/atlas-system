CREATE TABLE exercises (
    id       INTEGER PRIMARY KEY,
    name     TEXT    NOT NULL,
    metric   TEXT    NOT NULL,
    archived INTEGER NOT NULL DEFAULT 0
)