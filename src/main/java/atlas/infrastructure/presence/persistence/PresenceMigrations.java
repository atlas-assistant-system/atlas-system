package atlas.infrastructure.presence.persistence;

import java.util.List;
import sharedkernel.infrastructure.persistence.Migration;
import sharedkernel.infrastructure.persistence.Migrations;

public final class PresenceMigrations {

    private static final String DIRECTORY = "/db-migrations/presence";
    private static final String[] FILES = {
        "V001__create_profiles.sql",
        "V002__create_face_templates.sql",
        "V003__create_sessions.sql",
        "V004__create_authentication_gate.sql",
        "V005__create_authentication_attempts.sql",
        "V006__create_sequences.sql",
        "V007__create_presence_indexes.sql"
    };

    private PresenceMigrations() {}

    public static List<Migration> load() {
        return Migrations.load(PresenceMigrations.class, DIRECTORY, FILES);
    }
}
