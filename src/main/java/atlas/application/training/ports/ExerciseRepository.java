package atlas.application.training.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.vos.ExerciseName;
import java.util.Optional;

public interface ExerciseRepository extends Repository<Exercise, ExerciseId> {

    ExerciseId nextId();

    Optional<Exercise> findByName(ExerciseName name);
}
