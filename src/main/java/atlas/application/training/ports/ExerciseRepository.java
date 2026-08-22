package atlas.application.training.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.training.Exercise;
import atlas.domain.training.ExerciseId;
import atlas.domain.training.vos.ExerciseName;
import java.util.Optional;

public interface ExerciseRepository extends Repository<Exercise, ExerciseId> {

    ExerciseId nextId();

    /**
     * El nombre único lo sostiene un índice de SQLite. Esto existe para poder devolver un
     * conflicto legible en vez de dejar que reviente la restricción como un 500.
     */
    Optional<Exercise> findByName(ExerciseName name);
}
