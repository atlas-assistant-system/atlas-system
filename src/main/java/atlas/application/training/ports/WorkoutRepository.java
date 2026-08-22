package atlas.application.training.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.training.Workout;
import atlas.domain.training.WorkoutId;

public interface WorkoutRepository extends Repository<Workout, WorkoutId> {

    WorkoutId nextId();
}
