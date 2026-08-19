package sharedkernel.infrastructure.persistence;

import java.lang.System.Logger.Level;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sharedkernel.domain.guards.ObjectGuard;

public final class SchemaMigrator {

    public static final String VERSION_TABLE = "schema_version";

    private static final System.Logger LOG = System.getLogger("sharedkernel.migrations");

    private static final String CREATE_VERSION_TABLE = """
        CREATE TABLE IF NOT EXISTS schema_version (
            version INTEGER PRIMARY KEY,
            name TEXT NOT NULL,
            checksum TEXT NOT NULL,
            applied_at TEXT NOT NULL
        )""";

    private static final String SELECT_APPLIED = "SELECT version, name, checksum FROM schema_version ORDER BY version";

    private static final String INSERT_APPLIED =
        "INSERT INTO schema_version (version, name, checksum, applied_at) VALUES (?, ?, ?, ?)";

    private final Connection connection;
    private final Clock clock;

    public SchemaMigrator(Connection connection, Clock clock) {
        this.connection = ObjectGuard.notNull(connection, "connection");
        this.clock = ObjectGuard.notNull(clock, "clock");
    }

    public int migrate(List<Migration> migrations) {
        ObjectGuard.notNull(migrations, "migrations");

        var ordered = sortedAndValidated(migrations);
        createVersionTable();

        var applied = alreadyApplied();
        verifyNothingChanged(ordered, applied);

        var pending = new ArrayList<Migration>();
        for (var migration : ordered) {
            if (!applied.containsKey(migration.version())) {
                pending.add(migration);
            }
        }

        rejectOutOfOrder(pending, applied);

        for (var migration : pending) {
            apply(migration);
        }

        return pending.size();
    }

    public List<Integer> appliedVersions() {
        createVersionTable();

        return List.copyOf(alreadyApplied().keySet());
    }

    private void apply(Migration migration) {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(migration.sql());
        } catch (SQLException e) {
            rollbackQuietly();
            throw new PersistenceException(
                "Migration V" + migration.version() + " (" + migration.name() + ") failed", e);
        }

        try (var statement = connection.prepareStatement(INSERT_APPLIED)) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.name());
            statement.setString(3, migration.checksum());
            statement.setString(4, Instant.now(clock).toString());
            statement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            rollbackQuietly();
            throw new PersistenceException("Failed to record migration V" + migration.version(), e);
        }

        LOG.log(Level.INFO, "Applied migration V" + migration.version() + " (" + migration.name() + ")");
    }

    private List<Migration> sortedAndValidated(List<Migration> migrations) {
        var byVersion = new HashMap<Integer, Migration>();

        for (var migration : migrations) {
            var previous = byVersion.put(migration.version(), migration);

            if (previous != null) {
                throw new PersistenceException(
                    "Duplicate migration version " + migration.version() + ": '" + previous.name() + "' and '"
                        + migration.name() + "'");
            }
        }

        var ordered = new ArrayList<>(migrations);
        ordered.sort((left, right) -> Integer.compare(left.version(), right.version()));

        return ordered;
    }

    private void verifyNothingChanged(List<Migration> ordered, Map<Integer, AppliedMigration> applied) {
        for (var migration : ordered) {
            var previous = applied.get(migration.version());

            if (previous != null && !previous.checksum().equals(migration.checksum())) {
                throw new PersistenceException(
                    "Migration V" + migration.version() + " (" + migration.name()
                        + ") was modified after being applied. Add a new migration instead of editing this one.");
            }
        }
    }

    private void rejectOutOfOrder(List<Migration> pending, Map<Integer, AppliedMigration> applied) {
        if (pending.isEmpty() || applied.isEmpty()) {
            return;
        }

        var highestApplied = applied.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        var lowestPending = pending.getFirst().version();

        if (lowestPending < highestApplied) {
            throw new PersistenceException(
                "Migration V" + lowestPending + " is older than the last applied version V" + highestApplied
                    + ". Renumber it above the applied ones.");
        }
    }

    private void createVersionTable() {
        try (var statement = connection.createStatement()) {
            statement.execute(CREATE_VERSION_TABLE);
            connection.commit();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to create " + VERSION_TABLE, e);
        }
    }

    private Map<Integer, AppliedMigration> alreadyApplied() {
        var applied = new HashMap<Integer, AppliedMigration>();

        try (var statement = connection.prepareStatement(SELECT_APPLIED); var rows = statement.executeQuery()) {
            while (rows.next()) {
                applied.put(
                    rows.getInt("version"),
                    new AppliedMigration(rows.getInt("version"), rows.getString("name"), rows.getString("checksum")));
            }

            return applied;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read " + VERSION_TABLE, e);
        }
    }

    private void rollbackQuietly() {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            LOG.log(Level.WARNING, "Rollback after a failed migration did not succeed");
        }
    }

    private record AppliedMigration(int version, String name, String checksum) {}
}
