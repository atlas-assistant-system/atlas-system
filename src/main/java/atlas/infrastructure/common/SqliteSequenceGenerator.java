package atlas.infrastructure.common;

import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteSequenceGenerator implements SequenceGenerator {

    private final Connection connection;

    public SqliteSequenceGenerator(Connection connection) {
        this.connection = connection;
    }

    @Override
    public long next(String sequenceName) {
        var sql = "INSERT INTO sequences (name, value) VALUES (?, 1)"
            + " ON CONFLICT(name) DO UPDATE SET value = value + 1 RETURNING value";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, sequenceName);

            try (var rows = statement.executeQuery()) {
                if (!rows.next()) {
                    throw new PersistenceException("Sequence '" + sequenceName + "' returned no value");
                }

                return rows.getLong(1);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to advance the sequence '" + sequenceName + "'", e);
        }
    }

    @Override
    public long current(String sequenceName) {
        try (var statement = connection.prepareStatement("SELECT value FROM sequences WHERE name = ?")) {
            statement.setString(1, sequenceName);

            try (var rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read the sequence '" + sequenceName + "'", e);
        }
    }
}
