package atlas.application.routines.commands.unarchiveroutine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import atlas.application.routines.ports.RoutineRepository;
import atlas.application.routines.ports.RoutineUnitOfWork;
import atlas.domain.routines.RoutineErrors;
import atlas.domain.routines.RoutineId;
import atlas.domain.routines.enums.RecurrencePeriod;
import atlas.domain.routines.events.RoutineUnarchivedEvent;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnarchiveRoutineCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final UnarchiveRoutineCommandHandler handler =
        new UnarchiveRoutineCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines);
    }

    @Test
    void shouldUnarchiveTheRoutineAndRaiseItsEvent() {
        var routine = RoutineFixtures.archived(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));

        var result = handler.handle(new UnarchiveRoutineCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.isArchived()).isFalse();
        assertThat(routine.pendingEvents()).containsExactly(new RoutineUnarchivedEvent(ID, NOW));
        verify(routines).update(routine);
    }

    @Test
    void shouldFailWhenTheRoutineIsNotArchived() {
        when(routines.get(ID)).thenReturn(Optional.of(RoutineFixtures.routine(RecurrencePeriod.WEEK, 3)));

        var result = handler.handle(new UnarchiveRoutineCommand(ID));

        assertThat(result.error()).isEqualTo(RoutineErrors.ROUTINE_NOT_ARCHIVED);
        verify(routines, never()).update(any());
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.get(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new UnarchiveRoutineCommand(ID)).error()).isEqualTo(RoutineErrors.notFound(ID));
    }
}
