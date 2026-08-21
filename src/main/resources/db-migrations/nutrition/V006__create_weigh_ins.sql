CREATE TABLE weigh_ins (
    id          INTEGER PRIMARY KEY,
    weight_g    INTEGER NOT NULL,
    measured_on TEXT    NOT NULL UNIQUE,
    recorded_at TEXT    NOT NULL
)