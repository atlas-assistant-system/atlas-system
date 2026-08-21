CREATE TABLE plans (
    id              INTEGER PRIMARY KEY,
    start_weight_g  INTEGER NOT NULL,
    target_weight_g INTEGER NOT NULL,
    protein_g       INTEGER NOT NULL,
    carbs_g         INTEGER NOT NULL,
    fat_g           INTEGER NOT NULL,
    status          TEXT    NOT NULL,
    started_on      TEXT    NOT NULL,
    defined_at      TEXT    NOT NULL
)