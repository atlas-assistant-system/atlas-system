CREATE TABLE home_profiles (
    profile_id      TEXT PRIMARY KEY,
    location_name   TEXT NOT NULL,
    latitude        REAL NOT NULL,
    longitude       REAL NOT NULL,
    time_zone       TEXT NOT NULL,
    news_categories TEXT NOT NULL
)
