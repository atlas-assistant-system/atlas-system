package atlas.application.training.ports;

import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import java.util.List;
import java.util.Optional;

public interface ExerciseReadModel {

    Optional<Exercise> find(ExerciseId id);

    List<Exercise> findAll(boolean includeArchived);
}
