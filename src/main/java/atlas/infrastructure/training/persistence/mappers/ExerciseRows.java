package atlas.infrastructure.training.persistence.mappers;

import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.enums.Metric;
import atlas.domain.training.vos.ExerciseName;
import atlas.infrastructure.sharedkernel.persistence.PersistenceException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ExerciseRows {

    private ExerciseRows() {}

    public static Exercise toExercise(ResultSet row) throws SQLException {
        var name = ExerciseName.create(row.getString("name"));
        if (name.isFailure()) {
            throw new PersistenceException("Corrupt value in exercises.name");
        }

        return Exercise.rehydrate(
            ExerciseId.of(row.getLong("id")),
            name.value(),
            Metric.valueOf(row.getString("metric")),
            row.getBoolean("archived"));
    }

    public static void bind(PreparedStatement statement, Exercise exercise) throws SQLException {
        statement.setLong(1, exercise.id().value());
        statement.setString(2, exercise.name().value());
        statement.setString(3, exercise.metric().name());
        statement.setBoolean(4, exercise.isArchived());
    }
}
