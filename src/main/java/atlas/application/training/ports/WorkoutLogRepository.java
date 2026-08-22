package atlas.application.training.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogId;

public interface WorkoutLogRepository extends Repository<WorkoutLog, WorkoutLogId> {

    WorkoutLogId nextId();
}
