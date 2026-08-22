package atlas.application.training.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface TrainingUnitOfWork extends UnitOfWork {

    ExerciseRepository exercises();

    WorkoutRepository workouts();

    WorkoutLogRepository logs();
}
