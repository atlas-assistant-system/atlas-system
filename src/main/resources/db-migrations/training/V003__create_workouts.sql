CREATE TABLE workouts (
    id       INTEGER PRIMARY KEY,
    name     TEXT    NOT NULL,
    archived INTEGER NOT NULL DEFAULT 0
)