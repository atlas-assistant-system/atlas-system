CREATE TABLE intakes (
    id          INTEGER PRIMARY KEY,
    protein_g   INTEGER NOT NULL,
    carbs_g     INTEGER NOT NULL,
    fat_g       INTEGER NOT NULL,
    note        TEXT,
    consumed_on TEXT    NOT NULL,
    recorded_at TEXT    NOT NULL
)