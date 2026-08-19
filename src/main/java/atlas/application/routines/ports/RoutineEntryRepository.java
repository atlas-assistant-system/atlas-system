package atlas.application.routines.ports;

import atlas.domain.routines.RoutineEntry;
import atlas.domain.routines.RoutineEntryId;
import atlas.domain.routines.RoutineId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import sharedkernel.application.ports.Repository;

public interface RoutineEntryRepository extends Repository<RoutineEntry, RoutineEntryId> {

    Optional<RoutineEntry> find(RoutineId routineId, LocalDate day);

    List<RoutineEntry> findInWindow(RoutineId routineId, LocalDate from, LocalDate toExclusive);

    void deleteAllOf(RoutineId routineId);
}
