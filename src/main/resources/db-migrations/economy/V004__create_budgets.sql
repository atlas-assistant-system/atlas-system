CREATE TABLE budgets (
    id           INTEGER PRIMARY KEY,
    category     TEXT    NOT NULL UNIQUE,
    limit_cents  INTEGER NOT NULL,
    currency     TEXT    NOT NULL DEFAULT 'EUR'
)
