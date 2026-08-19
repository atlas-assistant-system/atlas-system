CREATE TABLE appointment_reminders (
    id                TEXT    PRIMARY KEY,
    appointment_id    INTEGER NOT NULL REFERENCES appointments (id),
    lead_time_minutes INTEGER NOT NULL,
    acknowledged_at   TEXT
)
