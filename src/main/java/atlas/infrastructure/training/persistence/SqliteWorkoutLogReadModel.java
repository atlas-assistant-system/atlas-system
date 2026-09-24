package atlas.infrastructure.training.persistence;

import atlas.application.training.ports.WorkoutLogReadModel;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.SetLog;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SqliteWorkoutLogReadModel implements WorkoutLogReadModel {

    private static final String BY_ID = "SELECT * FROM workout_logs WHERE id = ?";

    private static final String ON_DAY = """
        SELECT * FROM workout_logs
        WHERE performed_on = ?
        ORDER BY started_at, id""";

    private static final String BETWEEN = """
        SELECT * FROM workout_logs
        WHERE performed_on >= ? AND performed_on <= ?
        ORDER BY performed_on DESC, id DESC
        LIMIT ?""";

    private static final String SETS_OF_ALL = """
        SELECT s.* FROM set_logs s
        JOIN workout_logs l ON l.id = s.log_id
        WHERE l.performed_on >= ? AND l.performed_on <= ?
        ORDER BY s.log_id, s.position""";

    private final Connection connection;

    public SqliteWorkoutLogReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<WorkoutLog> find(WorkoutLogId id) {
        var sets = SqlQuery.list(
            connection, "SELECT * FROM set_logs WHERE log_id = ? ORDER BY position",
            statement -> statement.setLong(1, id.value()),
            SqliteWorkoutLogRepository::toSet);

        return SqlQuery.list(
            connection, BY_ID,
            statement -> statement.setLong(1, id.value()),
            row -> SqliteWorkoutLogRepository.toLog(row, sets))
            .stream()
            .findFirst();
    }

    @Override
    public List<WorkoutLog> findOn(LocalDate date) {
        return withSets(date, date, ON_DAY, statement -> statement.setString(1, date.toString()));
    }

    @Override
    public List<WorkoutLog> findBetween(LocalDate from, LocalDate to, int limit) {
        return withSets(from, to, BETWEEN, statement -> {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
            statement.setInt(3, limit);
        });
    }

    private List<WorkoutLog> withSets(
        LocalDate from,
        LocalDate to,
        String sql,
        atlas.infrastructure.sharedkernel.persistence.StatementBinder binder) {

        var byLog = setsByLog(from, to);

        return SqlQuery.list(connection, sql, binder, row -> SqliteWorkoutLogRepository.toLog(
            row, byLog.getOrDefault(row.getLong("id"), List.of())));
    }

    private Map<Long, List<SetLog>> setsByLog(LocalDate from, LocalDate to) {
        var rows = SqlQuery.list(connection, SETS_OF_ALL, statement -> {
            statement.setString(1, from.toString());
            statement.setString(2, to.toString());
        }, row -> Map.entry(row.getLong("log_id"), SqliteWorkoutLogRepository.toSet(row)));

        var byLog = new LinkedHashMap<Long, List<SetLog>>();
        for (var row : rows) {
            byLog.computeIfAbsent(row.getKey(), key -> new ArrayList<>()).add(row.getValue());
        }

        return byLog;
    }
}
