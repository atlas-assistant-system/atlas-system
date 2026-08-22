package atlas.infrastructure.training.persistence;

import atlas.application.training.ports.ExerciseReadModel;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.infrastructure.training.persistence.mappers.ExerciseRows;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public final class SqliteExerciseReadModel implements ExerciseReadModel {

    private static final String BY_ID = "SELECT * FROM exercises WHERE id = ?";
    private static final String ALL = "SELECT * FROM exercises ORDER BY name";
    private static final String ACTIVE = "SELECT * FROM exercises WHERE archived = 0 ORDER BY name";

    private final Connection connection;

    public SqliteExerciseReadModel(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<Exercise> find(ExerciseId id) {
        return SqlQuery.list(
            connection, BY_ID,
            statement -> statement.setLong(1, id.value()),
            ExerciseRows::toExercise)
            .stream()
            .findFirst();
    }

    @Override
    public List<Exercise> findAll(boolean includeArchived) {
        return SqlQuery.list(
            connection, includeArchived ? ALL : ACTIVE, statement -> {}, ExerciseRows::toExercise);
    }
}
