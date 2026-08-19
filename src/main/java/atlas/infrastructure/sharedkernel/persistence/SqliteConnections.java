package atlas.infrastructure.sharedkernel.persistence;

import atlas.domain.sharedkernel.guards.ObjectGuard;
import atlas.domain.sharedkernel.guards.StringGuard;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.regex.Pattern;

public final class SqliteConnections {

    public static final String DATABASE_EXTENSION = ".db";

    private static final Pattern SAFE_CONTEXT_NAME = Pattern.compile("[a-z][a-z0-9_]*");

    private SqliteConnections() {}

    public static Connection openForContext(Path databaseDirectory, String boundedContext) {
        ObjectGuard.notNull(databaseDirectory, "databaseDirectory");
        StringGuard.notBlank(boundedContext, "boundedContext");

        if (!SAFE_CONTEXT_NAME.matcher(boundedContext).matches()) {
            throw new PersistenceException("Not a valid bounded context name: " + boundedContext);
        }

        var file = databaseDirectory.resolve(boundedContext + DATABASE_EXTENSION);

        try {
            Files.createDirectories(databaseDirectory);

            var connection = DriverManager.getConnection("jdbc:sqlite:" + file);
            connection.setAutoCommit(false);

            return connection;
        } catch (SQLException | IOException e) {
            throw new PersistenceException("Failed to open the database for " + boundedContext, e);
        }
    }
}
