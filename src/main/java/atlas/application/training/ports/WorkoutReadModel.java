package atlas.application.training.ports;

import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutId;
import java.util.List;
import java.util.Optional;

public interface WorkoutReadModel {

    Optional<Workout> find(WorkoutId id);

    List<Workout> findAll(boolean includeArchived);
}
