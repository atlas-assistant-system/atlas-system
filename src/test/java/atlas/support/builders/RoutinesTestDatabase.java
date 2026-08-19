package atlas.support.builders;

import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;
import sharedkernel.infrastructure.persistence.Migrations;
import sharedkernel.infrastructure.persistence.SchemaMigrator;
import sharedkernel.infrastructure.persistence.SqliteConnections;

public final class RoutinesTestDatabase {

    private RoutinesTestDatabase() {}

    public static Connection open(Path directory, Clock clock) {
        var connection = SqliteConnections.openForContext(directory, "routines");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            RoutinesTestDatabase.class, "/db-migrations/routines",
            "V001__create_routines.sql", "V002__create_routine_entries.sql",
            "V003__create_sequences.sql", "V004__index_entries_by_routine_and_day.sql"));

        return connection;
    }
}
