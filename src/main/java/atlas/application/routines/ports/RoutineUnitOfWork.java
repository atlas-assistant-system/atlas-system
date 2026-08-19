package atlas.application.routines.ports;

import sharedkernel.application.unitofwork.UnitOfWork;

public interface RoutineUnitOfWork extends UnitOfWork {

    RoutineRepository routines();

    RoutineEntryRepository entries();
}
