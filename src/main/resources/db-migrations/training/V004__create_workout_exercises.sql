CREATE TABLE workout_exercises (
    id          TEXT    PRIMARY KEY,
    workout_id  INTEGER NOT NULL REFERENCES workouts (id),
    exercise_id INTEGER NOT NULL REFERENCES exercises (id),
    position    INTEGER NOT NULL,
    sets        INTEGER NOT NULL,
    load_g      INTEGER NOT NULL DEFAULT 0,
    reps        INTEGER NOT NULL DEFAULT 0,
    seconds     INTEGER NOT NULL DEFAULT 0,
    meters      INTEGER NOT NULL DEFAULT 0
)