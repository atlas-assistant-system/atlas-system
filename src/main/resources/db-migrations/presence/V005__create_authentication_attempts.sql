CREATE TABLE authentication_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    occurred_on TEXT NOT NULL,
    outcome TEXT NOT NULL
);
