CREATE TABLE savings_goals (
    id           INTEGER PRIMARY KEY,
    name         TEXT    NOT NULL,
    target_cents INTEGER NOT NULL,
    currency     TEXT    NOT NULL DEFAULT 'EUR',
    deadline     TEXT    NOT NULL
)
