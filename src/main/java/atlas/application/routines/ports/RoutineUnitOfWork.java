package atlas.application.routines.ports;

import atlas.application.sharedkernel.unitofwork.UnitOfWork;

public interface RoutineUnitOfWork extends UnitOfWork {

    RoutineRepository routines();

    RoutineEntryRepository entries();
}
