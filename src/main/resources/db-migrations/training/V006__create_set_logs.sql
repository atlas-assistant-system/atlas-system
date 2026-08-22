CREATE TABLE set_logs (
    id              TEXT    PRIMARY KEY,
    log_id          INTEGER NOT NULL REFERENCES workout_logs (id),
    exercise_id     INTEGER NOT NULL REFERENCES exercises (id),
    position        INTEGER NOT NULL,
    planned_load_g  INTEGER,
    planned_reps    INTEGER,
    planned_seconds INTEGER,
    planned_meters  INTEGER,
    load_g          INTEGER,
    reps            INTEGER,
    seconds         INTEGER,
    meters          INTEGER
)