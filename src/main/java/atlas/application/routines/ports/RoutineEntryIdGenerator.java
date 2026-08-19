package atlas.application.routines.ports;

import atlas.domain.routines.RoutineEntryId;

public interface RoutineEntryIdGenerator {

    RoutineEntryId next();
}
