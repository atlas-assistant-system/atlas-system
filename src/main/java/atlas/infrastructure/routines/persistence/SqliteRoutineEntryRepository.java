package atlas.infrastructure.routines.persistence;

import atlas.application.routines.ports.RoutineEntryRepository;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineId;
import atlas.infrastructure.routines.persistence.mappers.RoutineRows;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SqliteRoutineEntryRepository extends AbstractSqlRepository<RoutineEntry, RoutineEntryId>
    implements RoutineEntryRepository {

    private static final String BY_ROUTINE_AND_DAY =
        "SELECT * FROM routine_entries WHERE routine_id = ? AND day = ?";

    private static final String IN_WINDOW =
        "SELECT * FROM routine_entries WHERE routine_id = ? AND day >= ? AND day < ? ORDER BY day";

    private static final String DELETE_BY_ROUTINE = "DELETE FROM routine_entries WHERE routine_id = ?";

    public SqliteRoutineEntryRepository(Connection connection) {
        super(connection, "routine_entries", "id");
    }

    @Override
    public Optional<RoutineEntry> find(RoutineId routineId, LocalDate day) {
        try (var statement = connection().prepareStatement(BY_ROUTINE_AND_DAY)) {
            statement.setLong(1, routineId.value());
            statement.setString(2, day.toString());

            try (var row = statement.executeQuery()) {
                return row.next() ? Optional.of(RoutineRows.toEntry(row)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read the entry of '" + routineId + "' for " + day, e);
        }
    }

    @Override
    public List<RoutineEntry> findInWindow(RoutineId routineId, LocalDate from, LocalDate toExclusive) {
        try (var statement = connection().prepareStatement(IN_WINDOW)) {
            statement.setLong(1, routineId.value());
            statement.setString(2, from.toString());
            statement.setString(3, toExclusive.toString());

            try (var rows = statement.executeQuery()) {
                var entries = new ArrayList<RoutineEntry>();

                while (rows.next()) {
                    entries.add(RoutineRows.toEntry(rows));
                }

                return List.copyOf(entries);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to read the entries of '" + routineId + "'", e);
        }
    }

    @Override
    public void deleteAllOf(RoutineId routineId) {
        try (var statement = connection().prepareStatement(DELETE_BY_ROUTINE)) {
            statement.setLong(1, routineId.value());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete the history of '" + routineId + "'", e);
        }
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "routine_id", "day", "amount");
    }

    @Override
    protected void bind(PreparedStatement statement, RoutineEntry entry) throws SQLException {
        RoutineRows.bind(statement, entry);
    }

    @Override
    protected RoutineEntry mapRow(ResultSet row) throws SQLException {
        return RoutineRows.toEntry(row);
    }

    @Override
    protected Object idValue(RoutineEntryId id) {
        return id.value().toString();
    }
}
