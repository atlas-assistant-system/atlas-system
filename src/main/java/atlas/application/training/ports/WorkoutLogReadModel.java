package atlas.application.training.ports;

import atlas.domain.training.WorkoutLog;
import atlas.domain.training.WorkoutLogId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkoutLogReadModel {

    Optional<WorkoutLog> find(WorkoutLogId id);

    List<WorkoutLog> findBetween(LocalDate from, LocalDate to, int limit);

    List<WorkoutLog> findOn(LocalDate date);
}
