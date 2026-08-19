package atlas.application.routines.commands.deleteroutine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineEntryRepository;
import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.RoutineDeletedEvent;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteRoutineCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final RoutineEntryRepository entries = mock(RoutineEntryRepository.class);
    private final DeleteRoutineCommandHandler handler =
        new DeleteRoutineCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines, entries);
    }

    @Test
    void shouldTakeTheHistoryWithTheRoutine() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));

        var result = handler.handle(new DeleteRoutineCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.pendingEvents()).containsExactly(new RoutineDeletedEvent(ID, NOW));

        var order = inOrder(entries, routines);
        order.verify(entries).deleteAllOf(ID);
        order.verify(routines).delete(routine);
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.get(ID)).thenReturn(Optional.empty());

        var result = handler.handle(new DeleteRoutineCommand(ID));

        assertThat(result.error()).isEqualTo(RoutineErrors.notFound(ID));
        verify(entries, never()).deleteAllOf(any());
        verify(routines, never()).delete(any());
    }
}
