package atlas.infrastructure.routines.persistence;

import atlas.application.routines.ports.RoutineReadModel;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineId;
import atlas.infrastructure.routines.persistence.mappers.RoutineRows;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.sharedkernel.persistence.RowMapper;
import atlas.infrastructure.sharedkernel.persistence.StatementBinder;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class SqliteRoutineReadModel implements RoutineReadModel {

    private static final String BY_ID = "SELECT * FROM routines WHERE id = ?";
    private static final String ALL = "SELECT * FROM routines ORDER BY name";
    private static final String ALL_ACTIVE = "SELECT * FROM routines WHERE archived = 0 ORDER BY name";
    private static final String ENTRIES_OF = "SELECT * FROM routine_entries WHERE routine_id = ? ORDER BY day";

    private final Connection connection;

    public SqliteRoutineReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Routine> find(RoutineId id) {
        return query(BY_ID, statement -> statement.setLong(1, id.value()), RoutineRows::toRoutine)
            .stream()
            .findFirst();
    }

    @Override
    public List<Routine> findAll(boolean includeArchived) {
        return query(includeArchived ? ALL : ALL_ACTIVE, statement -> {}, RoutineRows::toRoutine);
    }

    @Override
    public List<RoutineEntry> findEntries(RoutineId routineId, LocalDate from, LocalDate toExclusive) {
        return findEntries(List.of(routineId), from, toExclusive);
    }

    @Override
    public List<RoutineEntry> findEntries(List<RoutineId> routineIds, LocalDate from, LocalDate toExclusive) {
        if (routineIds.isEmpty()) {
            return List.of();
        }

        var placeholders = String.join(",", Collections.nCopies(routineIds.size(), "?"));
        var sql = "SELECT * FROM routine_entries WHERE routine_id IN (" + placeholders
            + ") AND day >= ? AND day < ? ORDER BY day";

        return query(sql, statement -> {
            var index = 1;
            for (var routineId : routineIds) {
                statement.setLong(index++, routineId.value());
            }
            statement.setString(index++, from.toString());
            statement.setString(index, toExclusive.toString());
        }, RoutineRows::toEntry);
    }

    @Override
    public List<RoutineEntry> findAllEntries(RoutineId routineId) {
        return query(ENTRIES_OF, statement -> statement.setLong(1, routineId.value()), RoutineRows::toEntry);
    }

    private <T> List<T> query(String sql, StatementBinder binder, RowMapper<T> mapper) {
        try (var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);

            try (var rows = statement.executeQuery()) {
                var result = new ArrayList<T>();

                while (rows.next()) {
                    result.add(mapper.map(rows));
                }

                return List.copyOf(result);
            }
        } catch (SQLException e) {
            throw new PersistenceException("Query failed: " + sql, e);
        }
    }
}
