package atlas.infrastructure.training.persistence;

import atlas.application.training.ports.WorkoutLogRepository;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogId;
import atlas.domain.training.entities.SetLog;
import atlas.domain.training.entities.SetLogId;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import atlas.infrastructure.training.persistence.mappers.EffortRows;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteWorkoutLogRepository
    extends AbstractSqlRepository<WorkoutLog, WorkoutLogId> implements WorkoutLogRepository {

    private static final String SEQUENCE = "workout_logs";

    private static final String INSERT_SET = """
        INSERT INTO set_logs
            (id, log_id, exercise_id, position,
             planned_load_g, planned_reps, planned_seconds, planned_meters,
             load_g, reps, seconds, meters)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String SETS_OF =
        "SELECT * FROM set_logs WHERE log_id = ? ORDER BY position";

    private static final String DELETE_SETS = "DELETE FROM set_logs WHERE log_id = ?";

    private final Connection connection;
    private final SequenceGenerator sequences;

    public SqliteWorkoutLogRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "workout_logs", "id");
        this.connection = connection;
        this.sequences = sequences;
    }

    @Override
    public WorkoutLogId nextId() {
        return WorkoutLogId.of(sequences.next(SEQUENCE));
    }

    @Override
    public void create(WorkoutLog log) {
        super.create(log);
        insertSets(log);
    }

    @Override
    public void update(WorkoutLog log) {
        super.update(log);
        deleteSetsOf(log.id());
        insertSets(log);
    }

    @Override
    public void delete(WorkoutLog log) {
        deleteSetsOf(log.id());
        super.delete(log);
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "workout_id", "performed_on", "started_at");
    }

    @Override
    protected void bind(PreparedStatement statement, WorkoutLog log) throws SQLException {
        statement.setLong(1, log.id().value());

        var workoutId = log.workoutId();
        if (workoutId.isPresent()) {
            statement.setLong(2, workoutId.get().value());
        } else {
            statement.setNull(2, Types.INTEGER);
        }

        statement.setString(3, log.performedOn().toString());
        statement.setString(4, log.startedAt().toString());
    }

    @Override
    protected WorkoutLog mapRow(ResultSet row) throws SQLException {
        return toLog(row, setsOf(WorkoutLogId.of(row.getLong("id"))));
    }

    @Override
    protected Object idValue(WorkoutLogId id) {
        return id.value();
    }

    static WorkoutLog toLog(ResultSet row, List<SetLog> sets) throws SQLException {
        var workoutId = row.getLong("workout_id");
        var linked = row.wasNull()
            ? Optional.<WorkoutId>empty()
            : Optional.of(WorkoutId.of(workoutId));

        return WorkoutLog.rehydrate(
            WorkoutLogId.of(row.getLong("id")),
            linked,
            LocalDate.parse(row.getString("performed_on")),
            Instant.parse(row.getString("started_at")),
            sets);
    }

    static SetLog toSet(ResultSet row) throws SQLException {
        return new SetLog(
            SetLogId.of(UUID.fromString(row.getString("id"))),
            ExerciseId.of(row.getLong("exercise_id")),
            row.getInt("position"),
            EffortRows.read(row, "planned_"),
            EffortRows.read(row, ""));
    }

    private List<SetLog> setsOf(WorkoutLogId id) {
        return SqlQuery.list(
            connection, SETS_OF,
            statement -> statement.setLong(1, id.value()),
            SqliteWorkoutLogRepository::toSet);
    }

    private void insertSets(WorkoutLog log) {
        if (log.sets().isEmpty()) {
            return;
        }

        try (var statement = connection.prepareStatement(INSERT_SET)) {
            for (var set : log.sets()) {
                statement.setString(1, set.id().value().toString());
                statement.setLong(2, log.id().value());
                statement.setLong(3, set.exerciseId().value());
                statement.setInt(4, set.position());
                EffortRows.bind(statement, 5, set.planned());
                EffortRows.bind(statement, 9, set.actual());
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist the sets of " + log.id(), e);
        }
    }

    private void deleteSetsOf(WorkoutLogId id) {
        try (var statement = connection.prepareStatement(DELETE_SETS)) {
            statement.setLong(1, id.value());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete the sets of " + id, e);
        }
    }
}
