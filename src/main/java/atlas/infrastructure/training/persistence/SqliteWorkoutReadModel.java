package atlas.infrastructure.training.persistence;

import atlas.application.training.ports.WorkoutReadModel;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutId;
import atlas.infrastructure.sharedkernel.SequenceGenerator;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;


public final class SqliteWorkoutReadModel implements WorkoutReadModel {

    private final SqliteWorkoutRepository workouts;

    public SqliteWorkoutReadModel(Connection connection, SequenceGenerator sequences) {
        this.workouts = new SqliteWorkoutRepository(connection, sequences);
    }

    @Override
    public Optional<Workout> find(WorkoutId id) {
        return workouts.get(id);
    }

    @Override
    public List<Workout> findAll(boolean includeArchived) {
        return workouts.getAll().stream()
            .filter(workout -> includeArchived || !workout.isArchived())
            .sorted((left, right) -> left.name().value().compareToIgnoreCase(right.name().value()))
            .toList();
    }
}
