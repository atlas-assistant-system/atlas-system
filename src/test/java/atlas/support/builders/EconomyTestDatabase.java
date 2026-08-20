package atlas.support.builders;

import atlas.infrastructure.sharedkernel.persistence.Migrations;
import atlas.infrastructure.sharedkernel.persistence.SchemaMigrator;
import atlas.infrastructure.sharedkernel.persistence.SqliteConnections;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Clock;

public final class EconomyTestDatabase {

    private EconomyTestDatabase() {}

    public static Connection open(Path directory, Clock clock) {
        var connection = SqliteConnections.openForContext(directory, "economy");
        new SchemaMigrator(connection, clock).migrate(Migrations.load(
            EconomyTestDatabase.class, "/db-migrations/economy",
            "V001__create_movements.sql", "V002__create_sequences.sql",
            "V003__index_movements_by_date.sql"));

        return connection;
    }
}
