package atlas.application.routines.commands.archiveroutine;

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
import atlas.domain.routines.events.RoutineArchivedEvent;
import atlas.support.builders.RoutineFixtures;
import atlas.support.builders.UnitOfWorkStub;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArchiveRoutineCommandHandlerTest {

    private static final Instant NOW = Instant.parse("2026-02-14T10:00:00Z");
    private static final RoutineId ID = RoutineFixtures.ROUTINE_ID;

    private final RoutineUnitOfWork unitOfWork = mock(RoutineUnitOfWork.class);
    private final RoutineRepository routines = mock(RoutineRepository.class);
    private final ArchiveRoutineCommandHandler handler =
        new ArchiveRoutineCommandHandler(unitOfWork, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void wireUnitOfWork() {
        UnitOfWorkStub.with(unitOfWork, routines);
    }

    @Test
    void shouldArchiveTheRoutineAndRaiseItsEvent() {
        var routine = RoutineFixtures.routine(RecurrencePeriod.WEEK, 3);
        when(routines.get(ID)).thenReturn(Optional.of(routine));

        var result = handler.handle(new ArchiveRoutineCommand(ID));

        assertThat(result.isSuccess()).isTrue();
        assertThat(routine.isArchived()).isTrue();
        assertThat(routine.pendingEvents()).containsExactly(new RoutineArchivedEvent(ID, NOW));
        verify(routines).update(routine);
    }

    @Test
    void shouldFailWhenAlreadyArchived() {
        when(routines.get(ID)).thenReturn(Optional.of(RoutineFixtures.archived(RecurrencePeriod.WEEK, 3)));

        var result = handler.handle(new ArchiveRoutineCommand(ID));

        assertThat(result.error()).isEqualTo(RoutineErrors.ROUTINE_ALREADY_ARCHIVED);
        verify(routines, never()).update(any());
    }

    @Test
    void shouldFailWhenTheRoutineDoesNotExist() {
        when(routines.get(ID)).thenReturn(Optional.empty());

        assertThat(handler.handle(new ArchiveRoutineCommand(ID)).error()).isEqualTo(RoutineErrors.notFound(ID));
    }
}
