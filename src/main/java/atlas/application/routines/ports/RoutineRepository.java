package atlas.application.routines.ports;

import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineId;
import sharedkernel.application.ports.Repository;

public interface RoutineRepository extends Repository<Routine, RoutineId> {

    RoutineId nextId();
}
