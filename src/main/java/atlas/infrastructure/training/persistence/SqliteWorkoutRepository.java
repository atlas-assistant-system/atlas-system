package atlas.infrastructure.training.persistence;

import atlas.application.training.ports.WorkoutRepository;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutId;
import atlas.domain.training.entities.PlannedExercise;
import atlas.domain.training.entities.PlannedExerciseId;
import atlas.domain.training.vos.Effort;
import atlas.domain.training.vos.SetCount;
import atlas.domain.training.vos.WorkoutName;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Las líneas del plan se persisten como en {@code appointments}: al actualizar la raíz se
 * borran sus hijos y se reinsertan. Es una transacción sobre una tabla local con decenas de
 * filas; diffear la colección sería más código para ahorrar una escritura que no duele.
 */
public final class SqliteWorkoutRepository extends AbstractSqlRepository<Workout, WorkoutId>
    implements WorkoutRepository {

    private static final String SEQUENCE = "workouts";

    private static final String INSERT_LINE = """
        INSERT INTO workout_exercises
            (id, workout_id, exercise_id, position, sets, load_g, reps, seconds, meters)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""";

    private static final String LINES_OF = """
        SELECT * FROM workout_exercises WHERE workout_id = ? ORDER BY position""";

    private static final String DELETE_LINES = "DELETE FROM workout_exercises WHERE workout_id = ?";

    private final Connection connection;
    private final SequenceGenerator sequences;

    public SqliteWorkoutRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "workouts", "id");
        this.connection = connection;
        this.sequences = sequences;
    }

    @Override
    public WorkoutId nextId() {
        return WorkoutId.of(sequences.next(SEQUENCE));
    }

    @Override
    public void create(Workout workout) {
        super.create(workout);
        insertLines(workout);
    }

    @Override
    public void update(Workout workout) {
        super.update(workout);
        deleteLinesOf(workout.id());
        insertLines(workout);
    }

    @Override
    public void delete(Workout workout) {
        deleteLinesOf(workout.id());
        super.delete(workout);
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "name", "archived");
    }

    @Override
    protected void bind(PreparedStatement statement, Workout workout) throws SQLException {
        statement.setLong(1, workout.id().value());
        statement.setString(2, workout.name().value());
        statement.setBoolean(3, workout.isArchived());
    }

    @Override
    protected Workout mapRow(ResultSet row) throws SQLException {
        var id = WorkoutId.of(row.getLong("id"));
        var name = WorkoutName.create(row.getString("name"));
        if (name.isFailure()) {
            throw new PersistenceException("Corrupt value in workouts.name");
        }

        return Workout.rehydrate(id, name.value(), linesOf(id), row.getBoolean("archived"));
    }

    @Override
    protected Object idValue(WorkoutId id) {
        return id.value();
    }

    private List<PlannedExercise> linesOf(WorkoutId id) {
        return SqlQuery.list(
            connection, LINES_OF,
            statement -> statement.setLong(1, id.value()),
            SqliteWorkoutRepository::toLine);
    }

    private static PlannedExercise toLine(ResultSet row) throws SQLException {
        var sets = SetCount.create(row.getInt("sets"));
        if (sets.isFailure()) {
            throw new PersistenceException("Corrupt value in workout_exercises.sets");
        }

        var target = Effort.create(
            row.getInt("load_g"), row.getInt("reps"), row.getInt("seconds"), row.getInt("meters"));
        if (target.isFailure()) {
            throw new PersistenceException("Corrupt effort in workout_exercises");
        }

        return new PlannedExercise(
            PlannedExerciseId.of(UUID.fromString(row.getString("id"))),
            ExerciseId.of(row.getLong("exercise_id")),
            row.getInt("position"),
            sets.value(),
            target.value());
    }

    private void insertLines(Workout workout) {
        if (workout.plan().isEmpty()) {
            return;
        }

        try (var statement = connection.prepareStatement(INSERT_LINE)) {
            for (var line : workout.plan()) {
                var target = line.target();
                statement.setString(1, line.id().value().toString());
                statement.setLong(2, workout.id().value());
                statement.setLong(3, line.exerciseId().value());
                statement.setInt(4, line.position());
                statement.setInt(5, line.sets().value());
                statement.setInt(6, target.loadGrams());
                statement.setInt(7, target.reps());
                statement.setInt(8, target.seconds());
                statement.setInt(9, target.meters());
                statement.addBatch();
            }

            statement.executeBatch();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist the plan of " + workout.id(), e);
        }
    }

    private void deleteLinesOf(WorkoutId id) {
        try (var statement = connection.prepareStatement(DELETE_LINES)) {
            statement.setLong(1, id.value());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to delete the plan of " + id, e);
        }
    }
}
