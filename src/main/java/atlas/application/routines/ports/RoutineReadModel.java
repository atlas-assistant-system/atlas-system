package atlas.application.routines.ports;

import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoutineReadModel {

    Optional<Routine> find(RoutineId id);

    List<Routine> findAll(boolean includeArchived);

    List<RoutineEntry> findEntries(RoutineId routineId, LocalDate from, LocalDate toExclusive);

    List<RoutineEntry> findEntries(List<RoutineId> routineIds, LocalDate from, LocalDate toExclusive);

    List<RoutineEntry> findAllEntries(RoutineId routineId);
}
