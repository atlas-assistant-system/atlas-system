CREATE TABLE workout_logs (
    id           INTEGER PRIMARY KEY,
    workout_id   INTEGER REFERENCES workouts (id),
    performed_on TEXT    NOT NULL,
    started_at   TEXT    NOT NULL
)