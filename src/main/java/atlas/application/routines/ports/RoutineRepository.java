package atlas.application.routines.ports;

import atlas.application.sharedkernel.ports.Repository;
import atlas.domain.routines.Routine;
import atlas.domain.routines.RoutineId;

public interface RoutineRepository extends Repository<Routine, RoutineId> {

    RoutineId nextId();
}
