CREATE TABLE routines (
    id            INTEGER PRIMARY KEY,
    name          TEXT    NOT NULL,
    description   TEXT,
    target_amount TEXT    NOT NULL,
    target_unit   TEXT,
    period        TEXT    NOT NULL,
    active_days   TEXT    NOT NULL DEFAULT '',
    days_of_month TEXT    NOT NULL DEFAULT '',
    archived      INTEGER NOT NULL DEFAULT 0
)
