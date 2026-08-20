package atlas.support.builders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import atlas.application.economy.ports.MovementRepository;
import atlas.application.economy.ports.MovementUnitOfWork;
import atlas.application.routines.ports.RoutineEntryRepository;
import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.application.sharedkernel.unitofwork.UnitOfWork;
import java.util.function.Supplier;

public final class UnitOfWorkStub {

    private UnitOfWorkStub() {}

    public static void run(UnitOfWork unitOfWork) {
        when(unitOfWork.execute(any())).thenAnswer(call -> call.<Supplier<Object>>getArgument(0).get());
        doAnswer(call -> {
            call.<Runnable>getArgument(0).run();
            return null;
        }).when(unitOfWork).run(any());
    }

    public static void with(MovementUnitOfWork unitOfWork, MovementRepository movements) {
        run(unitOfWork);
        when(unitOfWork.movements()).thenReturn(movements);
    }

    public static void with(RoutineUnitOfWork unitOfWork, RoutineRepository routines) {
        run(unitOfWork);
        when(unitOfWork.routines()).thenReturn(routines);
    }

    public static void with(
        RoutineUnitOfWork unitOfWork,
        RoutineRepository routines,
        RoutineEntryRepository entries) {
        with(unitOfWork, routines);
        when(unitOfWork.entries()).thenReturn(entries);
    }
}
