package atlas.infrastructure.training.persistence;

import atlas.application.training.ports.ExerciseRepository;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.vos.ExerciseName;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import atlas.infrastructure.sharedkernel.persistence.AbstractSqlRepository;
import atlas.infrastructure.training.persistence.mappers.ExerciseRows;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class SqliteExerciseRepository extends AbstractSqlRepository<Exercise, ExerciseId>
    implements ExerciseRepository {

    private static final String SEQUENCE = "exercises";
    private static final String BY_NAME = "SELECT * FROM exercises WHERE name = ?";

    private final Connection connection;
    private final SequenceGenerator sequences;

    public SqliteExerciseRepository(Connection connection, SequenceGenerator sequences) {
        super(connection, "exercises", "id");
        this.connection = connection;
        this.sequences = sequences;
    }

    @Override
    public ExerciseId nextId() {
        return ExerciseId.of(sequences.next(SEQUENCE));
    }

    @Override
    public Optional<Exercise> findByName(ExerciseName name) {
        return SqlQuery.list(
            connection, BY_NAME,
            statement -> statement.setString(1, name.value()),
            ExerciseRows::toExercise)
            .stream()
            .findFirst();
    }

    @Override
    protected List<String> columns() {
        return List.of("id", "name", "metric", "archived");
    }

    @Override
    protected void bind(PreparedStatement statement, Exercise exercise) throws SQLException {
        ExerciseRows.bind(statement, exercise);
    }

    @Override
    protected Exercise mapRow(ResultSet row) throws SQLException {
        return ExerciseRows.toExercise(row);
    }

    @Override
    protected Object idValue(ExerciseId id) {
        return id.value();
    }
}
