CREATE TABLE movements (
    id           INTEGER PRIMARY KEY,
    amount_cents INTEGER NOT NULL,
    currency     TEXT    NOT NULL DEFAULT 'EUR',
    category     TEXT    NOT NULL,
    note         TEXT,
    occurred_on  TEXT    NOT NULL,
    recorded_at  TEXT    NOT NULL
)
